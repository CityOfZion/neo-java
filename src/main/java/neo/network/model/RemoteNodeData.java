package neo.network.model;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.json.JSONObject;

import neo.model.util.JsonUtil;
import neo.network.RemoteNodeControllerRunnable;
import neo.network.TimerUtil;

public class RemoteNodeData {

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

	private RemoteNodeControllerRunnable peerRunnable;

	public RemoteNodeData(final JSONObject config) {
		final JSONObject timersJson = config.getJSONObject("timers");
		timersMap = TimerUtil.getTimerMap(timersJson);
		sleepIntervalMs = JsonUtil.getTime(config, "sleep-interval");
		recycleIntervalMs = JsonUtil.getTime(config, "recycle-interval");
	}

	public Map<String, Long> getApiCallMap() {
		if (peerRunnable == null) {
			return Collections.emptyMap();
		}
		return peerRunnable.getApiCallMap();
	}

	public NodeConnectionPhaseEnum getConnectionPhase() {
		return connectionPhase;
	}

	public long getInBytes() {
		if (peerRunnable == null) {
			return 0L;
		}
		return peerRunnable.getInBytes();
	}

	public Long getLastMessageTimestamp() {
		return lastMessageTimestamp;
	}

	public long getOutBytes() {
		if (peerRunnable == null) {
			return 0L;
		}
		return peerRunnable.getOutBytes();
	}

	public RemoteNodeControllerRunnable getPeerRunnable() {
		return peerRunnable;
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

	public void setConnectionPhase(final NodeConnectionPhaseEnum connectionPhase) {
		this.connectionPhase = connectionPhase;
	}

	public void setLastMessageTimestamp(final Long lastMessageTimestamp) {
		this.lastMessageTimestamp = lastMessageTimestamp;
	}

	public void setPeerRunnable(final RemoteNodeControllerRunnable peerRunnable) {
		this.peerRunnable = peerRunnable;
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
