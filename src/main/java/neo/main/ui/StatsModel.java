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
import neo.model.util.MapUtil;
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
		throw new RuntimeException("unknown column index:" + columnIndex);
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
		throw new RuntimeException("unknown column index:" + columnIndex);
	}

	@Override
	public void nodeDataChanged(final LocalNodeData localNodeData, final Set<RemoteNodeData> peerDataSet) {
		LOG.trace("STARTED peersChanged count:{}", peerDataSet.size());
		synchronized (localNodeData) {
			synchronized (StatsModel.this) {
				statsNameList.clear();
				statsValueList.clear();

				final Map<String, Long> apiCallMap = new TreeMap<>();
				MapUtil.increment(apiCallMap, localNodeData.getApiCallMap());

				final Map<NodeConnectionPhaseEnum, Integer> statsMap = new EnumMap<>(NodeConnectionPhaseEnum.class);
				for (final NodeConnectionPhaseEnum connectionPhase : NodeConnectionPhaseEnum.values()) {
					statsMap.put(connectionPhase, 0);

				}
				for (final RemoteNodeData data : peerDataSet) {
					MapUtil.increment(apiCallMap, data.getApiCallMap());
					final int oldCount = statsMap.get(data.getConnectionPhase());
					statsMap.put(data.getConnectionPhase(), oldCount + 1);
				}

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

				for (final NodeConnectionPhaseEnum connectionPhase : statsMap.keySet()) {
					final int count = statsMap.get(connectionPhase);
					addNameAndValue(WordUtils.capitalize(connectionPhase.name()), count);
				}

				for (final String version : versionCountMap.keySet()) {
					final int count = versionCountMap.get(version);
					addNameAndValue(version, count);
				}
				final long blockCount = localNodeData.getBlockDb().getBlockCount();
				final long blockFileSize = localNodeData.getBlockFileSize();

				final int allChainBlockCount = localNodeData.getBlockchainBlockCount();
				addNameAndValue("Blockchain Block Count", allChainBlockCount);
				addNameAndValue("Known Block Count", blockCount);

				final Block highestBlock = localNodeData.getBlockDb().getBlockWithMaxIndex();
				if (highestBlock != null) {
					addNameAndValue("Max Block Height", highestBlock.getIndexAsLong());
				}
				if (localNodeData.getHighestBlockTime() != null) {
					addNameAndValue("Last Block Height Change", localNodeData.getHighestBlockTime());
				}

				if (!localNodeData.getVerifiedHeaderPoolMap().isEmpty()) {
					addNameAndValue("Max Header Height", localNodeData.getVerifiedHeaderPoolMap().lastKey());
				}

				if (!localNodeData.getUnverifiedBlockPoolSet().isEmpty()) {
					addNameAndValue("Min Unverified Block Height",
							localNodeData.getUnverifiedBlockPoolSet().first().getIndexAsLong());
					addNameAndValue("Max Unverified Block Height",
							localNodeData.getUnverifiedBlockPoolSet().last().getIndexAsLong());
					addNameAndValue("Count Unverified Block Height", localNodeData.getUnverifiedBlockPoolSet().size());
				}

				if (!localNodeData.getUnverifiedHeaderPoolSet().isEmpty()) {
					addNameAndValue("Min Unverified Header Height",
							localNodeData.getUnverifiedHeaderPoolSet().first().getIndexAsLong());
					addNameAndValue("Max Unverified Header Height",
							localNodeData.getUnverifiedHeaderPoolSet().last().getIndexAsLong());
					addNameAndValue("Count Unverified Header Height",
							localNodeData.getUnverifiedHeaderPoolSet().size());
				}

				if (localNodeData.getHighestHeaderTime() != null) {
					addNameAndValue("Last Header Height Change", localNodeData.getHighestHeaderTime());
				}

				addNameAndValue("Block File Size", blockFileSize);
				if (blockCount > 0) {
					addNameAndValue("Avg File Size Per Block", blockFileSize / blockCount);
					addNameAndValue("Est File Size For Blockchain", (blockFileSize / blockCount) * allChainBlockCount);
				}

				for (final String key : apiCallMap.keySet()) {
					final long value = apiCallMap.get(key);
					addNameAndValue(key, value);
				}

				try (FileOutputStream fout = new FileOutputStream("LocalControllerStatsModel.txt");
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
