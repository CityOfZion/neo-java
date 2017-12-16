package neo.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.model.bytes.UInt256;
import neo.model.network.GetBlocksPayload;
import neo.model.network.InvPayload;
import neo.model.network.InventoryType;
import neo.model.network.Message;
import neo.model.util.MapUtil;
import neo.network.model.LocalNodeData;
import neo.network.model.RemoteNodeData;

public class MessageUtil {

	private static final String DUPLICATE_OUT_BLOCK = "duplicate-out-block";
	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(MessageUtil.class);

	public static void sendGetAddresses(final RemoteNodeData r, final LocalNodeData localNodeData) {
		r.send(new Message(LocalNodeDataSynchronizedUtil.getMagic(localNodeData), CommandEnum.GETADDR.getName()));
	}

	public static void sendGetData(final RemoteNodeData r, final LocalNodeData localNodeData, final UInt256... hashs) {
		boolean hasDuplicates = false;
		for (final UInt256 hash : hashs) {
			if (localNodeData.getBlockDb().containsBlockWithHash(hash)) {
				hasDuplicates = true;
				LOG.debug("sendGetData requesting duplicate block hash:{}", hash);
			}
		}
		if (hasDuplicates) {
			MapUtil.increment(LocalNodeData.API_CALL_MAP, DUPLICATE_OUT_BLOCK);
		}

		r.send(new Message(localNodeData.getMagic(), CommandEnum.GETDATA.getName(),
				new InvPayload(InventoryType.BLOCK, hashs).toByteArray()));
	}

	public static void sendGetHeaders(final RemoteNodeData r, final LocalNodeData localNodeData, final UInt256 hash) {
		r.send(new Message(localNodeData.getMagic(), CommandEnum.GETHEADERS.getName(),
				new GetBlocksPayload(hash, null).toByteArray()));
	}

}
