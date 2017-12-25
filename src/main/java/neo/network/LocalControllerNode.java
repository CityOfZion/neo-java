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
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.IndexedSet;
import neo.model.core.Block;
import neo.model.core.Header;
import neo.model.db.BlockDb;
import neo.model.network.AddrPayload;
import neo.model.network.HeadersPayload;
import neo.model.network.InvPayload;
import neo.model.network.Message;
import neo.model.network.NetworkAddressWithTime;
import neo.model.network.VersionPayload;
import neo.model.util.ConfigurationUtil;
import neo.model.util.JsonUtil;
import neo.model.util.MapUtil;
import neo.model.util.threadpool.ThreadPool;
import neo.network.model.LocalNodeData;
import neo.network.model.NodeConnectionPhaseEnum;
import neo.network.model.RemoteNodeData;
import neo.network.model.TimerData;
import neo.network.model.socket.SocketFactory;

/**
 * the local controller node.
 *
 * @author coranos
 *
 */
public class LocalControllerNode {

	/**
	 * the JSON key, "port".
	 */
	private static final String PORT = "port";

	/**
	 * the JSON key, "address".
	 */
	private static final String ADDRESS = "address";

	/**
	 * the JSON key, "good-peers".
	 */
	private static final String GOOD_PEERS = "good-peers";

	/**
	 * the JSON key, "in-headers-all-duplicates".
	 */
	private static final String IN_HEADERS_ALL_DUPLICATES = "in-headers-all-duplicates";

	/**
	 * the JSON key, "duplicate-in-header".
	 */
	private static final String DUPLICATE_IN_HEADER = "duplicate-in-header";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(LocalControllerNode.class);

	/**
	 * the list of peer change listeners.
	 */
	private final List<NodeDataChangeListener> peerChangeListeners = new ArrayList<>();

	/**
	 * the list of peer data, indexed.
	 */
	private final IndexedSet<RemoteNodeData> peerDataSet = new IndexedSet<>(RemoteNodeData.getComparator(),
			RemoteNodeData.getIndexCollector());

	/**
	 * the thread pool.
	 */
	private final ThreadPool threadPool;

	/**
	 * the local node data.
	 */
	private final LocalNodeData localNodeData;

	/**
	 * the node refresh runnable class.
	 */
	private final LocalControllerNodeRefreshRunnable refreshRunnable;

	/**
	 * the node core RPC runnable class.
	 */
	private final LocalControllerNodeCoreRpcRunnable coreRpcRunnable;

	/**
	 * the refresh thread.
	 */
	private final Thread refreshThread;

	/**
	 * the core RPC server thread.
	 */
	private final Thread coreRpcServerThread;

	/**
	 * the remote node config.
	 */
	private final JSONObject remoteNodeConfig;

	/**
	 * the constructor.
	 *
	 * @param config
	 *            the JSON configuration.
	 */
	public LocalControllerNode(final JSONObject config) {
		LOG.debug("STARTED LocalControllerNode config : {}", config);
		final JSONObject localJson = config.getJSONObject(ConfigurationUtil.LOCAL);
		remoteNodeConfig = config.getJSONObject(ConfigurationUtil.REMOTE);
		final long magic = localJson.getLong(ConfigurationUtil.MAGIC);
		final int activeThreadCount = localJson.getInt(ConfigurationUtil.ACTIVE_THREAD_COUNT);
		final JSONObject timersJson = localJson.getJSONObject(ConfigurationUtil.TIMERS);
		final Map<String, TimerData> timersMap = TimerUtil.getTimerMap(timersJson);
		final long rpcClientTimeoutMillis = JsonUtil.getTime(localJson, ConfigurationUtil.RPC_CLIENT_TIMOUT);
		final long rpcServerTimeoutMillis = JsonUtil.getTime(localJson, ConfigurationUtil.RPC_SERVER_TIMOUT);
		final String blockDbImplStr = localJson.getString(ConfigurationUtil.BLOCK_DB_IMPL);
		final Class<BlockDb> blockDbImplClass = getBlockDbImplClass(blockDbImplStr);

		final String socketFactoryImplStr = localJson.getString(ConfigurationUtil.SOCKET_FACTORY_IMPL);
		final Class<SocketFactory> socketFactoryClass = getSocketFactoryClass(socketFactoryImplStr);

		final int nonce = config.getInt(ConfigurationUtil.NONCE);
		final int port = localJson.getInt(ConfigurationUtil.PORT);
		final File seedNodeFile = new File(localJson.getString(ConfigurationUtil.SEED_NODE_FILE));
		final File goodNodeFile = new File(localJson.getString(ConfigurationUtil.GOOD_NODE_FILE));

		localNodeData = new LocalNodeData(magic, activeThreadCount, rpcClientTimeoutMillis, rpcServerTimeoutMillis,
				blockDbImplClass, timersMap, nonce, port, seedNodeFile, goodNodeFile, socketFactoryClass);
		LocalNodeDataSynchronizedUtil.refreshCityOfZionBlockHeight(localNodeData);

		threadPool = new ThreadPool(localJson.getInt(ConfigurationUtil.THREAD_POOL_COUNT));
		refreshRunnable = new LocalControllerNodeRefreshRunnable(this);
		refreshThread = new Thread(refreshRunnable, "Refresh Thread");

		coreRpcRunnable = new LocalControllerNodeCoreRpcRunnable(this);
		coreRpcServerThread = new Thread(coreRpcRunnable, "Core RPC Thread");
	}

