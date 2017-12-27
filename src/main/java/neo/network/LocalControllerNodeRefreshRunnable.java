package neo.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.model.network.InventoryType;
import neo.network.model.LocalNodeData;
import neo.network.model.NodeConnectionPhaseEnum;
import neo.network.model.RemoteNodeData;
import neo.network.model.TimerTypeEnum;

/**
 * the controller that controls node refreshing.
 *
 * @author coranos
 *
 */
public final class LocalControllerNodeRefreshRunnable implements Runnable {

	/**
	 * max time to wait between refreshing.
	 */
	private static final int REFRESH_THREAD_MAX_MS = 1000;

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(LocalControllerNodeRefreshRunnable.class);

	/**
	 * if true, stop.
	 */
	private boolean stop = false;

	/**
	 * the local controller node.
	 */
	private final LocalControllerNode localControllerNode;

	/**
	 * the constructor.
	 *
	 * @param localControllerNode
	 *            the node data to use.
	 */
	public LocalControllerNodeRefreshRunnable(final LocalControllerNode localControllerNode) {
		this.localControllerNode = localControllerNode;
	}

	/**
	 * returns true if the command is ready to be sent to the remote node, based on
	 * the timer.
	 *
	 * @param data
	 *            the data to look at.
	 * @param command
	 *            the command to look at.
	 * @return true if the command is ready to be sent to the remote node, based on
	 *         the timer.
	 */
	public boolean isReadyForSend(final RemoteNodeData data, final CommandEnum command) {
		LOG.debug("STARTED isReadyForSend \"{}\"", command);
		final boolean ready = TimerUtil.getTimerData(data.getTimersMap(), command, null).isReadyForSend();
		LOG.debug("SUCCESS isReadyForSend \"{}\" {}", command, ready);
		return ready;
	}

	/**
	 * retain only active and acknowledged peers.
	 *
	 * @param list
	 *            the list of peers to use.
	 */
	private void retainOnlyActiveAndAcknowledgedPeers(final List<RemoteNodeData> list) {
		final Iterator<RemoteNodeData> dataIt = list.iterator();
		while (dataIt.hasNext()) {
			final RemoteNodeData data = dataIt.next();
			switch (data.getConnectionPhase()) {
			case ACTIVE:
			case ACKNOWLEDGED:
				break;
			default:
				dataIt.remove();
			}
		}
	}

