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
import neo.model.util.ConfigurationUtil;
import neo.model.util.JsonUtil;
import neo.network.TimerUtil;

/**
 * the remote node data.
 *
 * @author coranos
 *
 */
public final class RemoteNodeData {

	/**
	 * number of bytes input.
	 */
	public static final String OUT_BYTES = "out-bytes";

	/**
	 * number of bytes output.
	 */
	public static final String IN_BYTES = "in-bytes";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RemoteNodeData.class);

	/**
	 * the tcp address and port function.
	 */
	public static final Function<RemoteNodeData, Object> TCP_ADDRESS_AND_PORT = (final RemoteNodeData data) -> {
		return data.getTcpAddressAndPortString();
	};

	/**
	 * the comparator.
	 *
	 * @return the comparator.
	 */
	public static Comparator<RemoteNodeData> getComparator() {
		final Comparator<String> nullSafeStringComparator = Comparator.nullsFirst(String::compareToIgnoreCase);
		final Comparator<Long> nullSafeLongComparator = Comparator.nullsFirst(Long::compareTo);

		final Comparator<RemoteNodeData> c = Comparator
				/** getConnectionPhase */
				.comparing(RemoteNodeData::getConnectionPhase)
				/** getVersion */
				.thenComparing(RemoteNodeData::getVersion, nullSafeStringComparator)
				/** getBlockHeight */
				.thenComparing(RemoteNodeData::getBlockHeight, nullSafeLongComparator)
				/** getTcpAddressAndPortString */
				.thenComparing(RemoteNodeData::getTcpAddressAndPortString, nullSafeStringComparator)
				/** getLastMessageTimestamp */
				.thenComparing(Comparator.comparing(RemoteNodeData::getLastMessageTimestamp, nullSafeLongComparator));
		return c;
	}

	/**
	 * return the list of indexes.
	 *
	 * @return the list of indexes (simmilar to database indexes, I.E. searchable
	 *         fields), currently just "tcp address and port".
	 */
	public static List<Function<RemoteNodeData, Object>> getIndexCollector() {
		return Arrays.asList(TCP_ADDRESS_AND_PORT);
	}

	/**
	 * tthe block height.
	 */
	private Long blockHeight;

	/**
	 * the sleep interval.
	 */
	private final long sleepIntervalMs;

	/**
	 * the recycle interval.
	 */
	private final long recycleIntervalMs;

	/**
	 * the last message timestamp.
	 */
	private Long lastMessageTimestamp;

	/**
	 * the connection phase.
	 */
	private NodeConnectionPhaseEnum connectionPhase;

	/**
	 * the tcp address and port.
	 */
	private InetSocketAddress tcpAddressAndPort;

	/**
	 * the version.
	 */
	private String version;

	/**
	 * the timer data map.
	 */
	private final Map<String, TimerData> timersMap;

	/**
	 * the message send queue.
	 */
	private final ConcurrentLinkedQueue<Message> sendQueue = new ConcurrentLinkedQueue<>();

	/**
	 * the good peer flag.
	 */
	private boolean isGoodPeer = false;

	/**
	 * the acknowledged peer flag.
	 */
	private boolean isAcknowledgedPeer = false;

	/**
	 * the constructor.
	 *
	 * @param config
	 *            the configuration to use.
	 */
	public RemoteNodeData(final JSONObject config) {
		final JSONObject timersJson = config.getJSONObject(ConfigurationUtil.TIMERS);
		timersMap = TimerUtil.getTimerMap(timersJson);
		sleepIntervalMs = JsonUtil.getTime(config, ConfigurationUtil.SLEEP_INTERVAL);
		recycleIntervalMs = JsonUtil.getTime(config, ConfigurationUtil.RECYCLE_INTERVAL);
	}

	/**
	 * return the block height.
	 *
	 * @return the block height.
	 */
	public Long getBlockHeight() {
		return blockHeight;
	}

	/**
	 * return the connection phase.
	 *
	 * @return the connection phase.
	 */
	public NodeConnectionPhaseEnum getConnectionPhase() {
		return connectionPhase;
	}

	/**
	 * return the host address.
	 *
	 * @return the host address.
	 */
	public String getHostAddress() {
		final InetSocketAddress peer = getTcpAddressAndPort();
		return peer.getAddress().getHostAddress();
	}

	/**
	 * return the last message timestamp.
	 *
	 * @return the last message timestamp.
	 */
	public Long getLastMessageTimestamp() {
		return lastMessageTimestamp;
	}

	/**
	 * return the queue depth.
	 *
	 * @return the queue depth.
	 */
	public int getQueueDepth() {
		return sendQueue.size();
	}

	/**
	 * return the recycle interval.
	 *
	 * @return the recycle interval.
	 */
	public long getRecycleIntervalMs() {
		return recycleIntervalMs;
	}

	/**
	 * return the send queue.
	 *
	 * @return the send queue.
	 */
	public ConcurrentLinkedQueue<Message> getSendQueue() {
		return sendQueue;
	}

	/**
	 * return the sleep interval, in milliseconds.
	 *
	 * @return the sleep interval, in milliseconds.
	 */
	public long getSleepIntervalMs() {
		return sleepIntervalMs;
	}

	/**
	 * return the TCP address and port.
	 *
	 * @return the TCP address and port.
	 */
	public InetSocketAddress getTcpAddressAndPort() {
		return tcpAddressAndPort;
	}

	/**
	 * return the TCP address and port as a string.
	 *
	 * @return the TCP address and port as a string.
	 */
	public String getTcpAddressAndPortString() {
		if (tcpAddressAndPort == null) {
			return null;
		}
		return tcpAddressAndPort.getAddress().getHostAddress() + ":" + tcpAddressAndPort.getPort();
	}

	/**
	 * return the timers map.
	 *
	 * @return the timers map.
	 */
	public Map<String, TimerData> getTimersMap() {
		return timersMap;
	}

	/**
	 * return the version.
	 *
	 * @return the version.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * return the acknowledged peer flag.
	 *
	 * @return the acknowledged peer flag.
	 */
	public boolean isAcknowledgedPeer() {
		return isAcknowledgedPeer;
	}

	/**
	 * return the good peer flag.
	 *
	 * @return the good peer flag.
	 */
	public boolean isGoodPeer() {
		return isGoodPeer;
	}

	/**
	 * queue up a message for sending.
	 *
	 * @param message
	 *            the message to send.
	 */
	public void send(final Message message) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("send to {}:{}", getHostAddress(), message.command);
		}
		if (!isGoodPeer()) {
			LOG.trace("sending message to closed peer : {}", message.command);
			return;
		}
		sendQueue.add(message);
	}

	/**
	 * set the acknowledged peer flag.
	 *
	 * @param isAcknowledgedPeer
	 *            the flag value to use.
	 */
	public void setAcknowledgedPeer(final boolean isAcknowledgedPeer) {
		this.isAcknowledgedPeer = isAcknowledgedPeer;
	}

	/**
	 * sets the block height.
	 *
	 * @param blockHeight
	 *            the block height to set.
	 */
	public void setBlockHeight(final long blockHeight) {
		this.blockHeight = blockHeight;
	}

	/**
	 * sets the connection phase.
	 *
	 * @param connectionPhase
	 *            the connection phase to use.
	 */
	public void setConnectionPhase(final NodeConnectionPhaseEnum connectionPhase) {
		this.connectionPhase = connectionPhase;
	}

	/**
	 * sets the good per flag.
	 *
	 * @param isGoodPeer
	 *            the flag value to use.
	 */
	public void setGoodPeer(final boolean isGoodPeer) {
		this.isGoodPeer = isGoodPeer;
	}

	/**
	 * sets the last message timestamp.
	 *
	 * @param lastMessageTimestamp
	 *            the timestamp to use.
	 */
	public void setLastMessageTimestamp(final Long lastMessageTimestamp) {
		this.lastMessageTimestamp = lastMessageTimestamp;
	}

	/**
	 * sets the TCP address and port.
	 *
	 * @param tcpAddressAndPort
	 *            the tcp address and port to use.
	 */
	public void setTcpAddressAndPort(final InetSocketAddress tcpAddressAndPort) {
		this.tcpAddressAndPort = tcpAddressAndPort;
	}

	/**
	 * sets the version.
	 *
	 * @param version
	 *            the version to use.
	 */
	public void setVersion(final String version) {
		this.version = version;
	}

}
