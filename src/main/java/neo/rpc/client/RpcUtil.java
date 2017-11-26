package neo.rpc.client;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
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

public class RpcUtil {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RpcUtil.class);

	private static final int TIMEOUT_MILLIS = 2000;

	private static void addPeer(final JSONObject nodeListJson, final String key, final Set<String> nodeSet) {
		final JSONArray listJson = nodeListJson.getJSONObject("result").getJSONArray(key);
		final String prefix = "::ffff:";
		for (int ix = 0; ix < listJson.length(); ix++) {
			final JSONObject nodeJson = listJson.getJSONObject(ix);
			final String address = nodeJson.getString("address");
			if (address.startsWith(prefix)) {
				final String addressSuffix = address.substring(prefix.length());
				nodeJson.put("address", addressSuffix);
				nodeSet.add(addressSuffix);
			} else {
				nodeSet.add(address);
			}
		}
	}

	private static JSONObject getBlock(final String rpcNode, final int blockHeight, final Integer verbose,
			final boolean silentErrors) throws IOException, ClientProtocolException {
		final JSONArray paramsJson = new JSONArray();
		paramsJson.put(blockHeight);
		if (verbose != null) {
			paramsJson.put(verbose.intValue());
		}
		final JSONObject inputJson = new JSONObject();
		inputJson.put("jsonrpc", "2.0");
		inputJson.put("method", "getblock");
		inputJson.put("params", paramsJson);
		inputJson.put("id", 1);
		final JSONObject outputJson = post(rpcNode, silentErrors, inputJson);
		return outputJson;
	}

	public static int getBlockCount(final String rpcNode, final boolean silentErrors)
			throws ClientProtocolException, IOException {
		final JSONObject inputJson = new JSONObject(
				"{\"jsonrpc\": \"2.0\", \"method\": \"getblockcount\", \"params\": [], \"id\": 1}");
		final JSONObject outputJson = post(rpcNode, silentErrors, inputJson);
		LOG.trace("outputJson:{}", outputJson);
		if (outputJson == null) {
			return 0;
		}
		return outputJson.getInt("result");
	}

	// TODO: make private
	public static String getBlockHash(final String rpcNode, final int blockHeight, final Integer verbose,
			final boolean silentErrors) throws ClientProtocolException, IOException {
		final JSONObject outputJson = getBlock(rpcNode, blockHeight, verbose, silentErrors);
		if (outputJson == null) {
			return null;
		}
		if (verbose != null) {
			if (!outputJson.has("result")) {
				return null;
			}
			final JSONObject resultJson = outputJson.getJSONObject("result");
			resultJson.remove("tx");
			if (LOG.isTraceEnabled()) {
				LOG.trace("outputJson:{}", outputJson.toString(2));
			}
			final String hash = resultJson.getString("hash");
			return hash;
		} else {
			if (!outputJson.has("result")) {
				return null;
			}
			return outputJson.getString("result");
		}
	}

	public static String getBlockHashHex(final String rpcUrl, final int blockHeight, final boolean silentErrors)
			throws ClientProtocolException, IOException {
		return getHeaderHashHex(rpcUrl, blockHeight, silentErrors);
	}

	public static String getBlockHex(final String rpcNode, final int blockHeight, final boolean silentErrors)
			throws ClientProtocolException, IOException {
		return getBlockHash(rpcNode, blockHeight, null, silentErrors);
	}

	public static String getHeaderHashHex(final String rpcUrl, final int blockHeight, final boolean silentErrors)
			throws ClientProtocolException, IOException {
		final String hashStrRaw = getBlockHash(rpcUrl, blockHeight, 1, silentErrors);
		final String hashStr;
		if (hashStrRaw != null) {
			if (hashStrRaw.startsWith("0x")) {
				hashStr = hashStrRaw.substring(2);
			} else {
				hashStr = hashStrRaw;
			}
		} else {
			hashStr = hashStrRaw;
		}
		return hashStr;
	}

	public static JSONObject getJSONBlock(final String rpcNode, final int blockHeight, final boolean silentErrors)
			throws ClientProtocolException, IOException {
		final JSONObject outputJson = getBlock(rpcNode, blockHeight, 1, silentErrors);
		if (outputJson == null) {
			return null;
		}
		if (!outputJson.has("result")) {
			return null;
		}
		final JSONObject resultJson = outputJson.getJSONObject("result");
		return resultJson;
	}

	public static JSONObject getNeoBalance(final String rpcNode, final boolean silentErrors)
			throws IOException, ClientProtocolException {
		final JSONObject inputJson = new JSONObject(
				"{\"jsonrpc\": \"2.0\", \"method\": \"getbalance\", \"params\": [\"c56f33fc6ecfcd0c225c4ab356fee59390af8560be0e930faebe74a6daff7c9b\"], \"id\": 1}");
		final JSONObject outputJson = post(rpcNode, silentErrors, inputJson);
		return outputJson;
	}

	public static Set<String> getPeers(final String rpcNode, final boolean silentErrors)
			throws IOException, ClientProtocolException {
		final JSONObject inputJson = new JSONObject(
				"{\"jsonrpc\": \"2.0\", \"method\": \"getpeers\", \"params\": [], \"id\": 1}");
		final JSONObject outputJson = post(rpcNode, silentErrors, inputJson);
		if (outputJson == null) {
			return null;
		}
		final Set<String> peerSet = new LinkedHashSet<>();

		if (outputJson.has("result")) {
			addPeer(outputJson, "connected", peerSet);
			addPeer(outputJson, "unconnected", peerSet);
		}

		return peerSet;
	}

	public static JSONObject getVersion(final String rpcNode) throws ClientProtocolException, IOException {
		final JSONObject inputJson = new JSONObject(
				"{\"jsonrpc\": \"2.0\", \"method\": \"getversion\", \"params\": [], \"id\": 1}");
		final JSONObject outputJson = post(rpcNode, false, inputJson);
		LOG.trace("outputJson:{}", outputJson);
		return outputJson;
	}

	private static JSONObject post(final String rpcNode, final boolean silentErrors, final JSONObject inputJson)
			throws IOException, ClientProtocolException {
		LOG.debug("inputJson:{}", inputJson);
		final StringEntity input = new StringEntity(inputJson.toString(), ContentType.APPLICATION_JSON);
		final HttpPost post = new HttpPost(rpcNode);
		final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(TIMEOUT_MILLIS)
				.setConnectTimeout(TIMEOUT_MILLIS).setConnectionRequestTimeout(TIMEOUT_MILLIS).build();
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
}
