package neo.network;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.IndexedSet;
import neo.model.core.Block;
import neo.model.core.Header;
import neo.model.network.AddrPayload;
import neo.model.network.HeadersPayload;
import neo.model.network.InvPayload;
import neo.model.network.Message;
import neo.model.network.NetworkAddressWithTime;
import neo.model.network.VersionPayload;
import neo.model.util.MapUtil;
import neo.model.util.threadpool.ThreadPool;
import neo.network.model.LocalNodeData;
import neo.network.model.NodeConnectionPhaseEnum;
import neo.network.model.RemoteNodeData;
import neo.network.model.TimerData;

public class LocalControllerNode {

	private static final String IN_HEADERS_ALL_DUPLICATES = "in-headers-all-duplicates";

	private static final String DUPLICATE_IN_HEADER = "duplicate-in-header";

	public static final String GOOD_NODE_FILE = "good-node-file";

	public static final String MIN_RETRY_TIME_MS = "min-retry-time-ms";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(LocalControllerNode.class);

	public static final String MAGIC = "magic";

	public static final String NONCE = "nonce";

	public static final String THREAD_POOL_COUNT = "thread-pool-count";

	public static final String ACTIVE_THREAD_COUNT = "active-thread-count";

	public static final String SEED_NODE_FILE = "seed-node-file";

	public static final String PORT = "port";

	public static final String TIMERS = "timers";

	public static final String REMOTE = "remote";

	public static final String LOCAL = "local";

	private static final String ADDRESS = "address";

	private static final String GOOD_PEERS = "good-peers";

	private final List<NodeDataChangeListener> peerChangeListeners = new ArrayList<>();

	private final IndexedSet<RemoteNodeData> peerDataSet = new IndexedSet<>(RemoteNodeData.getComparator(),
			RemoteNodeData.getIndexCollector());

	private final ThreadPool threadPool;

	private final int nonce;

	private final int port;

	private final File seedNodeFile;

	private final File goodNodeFile;

	private final LocalNodeData localNodeData;

	private final LocalControllerNodeRefreshRunnable refreshRunnable;

	private final Thread refreshThread;

	private final JSONObject remoteNodeConfig;

	public LocalControllerNode(final JSONObject config) throws ClientProtocolException, IOException {
		LOG.debug("STARTED LocalControllerNode config : {}", config);
		final JSONObject localJson = config.getJSONObject(LOCAL);
		remoteNodeConfig = config.getJSONObject(REMOTE);
		final long magic = localJson.getLong(MAGIC);
		final int activeThreadCount = localJson.getInt(ACTIVE_THREAD_COUNT);
		final JSONObject timersJson = localJson.getJSONObject(TIMERS);
		final Map<String, TimerData> timersMap = TimerUtil.getTimerMap(timersJson);
		localNodeData = new LocalNodeData(magic, activeThreadCount, timersMap);
		threadPool = new ThreadPool(localJson.getInt(THREAD_POOL_COUNT));
		nonce = config.getInt(NONCE);
		port = localJson.getInt(PORT);
		seedNodeFile = new File(localJson.getString(SEED_NODE_FILE));
		goodNodeFile = new File(localJson.getString(GOOD_NODE_FILE));
		LocalNodeDataSynchronizedUtil.initUnknownBlockHashHeightSet(localNodeData);
		refreshRunnable = new LocalControllerNodeRefreshRunnable(this);
		refreshThread = new Thread(refreshRunnable, "Refresh Thread");
	}

	public void addPeerChangeListener(final NodeDataChangeListener l) {
		synchronized (LocalControllerNode.this) {
			peerChangeListeners.add(l);
			l.nodeDataChanged(localNodeData, getPeerDataSet());
		}
	}