	/**
	 * adds a peer listener.
	 *
	 * @param listener
	 *            the listener to add.
	 */
	public void addPeerChangeListener(final NodeDataChangeListener listener) {
		synchronized (LocalControllerNode.this) {
			peerChangeListeners.add(listener);
			listener.nodeDataChanged(localNodeData, getPeerDataSet());
		}
	}

	/**
	 * adds the remote node data to the pool of data used to in the network.
	 *
	 * @param data
	 *            the data to use.
	 */
	public void addRemoteNodeDataToPool(final RemoteNodeData data) {
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

	/**
	 * returns the class named in blockDbImplClassName , cast to a BlockDb.
	 *
	 * @param blockDbImplClassName
	 *            the name of the BlockDb implementation class to use.
	 * @return the BlockDb implementation class.
	 */
	@SuppressWarnings("unchecked")
	public Class<BlockDb> getBlockDbImplClass(final String blockDbImplClassName) {
		final Class<BlockDb> blockDbImplClass;
		try {
			blockDbImplClass = (Class<BlockDb>) Class.forName(blockDbImplClassName);
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return blockDbImplClass;
	}

	/**
	 * return the local node data.
	 *
	 * @return the local node data.
	 */
	public LocalNodeData getLocalNodeData() {
		return localNodeData;
	}

	/**
	 * return the peer data set.
	 *
	 * @return the peer data set.
	 */
	public IndexedSet<RemoteNodeData> getPeerDataSet() {
		return peerDataSet;
	}

	/**
	 * returns the class named in socketFactoryImplClassName, cast to a
	 * SocketFactory.
	 *
	 * @param socketFactoryImplClassName
	 *            the name of the SocketFactory implementation class to use.
	 * @return the BlockDb implementation class.
	 */
	@SuppressWarnings("unchecked")
	public Class<SocketFactory> getSocketFactoryClass(final String socketFactoryImplClassName) {
		final Class<SocketFactory> socketFactoryClass;
		try {
			socketFactoryClass = (Class<SocketFactory>) Class.forName(socketFactoryImplClassName);
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return socketFactoryClass;
	}

	/**
	 * loads the node file.
	 *
	 * @param nodeFile
	 *            the node file to load.
	 */
	private void loadNodeFile(final File nodeFile) {
		try {
			if (!nodeFile.exists()) {
				LOG.error("FAILURE loadNodeFile, file does not exist: {}", nodeFile.getCanonicalPath());
				return;
			}
			final JSONObject goodNodes = new JSONObject(FileUtils.readFileToString(nodeFile, Charset.defaultCharset()));
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
				addRemoteNodeDataToPool(data);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * loads the node files.
	 */
	public void loadNodeFiles() {
		synchronized (this) {
			loadNodeFile(localNodeData.getSeedNodeFile());
			loadNodeFile(localNodeData.getGoodNodeFile());
		}
		notifyNodeDataChangeListeners();
	}

	/**
	 * notify the node data change listeners that a data change occurred.
	 */
	public void notifyNodeDataChangeListeners() {
		LOG.debug("STARTED notifyNodeDataChangeListeners");
		synchronized (LocalControllerNode.this) {
			for (final NodeDataChangeListener l : peerChangeListeners) {
				l.nodeDataChanged(localNodeData, getPeerDataSet());
			}
		}
		LOG.debug("SUCCESS notifyNodeDataChangeListeners");
	}

	/**
	 * handles the "addr" message.
	 *
	 * @param peer
	 *            the peer taht sent the message.
	 * @param message
	 *            the message.
	 */
	private void onAddr(final RemoteNodeControllerRunnable peer, final Message message) {
		try {
			final AddrPayload addrPayload = (AddrPayload) message.payload;
			if (addrPayload == null) {
				return;
			}
			for (final NetworkAddressWithTime nawt : addrPayload.getAddressList()) {
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
				addRemoteNodeDataToPool(data);
			}

			synchronized (LocalControllerNode.this) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("{} onAddr response:{}, count {}, peerDataSet {}",
							peer.getData().getTcpAddressAndPortString(), message.command,
							addrPayload.getAddressList().size(), getPeerDataSet().size());
				}
			}
		} catch (final Exception e) {
			LOG.error("error in onAddr", e);
		}
	}

	/**
	 * does something on a "block" message.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
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

	/**
	 * does something on a "getaddr" message.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
	private void onGetAddr(final RemoteNodeControllerRunnable peer, final Message message) {
		// TODO: return a list of verified peers.
	}

	/**
	 * does something on a "getblocks" message.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
	private void onGetBlocks(final RemoteNodeControllerRunnable peer, final Message message) {
		// TODO: return a list of blocks.
	}

	/**
	 * does something on a "getdata" message.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
	private void onGetData(final RemoteNodeControllerRunnable peer, final Message message) {
		// TODO: return a list of blocks.
	}

	/**
	 * does something on a "getheaders" message.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
	private void onGetHeaders(final RemoteNodeControllerRunnable peer, final Message message) {
		// TODO: return a list of headers.
	}

	/**
	 * does something on a "headers" message.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
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

	/**
	 * does something on a "inv" message.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
	private void onInv(final RemoteNodeControllerRunnable peer, final Message message) {
		// TODO: figure out what to do here.
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

	/**
	 * does something on a "mempool" message.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
	private void onMempool(final RemoteNodeControllerRunnable peer, final Message message) {
		// TODO: figure out what to do here.
	}

	/**
	 * do something when a message is sent to this peer.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
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
				onGetData(peer, message);
				break;
			case GETHEADERS:
				peer.getData().setAcknowledgedPeer(true);
				onGetHeaders(peer, message);
				break;
			}

			LOG.debug("STARTED responseReceived {}", message.commandEnum);
			TimerUtil.responseReceived(localNodeData.getTimersMap(), message.commandEnum);
			LOG.debug("SUCCESS responseReceived");

		} catch (final Exception e) {
			LOG.error("onMessage error", e);
		}
	}

	/**
	 * do something when a peer closes it's socket.
	 *
	 * @param peer
	 *            the peer that closed the socket.
	 */
	public void onSocketClose(final RemoteNodeControllerRunnable peer) {
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

	/**
	 * does something on a "verack" message.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
	private void onVerack(final RemoteNodeControllerRunnable peer, final Message message) {
		// TODO: figure out what to do here.
	}

	/**
	 * when a "version" message is received, update the peer's connection phase to
	 * be "acknowledged" and set it's version to be the user agent in the payload.
	 *
	 * @param peer
	 *            the peer that sent the message.
	 * @param message
	 *            the message.
	 */
	private void onVersion(final RemoteNodeControllerRunnable peer, final Message message) {
		synchronized (this) {
			final VersionPayload payload = message.getPayload(VersionPayload.class);
			peer.getData().setVersion(payload.userAgent);
			peer.getData().setLastMessageTimestamp(System.currentTimeMillis());
			peer.getData().setConnectionPhase(NodeConnectionPhaseEnum.ACKNOWLEDGED);
		}
		notifyNodeDataChangeListeners();
	}

	/**
	 * removes all listeners.
	 */
	public void removePeerChangeListeners() {
		synchronized (this) {
			peerChangeListeners.clear();
		}
	}

	/**
	 * change all unknown and inactive remote nodes to the pool, and put them in the
	 * "try-start" phase.
	 *
	 * @return true if any new peers were added to the pool.
	 * @throws Exception
	 *             if an error occuured.
	 */
	private boolean runPeers() {
		boolean anyChanged = false;
		final List<RemoteNodeData> peerDataList = new ArrayList<>();
		synchronized (LocalControllerNode.this) {
			peerDataList.addAll(getPeerDataSet());
		}

		for (final RemoteNodeData data : peerDataList) {
			LOG.trace("refreshThread[1] {} runPeers node with phase {}", data.getTcpAddressAndPortString(),
					data.getConnectionPhase());
			if ((data.getConnectionPhase() == NodeConnectionPhaseEnum.UNKNOWN)
					|| (data.getConnectionPhase() == NodeConnectionPhaseEnum.INACTIVE)) {
				synchronized (this) {
					LOG.trace("refreshThread[2] {} runPeers node with phase {}", data.getTcpAddressAndPortString(),
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

	/**
	 * starts the core RPC server.
	 */
	public void startCoreRpcServer() {
		coreRpcServerThread.start();
		while (!coreRpcRunnable.isStarted()) {
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * starts the refresh thread.
	 */
	public void startRefreshThread() {
		refreshThread.start();
	}

	/**
	 * adds all the "unknown" remote nodes to the thread pool.
	 *
	 * @throws Exception
	 *             if an error occurs.
	 */
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
			addRemoteNodeDataToPool(data);
		}
	}

	/**
	 * stops the local controller node.
	 *
	 * @throws InterruptedException
	 *             if an error occurs.
	 */
	public void stop() throws InterruptedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("STARTED stop");
		}
		removePeerChangeListeners();
		stopCoreRpcServer();
		refreshRunnable.setStop(true);
		refreshThread.join();
		threadPool.stop();
		if (LOG.isDebugEnabled()) {
			LOG.debug("SUCCESS stop");
		}
	}

	/**
	 * stops the core RPC server (server cannot be restarted after it is stopped).
	 */
	public void stopCoreRpcServer() {
		coreRpcRunnable.stop();
		try {
			coreRpcServerThread.join();
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
