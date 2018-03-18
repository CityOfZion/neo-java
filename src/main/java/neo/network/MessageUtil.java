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

/**
 * the utility for creating messages.
 *
 * @author coranos
 *
 */
public final class MessageUtil {

	/**
	 * message for duplciate out blocks.
	 */
	private static final String DUPLICATE_OUT_BLOCK = "duplicate-out-block";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(MessageUtil.class);

	/**
	 * send a message to get addresses.
	 *
	 * @param remoteNodeData
	 *            the remote node data to use.
	 * @param localNodeData
	 *            the local node data to use.
	 */
	public static void sendGetAddresses(final RemoteNodeData remoteNodeData, final LocalNodeData localNodeData) {
		remoteNodeData.send(new Message(LocalNodeDataSynchronizedUtil.getMagic(localNodeData), CommandEnum.GETADDR));
	}

	/**
	 * send a message to get block data.
	 *
	 * @param remoteNodeData
	 *            the remote node data to use.
	 * @param localNodeData
	 *            the local node data to use.
	 * @param type
	 *            the inventory type.
	 * @param hashs
	 *            the hashes to use.
	 */
	public static void sendGetData(final RemoteNodeData remoteNodeData, final LocalNodeData localNodeData,
			final InventoryType type, final UInt256... hashs) {
		if (type.equals(InventoryType.BLOCK)) {
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
		}

		remoteNodeData.send(
				new Message(localNodeData.getMagic(), CommandEnum.GETDATA, new InvPayload(type, hashs).toByteArray()));
	}

	/**
	 * send a message to get header data.
	 *
	 * @param remoteNodeData
	 *            the remote node data to use.
	 * @param localNodeData
	 *            the local node data to use.
	 * @param hash
	 *            the hash to use.
	 */
	public static void sendGetHeaders(final RemoteNodeData remoteNodeData, final LocalNodeData localNodeData,
			final UInt256 hash) {
		LOG.debug("sendGetHeaders requesting hash:{};", hash);
		remoteNodeData.send(new Message(localNodeData.getMagic(), CommandEnum.GETHEADERS,
				new GetBlocksPayload(hash, null).toByteArray()));
	}

	/**
	 * the constructor.
	 */
	private MessageUtil() {

	}

}
