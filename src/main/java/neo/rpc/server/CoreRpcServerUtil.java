package neo.rpc.server;

import org.json.JSONObject;

import neo.model.core.Block;
import neo.network.LocalControllerNode;

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
	 * process the request.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param requestStr
	 *            the request to process
	 *
	 * @return the response.
	 */
	public static JSONObject process(final LocalControllerNode controller, final String requestStr) {
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
			final Block block = controller.getLocalNodeData().getBlockDb().getBlockWithMaxIndex();
			if (block == null) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "no blocks in blockchain");
				response.put(EXPECTED, "0x...");
				response.put(ACTUAL, "null");
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
		default: {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "unknown method");
			response.put(EXPECTED, CoreRpcCommandEnum.valuesJSONArray());
			response.put(ACTUAL, methodStr);
			return response;
		}
		}

	}

	/**
	 * the constructor.
	 */
	private CoreRpcServerUtil() {

	}

}