	@Override
	public void run() {
		final LocalNodeData localNodeData = localControllerNode.getLocalNodeData();
		final long prevSleepTime = 0;
		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("STARTED refreshThread");
			}
			while (!stop) {
				System.gc();
				if (LOG.isTraceEnabled()) {
					LOG.trace("INTERIM refreshThread");
				}
				final List<RemoteNodeData> allPeerDataList = new ArrayList<>();
				final List<RemoteNodeData> activePeerDataList = new ArrayList<>();
				localControllerNode.addPeerDataSetToList(allPeerDataList);

				Collections.shuffle(allPeerDataList);
				activePeerDataList.addAll(allPeerDataList);
				retainOnlyActiveAndAcknowledgedPeers(activePeerDataList);

				if (activePeerDataList.size() > localNodeData.getActiveThreadCount()) {
					activePeerDataList.subList(localNodeData.getActiveThreadCount(), activePeerDataList.size()).clear();
				}

				if (!activePeerDataList.isEmpty()) {
					if (activePeerDataList.size() == localNodeData.getActiveThreadCount()) {
						activePeerDataList.remove(0).setGoodPeer(false);
					}
				}

				for (final RemoteNodeData data : activePeerDataList) {
					LOG.trace("refreshThread {} isGoodPeer:{}, isAcknowledgedPeer {}, getQueueDepth {}",
							data.getTcpAddressAndPortString(), data.isGoodPeer(), data.isAcknowledgedPeer(),
							data.getQueueDepth());
					if (data.isGoodPeer()) {
						if (data.isAcknowledgedPeer()) {
							if (data.getQueueDepth() == 0) {
								if (isReadyForSend(data, CommandEnum.GETADDR)) {
									LocalNodeDataSynchronizedUtil.requestAddresses(localNodeData, data);
								}
								if (TimerUtil.getTimerData(data.getTimersMap(), CommandEnum.GETDATA,
										InventoryType.BLOCK.name().toLowerCase()).isReadyForSend()) {
									LocalNodeDataSynchronizedUtil.requestBlocks(localNodeData, data);
								}
								if (isReadyForSend(data, CommandEnum.GETHEADERS)) {
									LocalNodeDataSynchronizedUtil.requestHeaders(localNodeData, data);
									sent(data, CommandEnum.GETHEADERS);
								}
							}
						}
					}
				}
				for (final RemoteNodeData data : allPeerDataList) {
					boolean retry = false;
					if (data.getConnectionPhase().equals(NodeConnectionPhaseEnum.UNKNOWN)) {
						retry = true;
					}

					final List<RemoteNodeData> checkActivePeerDataList = new ArrayList<>();
					localControllerNode.addPeerDataSetToList(checkActivePeerDataList);
					retainOnlyActiveAndAcknowledgedPeers(checkActivePeerDataList);
					final int peerDataSetSize = checkActivePeerDataList.size();

					if (data.getConnectionPhase().equals(NodeConnectionPhaseEnum.INACTIVE)) {
						if (peerDataSetSize < (localNodeData.getActiveThreadCount() - 1)) {
							retry = true;
						}
					}
					if (retry) {
						try {
							if (LOG.isDebugEnabled()) {
								LOG.debug("refreshThread {} retrying node with phase {}",
										data.getTcpAddressAndPortString(), data.getConnectionPhase());
							}
							localControllerNode.addRemoteNodeDataToPool(data);
						} catch (final Exception e) {
							throw new RuntimeException(e);
						}
					}
				}

				if (TimerUtil.getTimerData(localNodeData.getTimersMap(), TimerTypeEnum.VERIFY, "headers")
						.isReadyForSend()) {
					LocalNodeDataSynchronizedUtil.verifyUnverifiedHeaders(localNodeData);
				}
				if (TimerUtil.getTimerData(localNodeData.getTimersMap(), TimerTypeEnum.VERIFY, "blocks")
						.isReadyForSend()) {
					LocalNodeDataSynchronizedUtil.verifyUnverifiedBlocks(localNodeData);
				}
				if (TimerUtil.getTimerData(localNodeData.getTimersMap(), TimerTypeEnum.REFRESH, "block-file-size")
						.isReadyForSend()) {
					LocalNodeDataSynchronizedUtil.refreshBlockFileSize(localNodeData);
				}
				if (TimerUtil
						.getTimerData(localNodeData.getTimersMap(), TimerTypeEnum.REFRESH, "block-height-city-of-zion")
						.isReadyForSend()) {
					LocalNodeDataSynchronizedUtil.refreshCityOfZionBlockHeight(localNodeData);
				}
				localControllerNode.notifyNodeDataChangeListeners();
				final long currTime = System.currentTimeMillis();
				final long sleepMs = Math.min(currTime - prevSleepTime, REFRESH_THREAD_MAX_MS);
				try {
					if (LOG.isDebugEnabled()) {
						LOG.debug("STARTED refreshThread sleep for {} ms", sleepMs);
					}
					Thread.sleep(sleepMs);
					if (LOG.isDebugEnabled()) {
						LOG.debug("SUCCESS refreshThread sleep for {} ms", sleepMs);
					}
				} catch (final InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		} catch (final Exception e) {
			LOG.error("FAILURE refreshThread", e);
			return;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("SUCCESS refreshThread");
		}
	}

	/**
	 * update timers saying message was sent.
	 *
	 * @param data
	 *            the remote node data to update.
	 * @param command
	 *            the command that was sent.
	 */
	private void sent(final RemoteNodeData data, final CommandEnum command) {
		LOG.debug("STARTED sent \"{}\"", command);
		TimerUtil.getTimerData(data.getTimersMap(), command, null).requestSent();
		LOG.debug("SUCCESS sent \"{}\"", command);
	}

	/**
	 * sets the stop flag.
	 *
	 * @param stop
	 *            the flag to set.
	 */
	public void setStop(final boolean stop) {
		this.stop = stop;
	}

}
