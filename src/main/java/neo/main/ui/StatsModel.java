package neo.main.ui;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	 * remaining blockchain block count.
	 */
	private static final String REMAINING_BLOCKCHAIN_BLOCK_COUNT = "Remaining Blockchain Block Count";

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
	private static final String ELAPSED_SINCE_START_BLOCK_COUNT = "Elapsed Since Start - Blocks";

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
	private static final String EST_TIME_FOR_BLOCK = "Est Time For Block (Seconds)";

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
	 * block count on the blockchain (as reported by the CityOfZion REST API).
	 */
	private static final String BLOCKCHAIN_BLOCK_COUNT = "Blockchain Block Count";

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

		final int allChainBlockCount = localNodeData.getBlockchainBlockCount();
		addNameAndValue(BLOCKCHAIN_BLOCK_COUNT, allChainBlockCount);
		addNameAndValue(KNOWN_BLOCK_COUNT, blockCount);

		final Block highestBlock = localNodeData.getBlockDb().getBlockWithMaxIndex(false);
		if (highestBlock != null) {
			addNameAndValue(MAX_BLOCK_HEIGHT, highestBlock.getIndexAsLong());
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
		if (blockCount > 0) {
			addNameAndValue(AVG_FILE_SIZE_PER_BLOCK, blockFileSize / blockCount);
			addNameAndValue(EST_FILE_SIZE_FOR_BLOCKCHAIN, (blockFileSize / blockCount) * allChainBlockCount);

			final long startBlockCount = localNodeData.getStartBlockCount();
			addNameAndValue(STARTING_BLOCK_HEIGHT, startBlockCount);

			final long numBlocks = blockCount - startBlockCount;
			addNameAndValue(ELAPSED_SINCE_START_BLOCK_COUNT, numBlocks);

			final long durationInSeconds = getDurationInSeconds(localNodeData);
			addNameAndValue(ELAPSED_SINCE_START_SECONDS, durationInSeconds);

			if (numBlocks > 0) {
				final long secondsPerBlock = durationInSeconds / numBlocks;
				final long remainingChainBlockCount = allChainBlockCount - blockCount;
				final long secondsForChain = (remainingChainBlockCount / numBlocks) * durationInSeconds;

				addNameAndValue(EST_TIME_FOR_BLOCK, secondsPerBlock);
				addNameAndValue(REMAINING_BLOCKCHAIN_BLOCK_COUNT, remainingChainBlockCount);
				addNameAndValue(REMAINING_TIME_FOR_BLOCKCHAIN, secondsForChain);
				addNameAndValue(EST_END_TIME, new Date(localNodeData.getStartTime() + (secondsForChain * 1000)));
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
	 * @param peerDataSet
	 *            the set of connected remote peers.
	 */
	private void addNodeConnectionPhaseStats(final Set<RemoteNodeData> peerDataSet) {
		final Map<NodeConnectionPhaseEnum, Integer> connectionPhaseMap = new EnumMap<>(NodeConnectionPhaseEnum.class);
		for (final NodeConnectionPhaseEnum connectionPhase : NodeConnectionPhaseEnum.values()) {
			connectionPhaseMap.put(connectionPhase, 0);
		}

		for (final RemoteNodeData data : peerDataSet) {
			final int oldCount = connectionPhaseMap.get(data.getConnectionPhase());
			connectionPhaseMap.put(data.getConnectionPhase(), oldCount + 1);
		}

		for (final NodeConnectionPhaseEnum connectionPhase : connectionPhaseMap.keySet()) {
			final int count = connectionPhaseMap.get(connectionPhase);
			addNameAndValue(WordUtils.capitalize(connectionPhase.name()), count);
		}
	}

	/**
	 * add stats on how many peers are at a given version.
	 *
	 * @param peerDataSet
	 *            the set of connected remote peers.
	 */
	private void addVersionStats(final Set<RemoteNodeData> peerDataSet) {
		final Map<String, Integer> versionCountMap = new TreeMap<>();
		for (final RemoteNodeData data : peerDataSet) {
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
				return statsNameList.get(rowIndex);
			case 1:
				return statsValueList.get(rowIndex);
			}
		}
		throw new RuntimeException("unknown column value index:" + columnIndex);
	}

	@Override
	public void nodeDataChanged(final LocalNodeData localNodeData, final Set<RemoteNodeData> peerDataSet) {
		LOG.trace("STARTED peersChanged count:{}", peerDataSet.size());
		synchronized (localNodeData) {
			synchronized (StatsModel.this) {
				statsNameList.clear();
				statsValueList.clear();

				addNameAndValue(START_TIME, new Date(localNodeData.getStartTime()));

				addNodeConnectionPhaseStats(peerDataSet);

				addVersionStats(peerDataSet);

				addBlockchainStats(localNodeData);

				try (FileOutputStream fout = new FileOutputStream("StatsModel.txt");
						PrintWriter pw = new PrintWriter(fout, true)) {
					for (int columnIndex = 0; columnIndex < getColumnCount(); columnIndex++) {
						pw.print(getColumnName(columnIndex));
						pw.print("\t");
					}
					pw.println();
					for (int rowIndex = 0; rowIndex < getRowCount(); rowIndex++) {
						for (int columnIndex = 0; columnIndex < getColumnCount(); columnIndex++) {
							pw.print(getValueAt(rowIndex, columnIndex));
							pw.print("\t");
						}
						pw.println();
					}
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}

				setRefresh(true);
			}
		}
		LOG.trace("SUCCESS peersChanged");
	}
}
