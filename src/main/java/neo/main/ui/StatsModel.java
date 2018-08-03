package neo.main.ui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.core.Block;
import neo.network.model.LocalNodeData;
import neo.network.model.NodeConnectionPhaseEnum;
import neo.network.model.RemoteNodeData;

/**
 * this class represents the model for the stats page.
 *
 * @author coranos
 *
 */
public final class StatsModel extends AbstractRefreshingModel {

	/**
	 * remaining blockchain block height.
	 */
	private static final String REMAINING_BLOCKCHAIN_BLOCK_HEIGHT = "Remaining Blockchain Block Height";

	/**
	 * duration of this session, in seconds.
	 */
	private static final String ELAPSED_SINCE_START_SECONDS = "Elapsed Since Start - Seconds";

	/**
	 * start time.
	 */
	private static final String START_TIME = "Start Time";

	/**
	 * estimated time when the blockchain is fully synched.
	 */
	private static final String EST_END_TIME = "Est End Time";

	/**
	 * number of blocks since this session started.
	 */
	private static final String ELAPSED_SINCE_START_BLOCK_HEIGHT = "Elapsed Since Start - Blocks";

	/**
	 * block height at session start.
	 */
	private static final String STARTING_BLOCK_HEIGHT = "Starting Block Height";

	/**
	 * estimated file size for blockchain.
	 */
	private static final String EST_FILE_SIZE_FOR_BLOCKCHAIN = "Est File Size For Blockchain";

	/**
	 * estimated time for a single block.
	 */
	private static final String EST_TIME_FOR_BLOCK_MS = "Est Time For Block (Millis)";

	/**
	 * estimated time for full blockchain.
	 */
	private static final String REMAINING_TIME_FOR_BLOCKCHAIN = "Remaining Blockchain Seconds";

	/**
	 * average file size per block.
	 */
	private static final String AVG_FILE_SIZE_PER_BLOCK = "Avg File Size Per Block";

	/**
	 * block file size.
	 */
	private static final String BLOCK_FILE_SIZE = "Block File Size";

	/**
	 * date of last header height change.
	 */
	private static final String LAST_HEADER_HEIGHT_CHANGE = "Last Header Height Change";

	/**
	 * block height on the blockchain (as reported by the CityOfZion REST API).
	 */
	private static final String BLOCKCHAIN_BLOCK_HEIGHT = "Blockchain Block Height";

	/**
	 * known block count.
	 */
	private static final String KNOWN_BLOCK_COUNT = "Known Block Count";

	/**
	 * date of last block height change.
	 */
	private static final String LAST_BLOCK_HEIGHT_CHANGE = "Last Block Height Change";

	/**
	 * maximum known block height.
	 */
	private static final String MAX_BLOCK_HEIGHT = "Max Block Height";

	/**
	 * maximum known block timestamp.
	 */
	private static final String MAX_BLOCK_TIMESTAMP = "Max Block Timestamp";

	/**
	 * maximum known header height.
	 */
	private static final String MAX_HEADER_HEIGHT = "Max Header Height";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(StatsModel.class);

	private static final long serialVersionUID = 1L;

	/**
	 * list of the names of the statistics.
	 */
	private final List<String> statsNameList = new ArrayList<>();

	/**
	 * lists of the values of the statistics, converted from numbers to strings.
	 */
	private final List<String> statsValueList = new ArrayList<>();

	/**
	 * add stats about the blockchain.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 */
	private void addBlockchainStats(final LocalNodeData localNodeData) {
		final long blockCount = localNodeData.getBlockDb().getBlockCount();
		final long blockFileSize = localNodeData.getBlockFileSize();

		final long allChainBlockHeight = localNodeData.getBlockchainBlockHeight();
		addNameAndValue(BLOCKCHAIN_BLOCK_HEIGHT, allChainBlockHeight);
		addNameAndValue(KNOWN_BLOCK_COUNT, blockCount);

		final long blockHeight;
		final Block highestBlock = localNodeData.getBlockDb().getHeaderOfBlockWithMaxIndex();
		if (highestBlock != null) {
			addNameAndValue(MAX_BLOCK_HEIGHT, highestBlock.getIndexAsLong());
			addNameAndValue(MAX_BLOCK_TIMESTAMP, highestBlock.getTimestamp());
			blockHeight = highestBlock.getIndexAsLong();
		} else {
			blockHeight = 0;
		}
		if (localNodeData.getHighestBlockTime() != null) {
			addNameAndValue(LAST_BLOCK_HEIGHT_CHANGE, localNodeData.getHighestBlockTime());
		}

		if (!localNodeData.getVerifiedHeaderPoolMap().isEmpty()) {
			addNameAndValue(MAX_HEADER_HEIGHT, localNodeData.getVerifiedHeaderPoolMap().lastKey());
		} else {
			if (highestBlock != null) {
				addNameAndValue(MAX_HEADER_HEIGHT, highestBlock.getIndexAsLong());
			}
		}

		if (localNodeData.getHighestHeaderTime() != null) {
			addNameAndValue(LAST_HEADER_HEIGHT_CHANGE, localNodeData.getHighestHeaderTime());
		}

		addNameAndValue(BLOCK_FILE_SIZE, blockFileSize);
		if (blockHeight > 0) {
			addNameAndValue(AVG_FILE_SIZE_PER_BLOCK, blockFileSize / blockHeight);
			addNameAndValue(EST_FILE_SIZE_FOR_BLOCKCHAIN, (blockFileSize / blockHeight) * allChainBlockHeight);

			final long startBlockHeight = localNodeData.getStartBlockHeight();
			addNameAndValue(STARTING_BLOCK_HEIGHT, startBlockHeight);

			final long numBlocks = blockHeight - startBlockHeight;
			addNameAndValue(ELAPSED_SINCE_START_BLOCK_HEIGHT, numBlocks);

			final long durationInSeconds = getDurationInSeconds(localNodeData);
			addNameAndValue(ELAPSED_SINCE_START_SECONDS, durationInSeconds);

			if (numBlocks > 0) {
				final long millisecondsPerBlock = (durationInSeconds * 1000) / numBlocks;
				addNameAndValue(EST_TIME_FOR_BLOCK_MS, millisecondsPerBlock);

				final long remainingChainBlockHeight = allChainBlockHeight - blockHeight;
				addNameAndValue(REMAINING_BLOCKCHAIN_BLOCK_HEIGHT, remainingChainBlockHeight);

				final long secondsForChain = (remainingChainBlockHeight * millisecondsPerBlock) / 1000;
				addNameAndValue(REMAINING_TIME_FOR_BLOCKCHAIN, secondsForChain);

				final Date estEndTime = new Date(System.currentTimeMillis() + (secondsForChain * 1000));
				addNameAndValue(EST_END_TIME, estEndTime);
			}
		}
	}

