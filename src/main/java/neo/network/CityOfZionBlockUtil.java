package neo.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.network.model.LocalNodeData;
import neo.rpc.client.CityOfZionUtil;
import neo.rpc.client.RpcClientUtil;

/**
 * the utility that controls block interaction with the CityOfZion RPC servers.
 *
 * @author coranos
 *
 */
public final class CityOfZionBlockUtil {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(CityOfZionBlockUtil.class);

	/**
	 * intialize the block has height from the CityOfZion servers.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 */
	public static void refreshCityOfZionBlockHeight(final LocalNodeData localNodeData) {
		try {
			final String rpcNode = CityOfZionUtil.getMainNetRpcNode();
			if (rpcNode == null) {
				return;
			}
			final Integer blockchainHeight = RpcClientUtil.getBlockCount(localNodeData.getRpcClientTimeoutMillis(),
					rpcNode, true);
			if (blockchainHeight != null) {
				if (blockchainHeight > localNodeData.getBlockchainBlockHeight()) {
					localNodeData.setBlockchainBlockHeight(blockchainHeight);
				}
			}
		} catch (final Exception e) {
			LOG.error("error refreshing block height", e);
		}
	}

	/**
	 * the constructor.
	 */
	private CityOfZionBlockUtil() {

	}

}
