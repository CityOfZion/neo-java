package neo.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.codec.DecoderException;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.UInt256;
import neo.model.core.Header;
import neo.model.network.InvPayload;
import neo.model.util.GenesisBlockUtil;
import neo.network.model.LocalNodeData;
import neo.network.model.RemoteNodeData;
import neo.rpc.client.CityOfZionUtil;
import neo.rpc.client.RpcClientUtil;

public class BlockControlUtil {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(BlockControlUtil.class);

	public static final void initUnknownBlockHashHeightSet(final LocalNodeData localNodeData) {
		refreshCityOfZionBlockHeight(localNodeData);
	}

	public static final void refreshCityOfZionBlockHeight(final LocalNodeData localNodeData) {
		try {
			final String rpcNode = CityOfZionUtil.getMainNetRpcNode();
			final int blockchainHeight = RpcClientUtil.getBlockCount(localNodeData.getRpcClientTimeoutMillis(), rpcNode,
					false);
			localNodeData.setBlockchainBlockCount(blockchainHeight);
		} catch (final Exception e) {
			LOG.error("error refreshing block height", e);
		}
	}

	public static final void requestBlocksUnsynchronized(final LocalNodeData localNodeData, final RemoteNodeData r)
			throws ClientProtocolException, IOException, DecoderException {
		if (!localNodeData.getVerifiedHeaderPoolMap().isEmpty()) {
			final List<UInt256> hashs = new ArrayList<>();

			final Iterator<Entry<Long, Header>> headerIt = localNodeData.getVerifiedHeaderPoolMap().entrySet()
					.iterator();
			while ((hashs.size() < InvPayload.MAX_HASHES) && headerIt.hasNext()) {
				final Entry<Long, Header> headerElt = headerIt.next();
				final Header header = headerElt.getValue();
				final UInt256 hashRaw = header.hash;
				if (localNodeData.getBlockDb().containsHash(hashRaw)) {
					headerIt.remove();
				} else {
					final byte[] ba = hashRaw.getBytesCopy();
					final UInt256 hash = new UInt256(ba);
					hashs.add(hash);
					if (LOG.isDebugEnabled()) {
						LOG.debug("requestBlocks send {} getblocks {} {}", r.getHostAddress(), header.getIndexAsLong(),
								hash.toReverseHexString());
					}
				}
			}
			MessageUtil.sendGetData(r, localNodeData, hashs.toArray(new UInt256[0]));
		} else {
			if (localNodeData.getBlockDb().getBlockWithMaxIndex() == null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("requestBlocks send {} hash is genesis.", r.getHostAddress());
				}
				MessageUtil.sendGetData(r, localNodeData, GenesisBlockUtil.GENESIS_HASH);
			} else {
				LOG.info("SKIPPING requestBlocks, no hashes.");
			}
		}
	}
}