	/**
	 * adds the name and value to the stats list, calling toString() on the value.
	 *
	 * @param name
	 *            the name of the statistic.
	 * @param value
	 *            the date value of the statistic.
	 */
	private void addNameAndValue(final String name, final Date value) {
		statsNameList.add(name);
		statsValueList.add(value.toString());
	}

	/**
	 * adds the name and value to the stats list, formatting the value as an
	 * integer.
	 *
	 * @param name
	 *            the name of the statistic.
	 *
	 * @param value
	 *            the value of the statistic.
	 */
	private void addNameAndValue(final String name, final long value) {
		statsNameList.add(name);
		statsValueList.add(NumberFormat.getIntegerInstance().format(value));
	}

	/**
	 * adds stats for how many peers are in each onnection [hase.
	 *
	 * @param peerDataList
	 *            the set of connected remote peers.
	 */
	private void addNodeConnectionPhaseStats(final List<RemoteNodeData> peerDataList) {
		final Map<NodeConnectionPhaseEnum, Integer> connectionPhaseMap = new EnumMap<>(NodeConnectionPhaseEnum.class);
		for (final NodeConnectionPhaseEnum connectionPhase : NodeConnectionPhaseEnum.values()) {
			connectionPhaseMap.put(connectionPhase, 0);
		}

		for (final RemoteNodeData data : peerDataList) {
			synchronized (data) {
				final int oldCount = connectionPhaseMap.get(data.getConnectionPhase());
				connectionPhaseMap.put(data.getConnectionPhase(), oldCount + 1);
			}
		}

		for (final NodeConnectionPhaseEnum connectionPhase : connectionPhaseMap.keySet()) {
			final int count = connectionPhaseMap.get(connectionPhase);
			addNameAndValue(WordUtils.capitalize(connectionPhase.name()), count);
		}
	}

	/**
	 * add stats on how many peers are at a given version.
	 *
	 * @param peerDataList
	 *            the set of connected remote peers.
	 */
	private void addVersionStats(final List<RemoteNodeData> peerDataList) {
		final Map<String, Integer> versionCountMap = new TreeMap<>();
		for (final RemoteNodeData data : peerDataList) {
			final String version = data.getVersion();
			if (version != null) {
				if (versionCountMap.containsKey(version)) {
					final int oldCount = versionCountMap.get(version);
					versionCountMap.put(version, oldCount + 1);
				} else {
					versionCountMap.put(version, 1);
				}
			}

		}

		for (final String version : versionCountMap.keySet()) {
			final int count = versionCountMap.get(version);
			addNameAndValue(version, count);
		}
	}

	@Override
	public int getColumnCount() {
		synchronized (StatsModel.this) {
			return 2;
		}
	}

	@Override
	public String getColumnName(final int columnIndex) {
		synchronized (StatsModel.this) {
			switch (columnIndex) {
			case 0:
				return "Name";
			case 1:
				return "Value";
			}
		}
		throw new RuntimeException("unknown column name index:" + columnIndex);
	}

	/**
	 * return the duration in seconds.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @return the duration in seconds.
	 */
	public long getDurationInSeconds(final LocalNodeData localNodeData) {
		return (System.currentTimeMillis() - localNodeData.getStartTime()) / 1000;
	}

	@Override
	public int getRowCount() {
		synchronized (StatsModel.this) {
			return statsNameList.size();
		}
	}

	@Override
	public String getThreadName() {
		return "StatsModel.Refresh";
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {
		synchronized (StatsModel.this) {
			switch (columnIndex) {
			case 0:
				if (statsNameList.isEmpty()) {
					return "";
				}
				if (rowIndex >= statsNameList.size()) {
					return "";
				}
				return statsNameList.get(rowIndex);
			case 1:
				if (statsValueList.isEmpty()) {
					return "";
				}
				if (rowIndex >= statsValueList.size()) {
					return "";
				}
				return statsValueList.get(rowIndex);
			}
		}
		throw new RuntimeException("unknown column value index:" + columnIndex);
	}

	@Override
	public void nodeDataChanged(final LocalNodeData localNodeData, final List<RemoteNodeData> peerDataList) {
		LOG.trace("STARTED peersChanged count:{}", peerDataList.size());
		synchronized (StatsModel.this) {
			synchronized (localNodeData) {
				statsNameList.clear();
				statsValueList.clear();

				addNameAndValue(START_TIME, new Date(localNodeData.getStartTime()));

				addNodeConnectionPhaseStats(peerDataList);

				addVersionStats(peerDataList);

				addBlockchainStats(localNodeData);

				setRefresh(true);
			}

			printToFile("StatsModel.txt");
		}
		LOG.trace("SUCCESS peersChanged");
	}
}
