package neo.main.ui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.network.model.LocalNodeData;
import neo.network.model.RemoteNodeData;

/**
 * this class represents the model for the page that shows the lost of remote
 * peers and the associated data.
 *
 * @author coranos
 *
 */
public final class RemotePeerDataModel extends AbstractRefreshingModel {

	/**
	 * a double dash "--" meaning null.
	 */
	private static final String DOUBLE_DASH_MEANING_NULL = "--";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RemotePeerDataModel.class);

	private static final long serialVersionUID = 1L;

	/**
	 * the list of remote peers.
	 */
	private final List<RemoteNodeData> tableDataList = new ArrayList<>();

	@Override
	public int getColumnCount() {
		synchronized (RemotePeerDataModel.this) {
			return 5;
		}
	}

	@Override
	public String getColumnName(final int columnIndex) {
		synchronized (RemotePeerDataModel.this) {
			switch (columnIndex) {
			case 0:
				return "Address";
			case 1:
				return "Last Polled";
			case 2:
				return "Status";
			case 3:
				return "Version";
			case 4:
				return "Block Height";
			case 5:
				return "Index";
			}
		}
		throw new RuntimeException("unknown column name index:" + columnIndex);
	}

	@Override
	public int getRowCount() {
		synchronized (RemotePeerDataModel.this) {
			return tableDataList.size();
		}
	}

	@Override
	public String getThreadName() {
		return "TableModel.Refresh";
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {
		synchronized (RemotePeerDataModel.this) {
			switch (columnIndex) {
			case 0:
				return tableDataList.get(rowIndex).getTcpAddressAndPortString();
			case 1:
				final Long timestamp = tableDataList.get(rowIndex).getLastMessageTimestamp();
				if (timestamp == null) {
					return DOUBLE_DASH_MEANING_NULL;
				}
				return new Date(timestamp);
			case 2:
				return tableDataList.get(rowIndex).getConnectionPhase();
			case 3:
				return tableDataList.get(rowIndex).getVersion();
			case 4:
				final Long blockHeight = tableDataList.get(rowIndex).getBlockHeight();
				if (blockHeight == null) {
					return DOUBLE_DASH_MEANING_NULL;
				}
				return NumberFormat.getIntegerInstance().format(blockHeight);
			case 5:
				return NumberFormat.getIntegerInstance().format(rowIndex + 1);
			}
		}
		throw new RuntimeException("unknown column value index:" + columnIndex);
	}

	@Override
	public void nodeDataChanged(final LocalNodeData localNodeData, final Set<RemoteNodeData> peerDataSet) {
		LOG.trace("STARTED peersChanged");
		synchronized (RemotePeerDataModel.this) {
			synchronized (localNodeData) {
				tableDataList.clear();
				tableDataList.addAll(peerDataSet);
				tableDataList.sort(RemoteNodeData.getComparator());
			}
			setRefresh(true);
		}
		LOG.trace("SUCCESS peersChanged");
	}

}