	public void addPeerWrapperToPool(final RemoteNodeData data) throws Exception {
		LOG.trace("STARTED addPeerWrapperToPool \"{}\"", data);
		synchronized (LocalControllerNode.this) {
			if (getPeerDataSet().containsIndex(RemoteNodeData.TCP_ADDRESS_AND_PORT, data)) {
				LOG.trace("FAILURE addPeerWrapperToPool, peer \"{}\" is a existing peer. ", data);
			} else {
				getPeerDataSet().add(data);
			}
		}

		final boolean anyChanged = runPeers();

		if (anyChanged) {
			notifyNodeDataChangeListeners();
		}
	}

	public LocalNodeData getLocalNodeData() {
		return localNodeData;
	}

	public int getNonce() {
		return nonce;
	}

	public IndexedSet<RemoteNodeData> getPeerDataSet() {
		return peerDataSet;
	}

	public int getPort() {
		return port;
	}

	public void loadNodeFile() throws Exception {
		synchronized (this) {
			loadNodeFile(seedNodeFile);
			loadNodeFile(goodNodeFile);
		}
		notifyNodeDataChangeListeners();
	}

	private void loadNodeFile(final File seedNodeFile) throws Exception {
		if (!seedNodeFile.exists()) {
			LOG.error("FAILURE loadNodeFile, file does not exist: {}", seedNodeFile.getCanonicalPath());
			return;
		}
		final JSONObject goodNodes = new JSONObject(FileUtils.readFileToString(seedNodeFile, Charset.defaultCharset()));
		final JSONArray goodPeers = goodNodes.getJSONArray(GOOD_PEERS);
		for (int ix = 0; ix < goodPeers.length(); ix++) {
			final JSONObject goodPeer = goodPeers.getJSONObject(ix);
			final String addressName = goodPeer.getString(ADDRESS);
			final int port = goodPeer.getInt(PORT);
			final InetAddress address = InetAddress.getByName(addressName);
			final InetSocketAddress addressAndPort = new InetSocketAddress(address, port);
			final RemoteNodeData data = new RemoteNodeData(remoteNodeConfig);
			data.setConnectionPhase(NodeConnectionPhaseEnum.UNKNOWN);
			data.setTcpAddressAndPort(addressAndPort);
			addPeerWrapperToPool(data);
		}
	}

	public void notifyNodeDataChangeListeners() {
		LOG.debug("STARTED notifyNodeDataChangeListeners");
		synchronized (LocalControllerNode.this) {
			for (final NodeDataChangeListener l : peerChangeListeners) {
				l.nodeDataChanged(localNodeData, getPeerDataSet());
			}
		}
		LOG.debug("SUCCESS notifyNodeDataChangeListeners");
	}

