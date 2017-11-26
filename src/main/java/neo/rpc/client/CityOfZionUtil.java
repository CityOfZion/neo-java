package neo.rpc.client;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CityOfZionUtil {

	public static final String GAS = "602c79718b16e442de58778e148d0b1084e3b2dffd5de6b7b16cee7969282de7";

	private static final String TESTNET_API = "http://testnet-api.wallet.cityofzion.io";

	private static final String MAINNET_API = "http://api.wallet.cityofzion.io";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(CityOfZionUtil.class);

	private static JSONObject getMainNetApiJsonAtUrl(final String urlSuffix)
			throws IOException, ClientProtocolException {
		final String url = MAINNET_API + urlSuffix;
		final HttpGet get = new HttpGet(url);
		final CloseableHttpClient client = HttpClients.createDefault();
		final CloseableHttpResponse response = client.execute(get);
		LOG.debug("status:{}", response.getStatusLine());
		final HttpEntity entity = response.getEntity();
		final String entityStr = EntityUtils.toString(entity);
		LOG.debug("entityStr:{}", entityStr);
		try {
			final JSONObject json = new JSONObject(entityStr);
			return json;
		} catch (final JSONException e) {
			LOG.error("url:" + url + ";entityStr:" + entityStr, e);
			throw e;
		}
	}

	public static String getMainNetRpcNode() throws IOException, ClientProtocolException {
		final JSONObject json = getMainNetApiJsonAtUrl("/v2/network/best_node");
		final String rpcNode = json.getString("node");
		LOG.debug("rpcNode {}", rpcNode);
		return rpcNode;
	}

	private static JSONObject getTestNetApiJsonAtUrl(final String urlSuffix)
			throws IOException, ClientProtocolException {
		final HttpGet get = new HttpGet(TESTNET_API + urlSuffix);
		final CloseableHttpClient client = HttpClients.createDefault();
		final CloseableHttpResponse response = client.execute(get);
		LOG.debug("status:{}", response.getStatusLine());
		final HttpEntity entity = response.getEntity();
		final String entityStr = EntityUtils.toString(entity);
		LOG.debug("entityStr:{}", entityStr);
		final JSONObject json = new JSONObject(entityStr);
		return json;
	}

	public static String getTestNetRpcNode() throws IOException, ClientProtocolException {
		final JSONObject json = getTestNetApiJsonAtUrl("/v1/network/best_node");
		final String rpcNode = json.getString("node");
		LOG.info("rpcNode {}", rpcNode);
		return rpcNode;
	}

}
