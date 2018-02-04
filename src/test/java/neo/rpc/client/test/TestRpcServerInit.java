package neo.rpc.client.test;

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
import neo.model.util.ModelUtil;
import neo.network.LocalControllerNode;
import neo.rpc.client.test.util.AbstractJsonMockBlockDb;
import neo.rpc.client.test.util.MockUtil;
import neo.rpc.client.test.util.TestRpcServerUtil;
import neo.rpc.client.test.util.TestUtil;
import neo.rpc.server.CityOfZionCommandEnum;
import neo.rpc.server.CoreRpcCommandEnum;
import neo.rpc.server.RpcServerUtil;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRpcServerInit {

	private static final String TEST_PACKAGE = "test";

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
		final JSONObject blockDbJson = localJson.getJSONObject(ConfigurationUtil.BLOCK_DB);
		blockDbJson.put(ConfigurationUtil.IMPL, "neo.rpc.client.test.TestRpcServerInit$JsonBlockDbImpl");
		localJson.put(ConfigurationUtil.RPC_PORT, 30332);
		localJson.getJSONObject(ConfigurationUtil.RPC).put(ConfigurationUtil.DISABLE, new JSONArray());
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
	 * first test, blank, so beforeClass() time doesnt throw off the metrics.
	 */
	@Test
	public void aaaFirstTest() {
	}

	/**
	 * test reading best block.
	 */
	@Test
	public void test001CoreGetBestBlockHash() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETBESTBLOCKHASH.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test001CoreGetBestBlockHash");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

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

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test002CoreGetBlockCount");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

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

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test004CoreGetBlockWithHashVerbose");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading block hash.
	 */
	@Test
	public void test005CoreGetBlockHash() {
		final JSONArray params = new JSONArray();
		params.put(0);
		final String method = CoreRpcCommandEnum.GETBLOCKHASH.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test005CoreGetBlockHash");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading block hash.
	 */
	@Test
	public void test006CoreGetBlockBlankParms() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETBLOCK.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test006CoreGetBlockBlankParms");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading block hash.
	 */
	@Test
	public void test007CoreGetBlockHashBlankParms() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETBLOCK.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test007CoreGetBlockHashBlankParms");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading block hash.
	 */
	@Test
	public void test008CoreGetBlockWithIndex1() {
		final JSONArray params = new JSONArray();
		params.put(1);
		params.put(0);
		final String method = CoreRpcCommandEnum.GETBLOCK.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test008CoreGetBlockWithIndex1");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading block hash.
	 */
	@Test
	public void test009CoreGetBlockWithHash() {
		final JSONArray params = new JSONArray();
		params.put(GenesisBlockUtil.GENESIS_HASH.toHexString());
		params.put("");
		final String method = CoreRpcCommandEnum.GETBLOCK.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test009CoreGetBlockWithHash");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading block hash.
	 */
	@Test
	public void test010CoreGetBlockWithIndex0() {
		final JSONArray params = new JSONArray();
		params.put(true);
		final String method = CoreRpcCommandEnum.GETBLOCK.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test010CoreGetBlockWithIndex0");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading core address with no subcategory.
	 */
	@Test
	public void test011CityOfZionGetTransactionBlankHash() {
		final JSONArray params = new JSONArray();
		final String txHash = "";
		final String uri = CityOfZionCommandEnum.TRANSACTION.getUriPrefix() + txHash;
		final String method = CoreRpcCommandEnum.UNKNOWN.getName();
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test011CityOfZionGetTransactionBlankHash");

		CityOfZionCommandEnum.getCommandStartingWith(uri);

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, uri, RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading core address with no subcategory.
	 */
	@Test
	public void test012CityOfZionGetTransactionBadHash() {
		final JSONArray params = new JSONArray();
		final String txHash = "0";
		final String uri = CityOfZionCommandEnum.TRANSACTION.getUriPrefix() + txHash;
		final String method = CoreRpcCommandEnum.UNKNOWN.getName();
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test012CityOfZionGetTransactionBadHash");

		CityOfZionCommandEnum.getCommandStartingWith(uri);

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, uri, RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test submitting core block.
	 */
	@Test
	public void test013CoreSubmitBlock() {
		final JSONArray params = new JSONArray();
		params.put(ModelUtil.toHexString(MockUtil.getMockBlock000().toByteArray()));
		final String method = CoreRpcCommandEnum.SUBMITBLOCK.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test013CoreSubmitBlock");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

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

		final String expectedStr = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test001CityOfZionAddressBalance");

		final String actualStr = TestRpcServerUtil.getCityOfZionResponse(input, method);

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
		public JsonBlockDbImpl(final JSONObject config) {
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
