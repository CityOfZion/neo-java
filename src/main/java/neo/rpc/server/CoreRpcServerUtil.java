package neo.rpc.server;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kotlin.NotImplementedError;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.util.ModelUtil;
import neo.network.LocalControllerNode;
import neo.network.model.LocalNodeData;
import neo.network.model.NodeConnectionPhaseEnum;
import neo.network.model.RemoteNodeData;

/**
 * examples.
 *
 * http://cityofzion.io/neon-js/api/index.html
 * <p>
 * https://github.com/CityOfZion/neon-wallet-db/blob/042d2d00c4fb1a657e2268280c46fb900b4645ce/README.md
 * <p>
 * curl
 * http://testnet-api.neonwallet.com/v2/address/balance/ANrL4vPnQCCi5Mro4fqKK1rxrkxEHqmp2E
 * <p>
 * curl
 * http://testnet-api.neonwallet.com/v2/address/history/ALpwWoxKLwbfCTkRpK2iXrXpaMHgWGcrDV
 * <p>
 * curl
 * http://testnet-api.neonwallet.com/v2/transaction/ec4dc0092d5adf8cdf30eadf5116dbb6f138b2e35ca2f1a26d992d69388e0b95
 * <p>
 * curl
 * http://testnet-api.neonwallet.com/v1/address/claims/AJ3yzTLc5jebUskHtphKi1rb2FNoZjbpkz
 * <p>
 * curl http://api.neonwallet.com/v1/network/nodes
 * <p>
 *
 * @author coranos
 *
 */
public final class CoreRpcServerUtil {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(CoreRpcServerUtil.class);

	/**
	 * error, no blocks in block chain.
	 */
	private static final String ERROR_NO_BLOCKS_IN_BLOCKCHAIN = "no blocks in blockchain";

	/**
	 * a generic expected value for a hex string "0x...".
	 */
	private static final String EXPECTED_GENERIC_HEX = "0x...";

	/**
	 * the string "null".
	 */
	private static final String NULL = "null";

	/**
	 * the response tag for the result.
	 */
	private static final String RESULT = "result";

	/**
	 * the response tag for an error, for the actual value of a field.
	 */
	private static final String ACTUAL = "actual";

	/**
	 * the response tag for an error, for the expected value of a field.
	 */
	private static final String EXPECTED = "expected";

	/**
	 * the error response tag.
	 */
	private static final String ERROR = "error";

	/**
	 * the ID request tag.
	 */
	public static final String ID = "id";

	/**
	 * the parameters tag..
	 */
	public static final String PARAMS = "params";

	/**
	 * the jsonrpc version 2.0.
	 */
	public static final String VERSION_2_0 = "2.0";

	/**
	 * the JSON RPC version request tag.
	 */
	public static final String JSONRPC = "jsonrpc";

	/**
	 * the method request tag.
	 */
	public static final String METHOD = "method";