	private void onAddr(final RemoteNodeControllerRunnable peer, final Message message) {
		try {
			final AddrPayload addrPayload = (AddrPayload) message.payload;
			for (final NetworkAddressWithTime nawt : addrPayload.addressList) {
				final byte[] addressBa = nawt.address.getBytesCopy();
				ArrayUtils.reverse(addressBa);
				final int port = nawt.port.toReverseBytesPositiveBigInteger().intValue();
				final InetAddress address = InetAddress.getByAddress(addressBa);
				if (LOG.isTraceEnabled()) {
					LOG.trace("address:{};port:{};", address, port);
				}
				final InetSocketAddress addressAndPort = new InetSocketAddress(address, port);
				final RemoteNodeData data = new RemoteNodeData(remoteNodeConfig);
				data.setConnectionPhase(NodeConnectionPhaseEnum.UNKNOWN);
				data.setTcpAddressAndPort(addressAndPort);
				addPeerWrapperToPool(data);
			}

			synchronized (LocalControllerNode.this) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("{} onAddr response:{}, count {}, peerDataSet {}",
							peer.getData().getTcpAddressAndPortString(), message.command,
							addrPayload.addressList.size(), getPeerDataSet().size());
				}
			}
		} catch (final Exception e) {

		}
	}

	private void onBlock(final RemoteNodeControllerRunnable peer, final Message message) {
		final Block newBlock = message.getPayload(Block.class);

		final String expected = new String(Hex.encodeHexString(message.getPayloadByteArray()));
		final String actual = Hex.encodeHexString(newBlock.toByteArray());

		if (!expected.equals(actual)) {
			LOG.error("onBlock newBlock: {}", newBlock);
			LOG.error("onBlock expected: {}", expected);
			LOG.error("onBlock actual  : {}", actual);
			return;
		}

		LocalNodeDataSynchronizedUtil.addUnverifiedBlock(localNodeData, newBlock);
		LocalNodeDataSynchronizedUtil.verifyUnverifiedBlocks(localNodeData);
	}

	private void onGetAddr(final RemoteNodeControllerRunnable peer, final Message message) {
	}

	private void onGetBlocks(final RemoteNodeControllerRunnable peer, final Message message) {
	}

	private void onGetdata(final RemoteNodeControllerRunnable peer, final Message message) {
	}

	private void onGetheaders(final RemoteNodeControllerRunnable peer, final Message message) {
	}

	private void onHeaders(final RemoteNodeControllerRunnable peer, final Message message) {
		final HeadersPayload headersPayload = message.getPayload(HeadersPayload.class);
		LOG.debug("STARTED onHeaders size:{}", headersPayload.getHeaderList().size());
		boolean headerChanged = false;
		for (final Header header : headersPayload.getHeaderList()) {
			final boolean headerNew = LocalNodeDataSynchronizedUtil.addHeaderIfNew(localNodeData, header);
			if (headerNew) {
				headerChanged = true;
			}

			if (!headerNew) {
				MapUtil.increment(LocalNodeData.API_CALL_MAP, DUPLICATE_IN_HEADER);
			}
		}
		LOG.debug("INTERIM onHeaders headerChanged:{}", headerChanged);
		if (headerChanged) {
			LocalNodeDataSynchronizedUtil.verifyUnverifiedHeaders(localNodeData);
			notifyNodeDataChangeListeners();
		} else {
			MapUtil.increment(LocalNodeData.API_CALL_MAP, IN_HEADERS_ALL_DUPLICATES);
			LOG.debug("header message received with {} headers, but all were duplicates.",
					headersPayload.getHeaderList().size());
		}
		LOG.debug("SUCCESS onHeaders headerChanged:{}", headerChanged);
	}

	private void onInv(final RemoteNodeControllerRunnable peer, final Message message) {
		final InvPayload invp = message.getPayload(InvPayload.class);
		switch (invp.getType()) {
		case BLOCK:
			break;
		case CONSENSUS:
			break;
		case TRANSACTION:
			break;
		}

	}

	private void onMempool(final RemoteNodeControllerRunnable peer, final Message message) {
	}

	public void onMessage(final RemoteNodeControllerRunnable peer, final Message message) {
		if (message == null) {
			return;
		}
		if (message.commandEnum == null) {
			LOG.error("unknown command: {}", message.command);
			return;
		}
		try {
			switch (message.commandEnum) {
			case VERSION:
				onVersion(peer, message);
				break;
			case VERACK:
				peer.getData().setAcknowledgedPeer(true);
				onVerack(peer, message);
			case ADDR:
				peer.getData().setAcknowledgedPeer(true);
				onAddr(peer, message);
				break;
			case INV:
				peer.getData().setAcknowledgedPeer(true);
				onInv(peer, message);
				break;
			case GETBLOCKS:
				peer.getData().setAcknowledgedPeer(true);
				onGetBlocks(peer, message);
				break;
			case GETADDR:
				peer.getData().setAcknowledgedPeer(true);
				onGetAddr(peer, message);
				break;
			case HEADERS:
				peer.getData().setAcknowledgedPeer(true);
				onHeaders(peer, message);
				break;
			case BLOCK:
				peer.getData().setAcknowledgedPeer(true);
				onBlock(peer, message);
				break;
			case MEMPOOL:
				peer.getData().setAcknowledgedPeer(true);
				onMempool(peer, message);
				break;
			case GETDATA:
				peer.getData().setAcknowledgedPeer(true);
				onGetdata(peer, message);
				break;
			case GETHEADERS:
				peer.getData().setAcknowledgedPeer(true);
				onGetheaders(peer, message);
				break;
			}

			LOG.debug("STARTED responseReceived {}", message.commandEnum);
			TimerUtil.responseReceived(localNodeData.getTimersMap(), message.commandEnum);
			LOG.debug("SUCCESS responseReceived");

		} catch (final Exception e) {
			LOG.error("onMessage error", e);
		}
	}

	public void OnSocketClose(final RemoteNodeControllerRunnable peer) {
		synchronized (this) {
			if (peer.getData().getVersion() != null) {
				LOG.debug("OnSocketClose {} {}", peer.getData().getTcpAddressAndPortString(),
						peer.getData().getVersion());
			}

			if (peer.getData().getVersion() != null) {
				peer.getData().setConnectionPhase(NodeConnectionPhaseEnum.INACTIVE);
			} else {
				peer.getData().setConnectionPhase(NodeConnectionPhaseEnum.REFUSED);
			}
			peer.getData().setLastMessageTimestamp(System.currentTimeMillis());
		}
		notifyNodeDataChangeListeners();
	}

	private void onVerack(final RemoteNodeControllerRunnable peer, final Message message) {
	}

	private void onVersion(final RemoteNodeControllerRunnable peer, final Message message) {
		synchronized (this) {
			final VersionPayload payload = message.getPayload(VersionPayload.class);
			peer.getData().setVersion(payload.userAgent);
			peer.getData().setLastMessageTimestamp(System.currentTimeMillis());
			peer.getData().setConnectionPhase(NodeConnectionPhaseEnum.ACKNOWLEDGED);
		}
		notifyNodeDataChangeListeners();
	}

	public void removePeerChangeListener(final NodeDataChangeListener l) {
		synchronized (this) {
			peerChangeListeners.remove(l);
		}
	}

	private boolean runPeers() throws Exception {
		boolean anyChanged = false;
		final List<RemoteNodeData> peerDataList = new ArrayList<>();
		synchronized (LocalControllerNode.this) {
			peerDataList.addAll(getPeerDataSet());
		}

		for (final RemoteNodeData data : peerDataList) {
			LOG.trace("refreshThread {} runPeers node with phase {}", data.getTcpAddressAndPortString(),
					data.getConnectionPhase());
			if ((data.getConnectionPhase() == NodeConnectionPhaseEnum.UNKNOWN)
					|| (data.getConnectionPhase() == NodeConnectionPhaseEnum.INACTIVE)) {
				synchronized (this) {
					LOG.trace("refreshThread {} runPeers node with phase {}", data.getTcpAddressAndPortString(),
							data.getConnectionPhase());
					data.setConnectionPhase(NodeConnectionPhaseEnum.TRY_START);

					final RemoteNodeControllerRunnable r = new RemoteNodeControllerRunnable(this, data);

					threadPool.execute(r);
					anyChanged = true;
				}
			}
		}
		return anyChanged;
	}

	public void startRefreshThread() {
		refreshThread.start();
	}

	public void startThreadPool() throws Exception {
		final List<RemoteNodeData> bootstrapPeerList = new ArrayList<>();
		synchronized (LocalControllerNode.this) {
			for (final RemoteNodeData data : getPeerDataSet()) {
				if (data.getConnectionPhase().equals(NodeConnectionPhaseEnum.UNKNOWN)) {
					bootstrapPeerList.add(data);
				}
			}
		}
		notifyNodeDataChangeListeners();

		for (final RemoteNodeData data : bootstrapPeerList) {
			addPeerWrapperToPool(data);
		}
	}

	public void stop() throws InterruptedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("STARTED stop");
		}
		refreshRunnable.setStop(true);
		refreshThread.join();
		if (LOG.isDebugEnabled()) {
			LOG.debug("SUCCESS stop");
		}
	}

	public void verifyUnverifiedBlocks() {
		final boolean blockChanged = LocalNodeDataSynchronizedUtil.verifyUnverifiedBlocks(localNodeData);
		if (blockChanged) {
			notifyNodeDataChangeListeners();
		}
	}

}
