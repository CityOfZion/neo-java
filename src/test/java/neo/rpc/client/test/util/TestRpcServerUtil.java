package neo.rpc.client.test.util;

import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.network.LocalControllerNode;
import neo.rpc.client.CityOfZionUtil;
import neo.rpc.server.RpcServerUtil;

/**
 * the utilities for testing the RPC server.
 *
 * @author coranos
 *
 */
public final class TestRpcServerUtil {

	/**
	 * a connection exception.
	 */
	private static final String CONNECTION_EXCEPTION = "connection exception";

	/**
	 * the connection timeout, in milliseconds.
	 */
	private static final int TIMEOUT_MILLIS = 2000000;

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestRpcServerUtil.class);

	/**
	 * creates a RPC input JSON from the method name and parameters.
	 *
	 * @param rpcVersion
	 *            the RPC version to use.
	 * @param method
	 *            the method to use.
	 * @param params
	 *            the parameters to use.
	 * @return the input JSON.
	 */
	public static JSONObject createInputJson(final String rpcVersion, final String method, final JSONArray params) {
		final JSONObject inputJson = new JSONObject();
		inputJson.put(RpcServerUtil.JSONRPC, rpcVersion);
		inputJson.put(RpcServerUtil.METHOD, method);
		inputJson.put(RpcServerUtil.PARAMS, params);
		inputJson.put(RpcServerUtil.ID, 1);
		return inputJson;
	}

	/**
	 *
	 * @param input
	 *            the input to use.
	 * @param method
	 *            the method to call.
	 * @return the reseponse.
	 */
	public static String getCityOfZionResponse(final String input, final String method) {
		try {

			final String url = CityOfZionUtil.MAINNET_API + method + input;

			LOG.debug("url:{}", url);

			final HttpGet httpRequest = new HttpGet(url);
			final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(TIMEOUT_MILLIS)
					.setConnectTimeout(TIMEOUT_MILLIS).setConnectionRequestTimeout(TIMEOUT_MILLIS).build();
			httpRequest.setConfig(requestConfig);
			final CloseableHttpClient client = HttpClients.createDefault();
			final String responseStr;
			try {
				final CloseableHttpResponse response = client.execute(httpRequest);
				logDebugStatus(response);
				final HttpEntity entity = response.getEntity();
				responseStr = EntityUtils.toString(entity);
			} catch (final ConnectTimeoutException | SocketTimeoutException | NoHttpResponseException
					| SocketException e) {
				throw new RuntimeException(CONNECTION_EXCEPTION, e);
			}

			final JSONObject responseJson = new JSONObject(responseStr);

			final String actualStr = responseJson.toString(2);
			return actualStr;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * returns the response from the RPC server.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param uri
	 *            the uri to send.
	 * @param rpcVersion
	 *            the version to send.
	 * @param params
	 *            the parameters to send.
	 * @param method
	 *            the method to call.
	 * @return the response from the RPC server.
	 */
	public static String getResponse(final LocalControllerNode controller, final String uri, final String rpcVersion,
			final JSONArray params, final String method) {
		final String actualStrRaw;
		try {
			final JSONObject inputJson = createInputJson(rpcVersion, method, params);
			final String coreRpcNode = "http://localhost:" + controller.getLocalNodeData().getRpcPort() + uri;
			final StringEntity input = new StringEntity(inputJson.toString(), ContentType.APPLICATION_JSON);
			final HttpPost post = new HttpPost(coreRpcNode);
			final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(TIMEOUT_MILLIS)
					.setConnectTimeout(TIMEOUT_MILLIS).setConnectionRequestTimeout(TIMEOUT_MILLIS).build();
			post.setConfig(requestConfig);
			post.setEntity(input);
			final CloseableHttpClient client = HttpClients.createDefault();
			final String responseStr;
			try {
				final CloseableHttpResponse response = client.execute(post);
				logDebugStatus(response);
				final HttpEntity entity = response.getEntity();
				responseStr = EntityUtils.toString(entity);
			} catch (final ConnectTimeoutException | SocketTimeoutException | NoHttpResponseException
					| SocketException e) {
				throw new RuntimeException(CONNECTION_EXCEPTION, e);
			}
			try {
				final JSONObject responseJson = new JSONObject(responseStr);
				actualStrRaw = responseJson.toString(2);
			} catch (final JSONException e) {
				throw new RuntimeException("cannot parse text \"" + responseStr + "\"", e);
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		return actualStrRaw;
	}

	/**
	 * logs the response status, at the "debug" log level.
	 *
	 * @param response
	 *            the response to use.
	 */
	private static void logDebugStatus(final CloseableHttpResponse response) {
		LOG.debug("status:{}", response.getStatusLine());
	}

	/**
	 * the constructor.
	 */
	private TestRpcServerUtil() {

	}
}
