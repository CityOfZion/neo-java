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
			return 8;
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
				return "Index";
			case 5:
				return "In Bytes";
			case 6:
				return "Out Bytes";
			case 7:
				return "API Calls";
			}
		}
		throw new RuntimeException("unknown column index:" + columnIndex);
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
					return "--";
				}
				return new Date(timestamp);
			case 2:
				return tableDataList.get(rowIndex).getConnectionPhase();
			case 3:
				return tableDataList.get(rowIndex).getVersion();
			case 4:
				return rowIndex + 1;
			case 5:
				return NumberFormat.getIntegerInstance().format(tableDataList.get(rowIndex).getInBytes());
			case 6:
				return NumberFormat.getIntegerInstance().format(tableDataList.get(rowIndex).getOutBytes());
			case 7:
				return tableDataList.get(rowIndex).getApiCallMap();
			}
		}
		throw new RuntimeException("unknown column index:" + columnIndex);
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
