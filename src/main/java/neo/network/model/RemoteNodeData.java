package neo.network.model;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.network.Message;
import neo.model.util.JsonUtil;
import neo.network.TimerUtil;

public class RemoteNodeData {

	public static final String OUT_BYTES = "out-bytes";

	public static final String IN_BYTES = "in-bytes";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RemoteNodeData.class);

	public static final Function<RemoteNodeData, Object> TCP_ADDRESS_AND_PORT = (final RemoteNodeData data) -> {
		return data.getTcpAddressAndPortString();
	};

	public static Comparator<RemoteNodeData> getComparator() {
		final Comparator<String> nullSafeStringComparator = Comparator.nullsFirst(String::compareToIgnoreCase);
		final Comparator<Long> nullSafeLongComparator = Comparator.nullsFirst(Long::compareTo);

		final Comparator<RemoteNodeData> c = Comparator
				/** getConnectionPhase */
				.comparing(RemoteNodeData::getConnectionPhase)
				/** getVersion */
				.thenComparing(RemoteNodeData::getVersion, nullSafeStringComparator)
				/** getTcpAddressAndPortString */
				.thenComparing(RemoteNodeData::getTcpAddressAndPortString, nullSafeStringComparator)
				/** getLastMessageTimestamp */
				.thenComparing(Comparator.comparing(RemoteNodeData::getLastMessageTimestamp, nullSafeLongComparator))
				/** getRcpAddressAndPortString */
				.thenComparing(RemoteNodeData::getRcpAddressAndPortString, nullSafeStringComparator);
		return c;
	}

	/**
	 * @return the list of indexes (simmilar to database indexes, I.E. searchable
	 *         fields), currently just "tcp address and port".
	 */
	public static List<Function<RemoteNodeData, Object>> getIndexCollector() {
		return Arrays.asList(TCP_ADDRESS_AND_PORT);
	}

	private final long sleepIntervalMs;

	private final long recycleIntervalMs;

	private Long lastMessageTimestamp;

	private NodeConnectionPhaseEnum connectionPhase;

	private InetSocketAddress tcpAddressAndPort;

	private InetSocketAddress rpcAddressAndPort;

	private String version;

	private final Map<String, TimerData> timersMap;

	private final ConcurrentLinkedQueue<Message> sendQueue = new ConcurrentLinkedQueue<>();

	private boolean isGoodPeer = false;

	private boolean isAcknowledgedPeer = false;

	public RemoteNodeData(final JSONObject config) {
		final JSONObject timersJson = config.getJSONObject("timers");
		timersMap = TimerUtil.getTimerMap(timersJson);
		sleepIntervalMs = JsonUtil.getTime(config, "sleep-interval");
		recycleIntervalMs = JsonUtil.getTime(config, "recycle-interval");
	}

	public NodeConnectionPhaseEnum getConnectionPhase() {
		return connectionPhase;
	}

	public String getHostAddress() {
		final InetSocketAddress peer = getTcpAddressAndPort();
		return peer.getAddress().getHostAddress();
	}

	public Long getLastMessageTimestamp() {
		return lastMessageTimestamp;
	}

	public int getQueueDepth() {
		return sendQueue.size();
	}

	public String getRcpAddressAndPortString() {
		if (rpcAddressAndPort == null) {
			return null;
		}
		return rpcAddressAndPort.getAddress().getHostAddress() + ":" + rpcAddressAndPort.getPort();
	}

	public long getRecycleIntervalMs() {
		return recycleIntervalMs;
	}

	public InetSocketAddress getRpcAddressAndPort() {
		return rpcAddressAndPort;
	}

	public ConcurrentLinkedQueue<Message> getSendQueue() {
		return sendQueue;
	}

	public long getSleepIntervalMs() {
		return sleepIntervalMs;
	}

	public InetSocketAddress getTcpAddressAndPort() {
		return tcpAddressAndPort;
	}

	public String getTcpAddressAndPortString() {
		if (tcpAddressAndPort == null) {
			return null;
		}
		return tcpAddressAndPort.getAddress().getHostAddress() + ":" + tcpAddressAndPort.getPort();
	}

	public Map<String, TimerData> getTimersMap() {
		return timersMap;
	}

	public String getVersion() {
		return version;
	}

	public boolean isAcknowledgedPeer() {
		return isAcknowledgedPeer;
	}

	public boolean isGoodPeer() {
		return isGoodPeer;
	}

	public void send(final Message message) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("send to {}:{}", getHostAddress(), message.command);
		}
		if (!isGoodPeer()) {
			LOG.error("sending message to closed peer : {}", message.command);
			return;
		}
		sendQueue.add(message);
	}

	public void setAcknowledgedPeer(final boolean isAcknowledgedPeer) {
		this.isAcknowledgedPeer = isAcknowledgedPeer;
	}

	public void setConnectionPhase(final NodeConnectionPhaseEnum connectionPhase) {
		this.connectionPhase = connectionPhase;
	}

	public void setGoodPeer(final boolean isGoodPeer) {
		this.isGoodPeer = isGoodPeer;
	}

	public void setLastMessageTimestamp(final Long lastMessageTimestamp) {
		this.lastMessageTimestamp = lastMessageTimestamp;
	}

	public void setRpcAddressAndPort(final InetSocketAddress rpcAddressAndPort) {
		this.rpcAddressAndPort = rpcAddressAndPort;
	}

	public void setTcpAddressAndPort(final InetSocketAddress tcpAddressAndPort) {
		this.tcpAddressAndPort = tcpAddressAndPort;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

}
