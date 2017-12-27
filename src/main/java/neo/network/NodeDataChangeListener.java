package neo.network;

import java.util.List;

import neo.network.model.LocalNodeData;
import neo.network.model.RemoteNodeData;

/**
 * the node data change listener interface.
 *
 * @author coranos
 *
 */
public interface NodeDataChangeListener {
	/**
	 * notify that node data changed.
	 *
	 * @param localNodeData
	 *            the local node data.
	 * @param peerDataList
	 *            the remote node data list.
	 */
	void nodeDataChanged(LocalNodeData localNodeData, List<RemoteNodeData> peerDataList);
}
