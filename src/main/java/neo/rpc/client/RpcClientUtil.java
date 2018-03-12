package neo.rpc.client;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the utility for making the Core Rpc Calls.
 *
 * @author coranos
 *
 */
public final class RpcClientUtil {

	/**
	 * the result JSON key.
	 */
	private static final String RESULT = "result";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RpcClientUtil.class);

	/**
	 * returns the block count.
	 *
	 * @param timeoutMillis
	 *            the timeout in milliseconds.
	 * @param rpcNode
	 *            the RPC node to use.
	 * @param silentErrors
	 *            if false, log all timeout errors.
	 * @return the block count.
	 */
	public static Integer getBlockCount(final long timeoutMillis, final String rpcNode, final boolean silentErrors) {
		final JSONObject inputJson = new JSONObject(
				"{\"jsonrpc\": \"2.0\", \"method\": \"getblockcount\", \"params\": [], \"id\": 1}");
		final JSONObject outputJson = post(timeoutMillis, rpcNode, silentErrors, inputJson);
		LOG.trace("getBlockCount outputJson:{}", outputJson);
		if (outputJson == null) {
			return null;
		}
		return outputJson.getInt(RESULT);
	}

	/**
	 * returns the transaction hash.
	 *
	 * @param timeoutMillis
	 *            the timeout in milliseconds.
	 * @param rpcNode
	 *            the RPC node to use.
	 * @param silentErrors
	 *            if false, log all timeout errors.
	 * @param txId
	 *            the transaction id.
	 * @return the transaction hash.
	 */
	public static JSONObject getTransactionByHash(final long timeoutMillis, final String rpcNode,
			final boolean silentErrors, final String txId) {
		final JSONArray paramsJson = new JSONArray();
		paramsJson.put(txId);
		paramsJson.put(0);
		final JSONObject inputJson = new JSONObject();
		inputJson.put("jsonrpc", "2.0");
		inputJson.put("method", "getrawtransaction");
		inputJson.put("params", paramsJson);
		inputJson.put("id", 1);

		final JSONObject outputJson = RpcClientUtil.post(1000, rpcNode, false, inputJson);
		return outputJson;
	}

	/**
	 * posts a request.
	 *
	 * @param timeoutMillis
	 *            the time to wait, in milliseconds. (used for SocketTimeout,
	 *            ConnectTimeout, and ConnectionRequestTimeout)
	 * @param rpcNode
	 *            the RPC node to use.
	 * @param silentErrors
	 *            if false, log the error to LOG.error().
	 * @param inputJson
	 *            the input JSON to use.
	 * @return the response, or null if an error occurs due to a timeout.
	 */
	public static JSONObject post(final long timeoutMillis, final String rpcNode, final boolean silentErrors,
			final JSONObject inputJson) {
		LOG.debug("inputJson:{}", inputJson);
		final StringEntity input = new StringEntity(inputJson.toString(), ContentType.APPLICATION_JSON);
		final HttpPost post = new HttpPost(rpcNode);
		final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout((int) timeoutMillis)
				.setConnectTimeout((int) timeoutMillis).setConnectionRequestTimeout((int) timeoutMillis).build();
		post.setConfig(requestConfig);
		post.setEntity(input);
		final CloseableHttpClient client = HttpClients.createDefault();
		final String str;
		try {
			final CloseableHttpResponse response = client.execute(post);
			LOG.debug("status:{}", response.getStatusLine());
			final HttpEntity entity = response.getEntity();
			str = EntityUtils.toString(entity);
		} catch (final ConnectTimeoutException | SocketTimeoutException | NoHttpResponseException | SocketException e) {
			if (!silentErrors) {
				LOG.error("post {} {} connection error:{}", rpcNode, inputJson, e.getMessage());
			}
			return null;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		if (!str.startsWith("{")) {
			if (!silentErrors) {
				LOG.error("post {} {} json error:\"{}\"", rpcNode, inputJson, str);
			}
			return null;
		}
		final JSONObject outputJson = new JSONObject(str);
		LOG.debug("outputJson:{}", outputJson.toString(2));
		return outputJson;
	}

	/**
	 * the constructor.
	 */
	private RpcClientUtil() {

	}
}
