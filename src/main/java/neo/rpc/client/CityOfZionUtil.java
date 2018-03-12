package neo.rpc.client;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
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
	 * the path to request the best node.
	 */
	private static final String V2_NETWORK_BEST_NODE = "/v2/network/best_node";

	/**
	 * the node.
	 */
	private static final String NODE = "node";

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
	 * return the http client.
	 *
	 * @return the http client.
	 */
	private static HttpClient getHttpClient() {
		final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2000)
				.setConnectionRequestTimeout(2000).setSocketTimeout(2000).build();
		final HttpClient client = HttpClientBuilder.create().disableAuthCaching().disableAutomaticRetries()
				.disableConnectionState().disableContentCompression().disableCookieManagement()
				.disableRedirectHandling().setDefaultRequestConfig(requestConfig).build();
		return client;
	}

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
			final HttpClient client = getHttpClient();
			final HttpResponse response = client.execute(get);
			LOG.debug("main net status:{}", response.getStatusLine());
			final HttpEntity entity = response.getEntity();
			final String entityStr = EntityUtils.toString(entity);
			LOG.debug("main net entityStr:{}", entityStr);
			try {
				final JSONObject json = new JSONObject(entityStr);
				return json;
			} catch (final JSONException e) {
				LOG.error("url:" + url + ";entityStr:" + entityStr, e);
				return null;
			}
		} catch (final SocketTimeoutException e) {
			LOG.debug("main net SocketTimeoutException:{}", e);
			return null;
		} catch (final SocketException e) {
			LOG.debug("main net SocketException:{}", e);
			return null;
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
		final JSONObject json = getMainNetApiJsonAtUrl(V2_NETWORK_BEST_NODE);
		if (json == null) {
			return null;
		}
		final String rpcNode = json.getString(NODE);
		LOG.debug("main net rpcNode {}", rpcNode);
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
			final HttpClient client = getHttpClient();
			final HttpResponse response = client.execute(get);
			LOG.debug("test net status:{}", response.getStatusLine());
			final HttpEntity entity = response.getEntity();
			final String entityStr = EntityUtils.toString(entity);
			LOG.debug("test net entityStr:{}", entityStr);
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
		final JSONObject json = getTestNetApiJsonAtUrl(V2_NETWORK_BEST_NODE);
		final String rpcNode = json.getString(NODE);
		LOG.info("test net rpcNode {}", rpcNode);
		return rpcNode;
	}

	/**
	 * the constructor.
	 */
	private CityOfZionUtil() {

	}

}
