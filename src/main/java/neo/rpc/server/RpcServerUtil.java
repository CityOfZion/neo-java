package neo.rpc.server;

import org.json.JSONObject;

/**
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
public class RpcServerUtil {

	public static JSONObject process(final String requestStr) {
		final JSONObject request = new JSONObject(requestStr);

		final String methodStr = request.getString("method");
		final CoreRpcCommandEnum coreRpcCommand = CoreRpcCommandEnum.valueOf(methodStr);

		switch (coreRpcCommand) {
		default: {
			final JSONObject response = new JSONObject();
			response.put("error", "unknown method");
			response.put("method", methodStr);
			return response;
		}
		}

	}

}
