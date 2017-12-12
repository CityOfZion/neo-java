package neo.rpc.client.test;

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
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.util.ConfigurationUtil;
import neo.model.util.GenesisBlockUtil;
import neo.network.LocalControllerNode;
import neo.rpc.client.CityOfZionUtil;
import neo.rpc.client.test.util.AbstractJsonMockBlockDb;
import neo.rpc.client.test.util.TestUtil;
import neo.rpc.server.CoreRpcCommandEnum;
import neo.rpc.server.CoreRpcServerUtil;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRpcServerInit {

	/**
	 * a connection exception.
	 */
	private static final String CONNECTION_EXCEPTION = "connection exception";

	/**
	 * the connection timeout, in milliseconds.
	 */
	private static final int TIMEOUT_MILLIS = 2000;

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestRpcServerInit.class);

	/**
	 * the controller.
	 */
	private static final LocalControllerNode CONTROLLER;

	static {
		final JSONObject controllerNodeConfig = ConfigurationUtil.getConfiguration();
		final JSONObject localJson = controllerNodeConfig.getJSONObject(ConfigurationUtil.LOCAL);
		localJson.put(ConfigurationUtil.BLOCK_DB_IMPL, "neo.rpc.client.test.TestRpcServerInit$JsonBlockDbImpl");
		localJson.put(ConfigurationUtil.PORT, 30333);
		CONTROLLER = new LocalControllerNode(controllerNodeConfig);
	}

	/**
	 * method for after class disposal.
	 */
	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
		CONTROLLER.stopCoreRpcServer();
	}

	/**
	 * method for before class setup.
	 */
	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");

		CONTROLLER.startCoreRpcServer();
	}

	/**
	 *
	 * @param input
	 *            the input to use.
	 * @param method
	 *            the method to call.
	 * @return the reseponse.
	 */
	private static String getCityOfZionResponse(final String input, final String method) {
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
	 * logs the response status, at the "debug" log level.
	 *
	 * @param response
	 *            the response to use.
	 */
	private static void logDebugStatus(final CloseableHttpResponse response) {
		LOG.debug("status:{}", response.getStatusLine());
	}

	/**
	 * first test, blank, so beforeClass() time doesnt throw off the metrics.
	 */
	@Test
	public void aaaFirstTest() {
	}

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
	private JSONObject createInputJson(final String rpcVersion, final String method, final JSONArray params) {
		final JSONObject inputJson = new JSONObject();
		inputJson.put(CoreRpcServerUtil.JSONRPC, rpcVersion);
		inputJson.put(CoreRpcServerUtil.METHOD, method);
		inputJson.put(CoreRpcServerUtil.PARAMS, params);
		inputJson.put(CoreRpcServerUtil.ID, 1);
		return inputJson;
	}

	/**
	 * returns the response from the RPC server.
	 *
	 * @param rpcVersion
	 *            the version to send.
	 * @param params
	 *            the parameters to send.
	 * @param method
	 *            the method to call.
	 * @return the response from the RPC server.
	 */
	private String getResponse(final String rpcVersion, final JSONArray params, final String method) {
		final String actualStrRaw;
		try {
			final JSONObject inputJson = createInputJson(rpcVersion, method, params);
			final String coreRpcNode = "http://localhost:" + CONTROLLER.getLocalNodeData().getPort();
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
			final JSONObject responseJson = new JSONObject(responseStr);

			actualStrRaw = responseJson.toString(2);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		return actualStrRaw;
	}

	/**
	 * test reading best block.
	 */
	@Test
	public void test001CoreGetBestBlockHash() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETBESTBLOCKHASH.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test001CoreGetBestBlockHash");

		final String actualStrRaw = getResponse(CoreRpcServerUtil.VERSION_2_0, params, method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading address balance.
	 */
	@Test
	public void test002CoreGetBlockCount() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETBLOCKCOUNT.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test002CoreGetBlockCount");

		final String actualStrRaw = getResponse(CoreRpcServerUtil.VERSION_2_0, params, method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading address balance.
	 */
	@Test
	public void test004CoreGetBlockWithHashVerbose() {
		final JSONArray params = new JSONArray();
		params.put(GenesisBlockUtil.GENESIS_HASH.toHexString());
		params.put(1);
		final String method = CoreRpcCommandEnum.GETBLOCK.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test004CoreGetBlockWithHashVerbose");

		final String actualStrRaw = getResponse(CoreRpcServerUtil.VERSION_2_0, params, method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading address balance.
	 */
	@Test
	@Ignore
	public void testCityOfZionAddressBalance() {
		final String input = "ANrL4vPnQCCi5Mro4fqKK1rxrkxEHqmp2E";
		final String method = "/v2/address/balance/";

		final String expectedStr = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test001CityOfZionAddressBalance");

		final String actualStr = getCityOfZionResponse(input, method);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading address balance.
	 */
	@Test
	@Ignore
	public void testCoreGetBestBlockHash() {
		final JSONArray params = new JSONArray();
		final String method = "getbestblockhash1";

		final String expectedStr = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"testCoreGetBestBlockHash");

		final String actualStr;
		try {
			final JSONObject inputJson = createInputJson(CoreRpcServerUtil.VERSION_2_0, method, params);
			final String coreRpcNode = CityOfZionUtil.getMainNetRpcNode();
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
			final JSONObject responseJson = new JSONObject(responseStr);

			actualStr = responseJson.toString(2);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}

	/**
	 * returns a BlockDb implementation for this test class.
	 *
	 * @author coranos
	 *
	 */
	public static final class JsonBlockDbImpl extends AbstractJsonMockBlockDb {

		/**
		 * the json array of test blocks.
		 */
		private final JSONArray jsonArray;

		/**
		 * the constructor.
		 */
		public JsonBlockDbImpl() {
			jsonArray = new JSONArray();
		}

		@Override
		public JSONArray getMockBlockDb() {
			return jsonArray;
		}

		@Override
		public String toString() {
			return jsonArray.toString();
		}
	}
}
