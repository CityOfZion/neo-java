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
import neo.network.LocalControllerNode;
import neo.rpc.client.CityOfZionUtil;
import neo.rpc.client.test.util.AbstractJsonMockBlockDb;
import neo.rpc.client.test.util.TestUtil;
import neo.rpc.server.CoreRpcServerUtil;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRpcServer {

	/**
	 * asesert responses must match.
	 */
	private static final String RESPONSES_MUST_MATCH = "responses must match";

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
	private static final Logger LOG = LoggerFactory.getLogger(TestRpcServer.class);

	/**
	 * the controller.
	 */
	private static final LocalControllerNode CONTROLLER;

	static {
		final JSONObject controllerNodeConfig = ConfigurationUtil.getConfiguration();
		controllerNodeConfig.getJSONObject(ConfigurationUtil.LOCAL).put(ConfigurationUtil.BLOCK_DB_IMPL,
				"neo.rpc.client.test.TestRpcServer$JsonBlockDbImpl");
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

		if (CONTROLLER.getLocalNodeData().getBlockDb().getBlockWithMaxIndex() == null) {
			// final BlockDb realDb = new BlockDbImpl();
			// CONTROLLER.getLocalNodeData().getBlockDb().put(realDb.getBlock(0));
			// realDb.close();
			LOG.error("empty JSON db:{}", CONTROLLER.getLocalNodeData().getBlockDb());
		}

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
	 * @param method
	 *            the method to use.
	 * @param params
	 *            the parameters to use.
	 * @return the input JSON.
	 */
	private JSONObject creteInputJson(final String method, final JSONArray params) {
		final JSONObject inputJson = new JSONObject();
		inputJson.put(CoreRpcServerUtil.JSONRPC, CoreRpcServerUtil.VERSION_2_0);
		inputJson.put(CoreRpcServerUtil.METHOD, method);
		inputJson.put(CoreRpcServerUtil.PARAMS, params);
		inputJson.put(CoreRpcServerUtil.ID, 1);
		return inputJson;
	}

	/**
	 * test reading address balance.
	 */
	@Test
	public void test001CoreGetBestBlockHash() {
		final JSONArray params = new JSONArray();
		final String method = "getbestblockhash";

		final String expectedStr = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test002CoreGetBestBlockHash");

		final String actualStr;
		try {
			final JSONObject inputJson = creteInputJson(method, params);
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

			actualStr = responseJson.toString(2);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		Assert.assertEquals(RESPONSES_MUST_MATCH, expectedStr, actualStr);
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

		Assert.assertEquals(RESPONSES_MUST_MATCH, expectedStr, actualStr);
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
			final JSONObject inputJson = creteInputJson(method, params);
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

		Assert.assertEquals(RESPONSES_MUST_MATCH, expectedStr, actualStr);
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
			final String dbStr = TestUtil.getJsonTestResourceAsString("TestRpcServer", "BlockDbImpl");
			jsonArray = new JSONArray(dbStr);
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
