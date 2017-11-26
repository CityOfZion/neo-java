package neo.network;

import java.util.Set;

import neo.network.model.LocalNodeData;
import neo.network.model.RemoteNodeData;

public interface NodeDataChangeListener {
	void nodeDataChanged(LocalNodeData localNodeData, Set<RemoteNodeData> peerDataSet);
}
