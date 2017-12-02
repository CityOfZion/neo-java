package neo.rpc.client;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the utilities for dealing with the City of Zion API.
 *
 * @author coranos
 *
 */
public final class CityOfZionUtil {

	/**
	 * the testnet API.
	 */
	private static final String TESTNET_API = "http://testnet-api.wallet.cityofzion.io";

	/**
	 * the mainnet API.
	 */
	public static final String MAINNET_API = "http://api.wallet.cityofzion.io";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(CityOfZionUtil.class);

	/**
	 * return the mainnet URL with the given suffix.
	 *
	 * @param urlSuffix
	 *            the url suffix to use.
	 * @return the mainnet URL with the given suffix.
	 */
	private static JSONObject getMainNetApiJsonAtUrl(final String urlSuffix) {
		try {
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
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * return the mainnet RPC node.
	 *
	 * @return the mainnet RPC node.
	 */
	public static String getMainNetRpcNode() {
		final JSONObject json = getMainNetApiJsonAtUrl("/v2/network/best_node");
		final String rpcNode = json.getString("node");
		LOG.debug("rpcNode {}", rpcNode);
		return rpcNode;
	}

	/**
	 * return the testnet URL with the given suffix.
	 *
	 * @param urlSuffix
	 *            the url suffix to use.
	 * @return the testnet URL with the given suffix.
	 */
	private static JSONObject getTestNetApiJsonAtUrl(final String urlSuffix) {
		try {
			final HttpGet get = new HttpGet(TESTNET_API + urlSuffix);
			final CloseableHttpClient client = HttpClients.createDefault();
			final CloseableHttpResponse response = client.execute(get);
			LOG.debug("status:{}", response.getStatusLine());
			final HttpEntity entity = response.getEntity();
			final String entityStr = EntityUtils.toString(entity);
			LOG.debug("entityStr:{}", entityStr);
			final JSONObject json = new JSONObject(entityStr);
			return json;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * return the testnet RPC node.
	 *
	 * @return the testnet RPC node.
	 */
	public static String getTestNetRpcNode() {
		final JSONObject json = getTestNetApiJsonAtUrl("/v1/network/best_node");
		final String rpcNode = json.getString("node");
		LOG.info("rpcNode {}", rpcNode);
		return rpcNode;
	}

	/**
	 * the constructor.
	 */
	private CityOfZionUtil() {

	}

}