	/**
	 * responds to a "getbestblockhash" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @return the response.
	 */
	private static JSONObject onGetBestBlockHash(final LocalControllerNode controller, final int id) {
		final Block block = controller.getLocalNodeData().getBlockDb().getBlockWithMaxIndex();
		if (block == null) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, ERROR_NO_BLOCKS_IN_BLOCKCHAIN);
			response.put(EXPECTED, EXPECTED_GENERIC_HEX);
			response.put(ACTUAL, NULL);
			return response;
		} else {
			final JSONObject response = new JSONObject();
			final String hashHex = block.hash.toHexString();
			response.put(RESULT, "0x" + hashHex);
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);
			return response;
		}
	}

	/**
	 * responds to a "getblock" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @param params
	 *            the parameters to use.
	 * @return the response.
	 */
	private static JSONObject onGetBlock(final LocalControllerNode controller, final int id, final JSONArray params) {
		if (params.length() == 0) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "no parameters, expected a hash or an index");
			response.put(EXPECTED, 0);
			response.put(ACTUAL, NULL);
			return response;
		} else {
			final boolean verbose;
			if (params.length() >= 2) {
				if (params.get(1) instanceof Number) {
					final long index = params.getLong(1);
					verbose = index == 1;
				} else {
					verbose = false;
				}
			} else {
				verbose = false;
			}

			final Block block;
			if (params.get(0) instanceof String) {
				final String hashStr = params.getString(0);
				final byte[] ba = ModelUtil.decodeHex(hashStr);
				final UInt256 hash = new UInt256(ByteBuffer.wrap(ba));
				try {
					block = controller.getLocalNodeData().getBlockDb().getBlock(hash);
				} catch (final RuntimeException e) {
					final JSONObject response = new JSONObject();
					response.put(ERROR, e.getMessage());
					response.put(EXPECTED, EXPECTED_GENERIC_HEX);
					response.put(ACTUAL, params.get(0));
					return response;
				}
			} else if (params.get(0) instanceof Number) {
				final long index = params.getLong(0);
				block = controller.getLocalNodeData().getBlockDb().getBlock(index);
			} else {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "bad parameters, expected a hash or an index");
				response.put(EXPECTED, 0);
				response.put(ACTUAL, params.get(0));
				return response;
			}
			final JSONObject response = new JSONObject();
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);

			if (verbose) {
				response.put(RESULT, block.toJSONObject());
			} else {
				response.put(RESULT, Hex.encodeHexString(block.toByteArray()));
			}
			return response;
		}
	}

	/**
	 * responds to a "getblockcount" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @return the response.
	 */
	private static JSONObject onGetBlockCount(final LocalControllerNode controller, final int id) {
		final Block block = controller.getLocalNodeData().getBlockDb().getBlockWithMaxIndex();
		if (block == null) {
			final JSONObject response = new JSONObject();
			response.put(RESULT, 0);
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);
			return response;
		} else {
			final JSONObject response = new JSONObject();
			final long index = block.getIndexAsLong();
			response.put(RESULT, index + 1);
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);
			return response;
		}
	}

	/**
	 * responds to a "getblockhash" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @param params
	 *            the parameters to use.
	 * @return the response.
	 */
	private static JSONObject onGetBlockHash(final LocalControllerNode controller, final int id,
			final JSONArray params) {
		if (params.length() == 0) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "no parameters, expected an index");
			response.put(EXPECTED, 0);
			response.put(ACTUAL, NULL);
			return response;
		} else {
			final long index = params.getLong(0);
			try {
				final Block block = controller.getLocalNodeData().getBlockDb().getBlock(index);
				final JSONObject response = new JSONObject();
				response.put(ID, id);
				response.put(JSONRPC, VERSION_2_0);
				response.put(RESULT, block.hash.toHexString());
				return response;
			} catch (final RuntimeException e) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, e.getMessage());
				response.put(EXPECTED, 0);
				response.put(ACTUAL, NULL);
				return response;
			}
		}
	}

	/**
	 * responds to a "getconnectioncount" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @return the response.
	 */
	private static JSONObject onGetConnectionCount(final LocalControllerNode controller, final int id) {
		int connectionCount = 0;
		synchronized (controller) {
			final LocalNodeData localNodeData = controller.getLocalNodeData();
			synchronized (localNodeData) {
				for (final RemoteNodeData data : controller.getPeerDataSet()) {
					if (data.getConnectionPhase() == NodeConnectionPhaseEnum.ACKNOWLEDGED) {
						connectionCount++;
					}
				}

			}
		}
		final JSONObject response = new JSONObject();
		response.put(RESULT, connectionCount);
		response.put(ID, id);
		response.put(JSONRPC, VERSION_2_0);
		return response;
	}

	/**
	 * process the request.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param uri
	 *            the uri to process
	 * @param requestStr
	 *            the request to process
	 *
	 * @return the response.
	 */
	public static JSONObject process(final LocalControllerNode controller, final String uri, final String requestStr) {
		LOG.error("process uri:{};", uri);
		if (uri.startsWith("/address/")) {
			final AddressCommandEnum addressCommand = AddressCommandEnum.fromName(uri);
			switch (addressCommand) {
			case BALANCE: {
				// TODO : implement.
				throw new NotImplementedError(addressCommand.getName());
			}
			case CLAIMS: {
				// TODO : implement.
				throw new NotImplementedError(addressCommand.getName());
			}
			case HISTORY: {
				// TODO : implement.
				throw new NotImplementedError(addressCommand.getName());
			}
			default: {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "unknown address URI");
				response.put(EXPECTED, AddressCommandEnum.getValuesJSONArray());
				response.put(ACTUAL, uri);
				return response;
			}
			}
		} else {

			final JSONObject request = new JSONObject(requestStr);

			final String versionStr = request.getString(JSONRPC);
			if (!versionStr.equals(VERSION_2_0)) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "unexpected version");
				response.put(EXPECTED, VERSION_2_0);
				response.put(ACTUAL, versionStr);
				return response;
			}
			final String methodStr = request.getString(METHOD);
			final int id = request.getInt(ID);
			final CoreRpcCommandEnum coreRpcCommand = CoreRpcCommandEnum.fromName(methodStr);

			switch (coreRpcCommand) {
			case GETBESTBLOCKHASH: {
				return onGetBestBlockHash(controller, id);
			}
			case GETBLOCKCOUNT: {
				return onGetBlockCount(controller, id);
			}
			case GETBLOCK: {
				final JSONArray params = request.getJSONArray(PARAMS);
				return onGetBlock(controller, id, params);
			}
			case GETBLOCKHASH: {
				final JSONArray params = request.getJSONArray(PARAMS);
				return onGetBlockHash(controller, id, params);
			}
			case GETCONNECTIONCOUNT: {
				return onGetConnectionCount(controller, id);
			}
			case GETRAWMEMPOOL: {
				// TODO : implement.
				throw new NotImplementedError(coreRpcCommand.getName());
			}
			case GETRAWTRANSACTION: {
				// TODO : implement.
				throw new NotImplementedError(coreRpcCommand.getName());
			}
			case GETTXOUT: {
				// TODO : implement.
				throw new NotImplementedError(coreRpcCommand.getName());
			}
			case SENDRAWTRANSACTION: {
				// TODO : implement.
				throw new NotImplementedError(coreRpcCommand.getName());
			}
			case SUBMITBLOCK: {
				// TODO : implement.
				throw new NotImplementedError(coreRpcCommand.getName());
			}
			default: {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "unknown method");
				response.put(EXPECTED, CoreRpcCommandEnum.getValuesJSONArray());
				response.put(ACTUAL, methodStr);
				return response;
			}
			}
		}
	}

	/**
	 * the constructor.
	 */
	private CoreRpcServerUtil() {

	}

}
