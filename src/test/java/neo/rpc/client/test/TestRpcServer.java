package neo.rpc.client.test;

import java.net.InetSocketAddress;

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

import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.util.ConfigurationUtil;
import neo.model.util.GenesisBlockUtil;
import neo.network.LocalControllerNode;
import neo.network.model.NodeConnectionPhaseEnum;
import neo.network.model.RemoteNodeData;
import neo.rpc.client.test.util.AbstractJsonMockBlockDb;
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
public class TestRpcServer {

	/**
	 * localhost.
	 */
	private static final String LOCALHOST = "localhost";

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
		final JSONObject localJson = controllerNodeConfig.getJSONObject(ConfigurationUtil.LOCAL);
		final JSONObject remoteJson = controllerNodeConfig.getJSONObject(ConfigurationUtil.REMOTE);
		localJson.put(ConfigurationUtil.BLOCK_DB_IMPL, "neo.rpc.client.test.TestRpcServer$JsonBlockDbImpl");
		localJson.put(ConfigurationUtil.PORT, 30333);
		CONTROLLER = new LocalControllerNode(controllerNodeConfig);
		final RemoteNodeData ackRemoteNode = new RemoteNodeData(remoteJson);
		ackRemoteNode.setTcpAddressAndPort(new InetSocketAddress(LOCALHOST, 30333));
		ackRemoteNode.setConnectionPhase(NodeConnectionPhaseEnum.ACKNOWLEDGED);
		CONTROLLER.addToPeerDataSet(ackRemoteNode);
		final RemoteNodeData refusedRemoteNode = new RemoteNodeData(remoteJson);
		refusedRemoteNode.setTcpAddressAndPort(new InetSocketAddress(LOCALHOST, 40333));
		refusedRemoteNode.setConnectionPhase(NodeConnectionPhaseEnum.REFUSED);
		CONTROLLER.addToPeerDataSet(refusedRemoteNode);
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

		if (CONTROLLER.getLocalNodeData().getBlockDb().getHeaderOfBlockWithMaxIndex() == null) {
			// final BlockDb realDb = new BlockDbImpl();
			// CONTROLLER.getLocalNodeData().getBlockDb().put(realDb.getBlock(0));
			// realDb.close();
			throw new RuntimeException("empty JSON db:" + CONTROLLER.getLocalNodeData().getBlockDb());
		}

		CONTROLLER.startCoreRpcServer();
	}

	/**
	 * first test, blank, so beforeClass() time doesnt throw off the metrics.
	 */
	@Test
	public void aaaFirstTest() {
	}

	/**
	 * test reading best block hash.
	 */
	@Test
	public void test001CoreGetBestBlockHash() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETBESTBLOCKHASH.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test001CoreGetBestBlockHash");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading block count.
	 */
	@Test
	public void test002CoreGetBlockCount() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETBLOCKCOUNT.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test002CoreGetBlockCount");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test for errors reading best block.
	 */
	@Test
	public void test003CoreGetBestBlockHashErrors() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETBESTBLOCKHASH.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test003CoreGetBestBlockHash");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", "", params, method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading core block, verbose hash.
	 */
	@Test
	public void test004CoreGetBlockWithHashVerbose() {
		final JSONArray params = new JSONArray();
		params.put(GenesisBlockUtil.GENESIS_HASH.toHexString());
		params.put(1);
		final String method = CoreRpcCommandEnum.GETBLOCK.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
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

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test005CoreGetBlockHash");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading address default.
	 */
	@Test
	public void test006CityOfZionAddressDefault() {
		final JSONArray params = new JSONArray();
		params.put(0);
		final String method = "";

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test006CityOfZionAddressDefault");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "/address/", RpcServerUtil.VERSION_2_0,
				params, method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading block hash, no parameters.
	 */
	@Test
	public void test007CoreGetBlockHashNoParms() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETBLOCKHASH.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test007CoreGetBlockHashNoParms");

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, "", RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading connection count.
	 */
	@Test
	public void test008CoreGetConnectionCount() {
		final JSONArray params = new JSONArray();
		final String method = CoreRpcCommandEnum.GETCONNECTIONCOUNT.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test008CoreGetConnectionCount");

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
	public void test009CoreDefault() {
		final JSONArray params = new JSONArray();
		params.put(0);
		final String method = CoreRpcCommandEnum.UNKNOWN.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test009CoreDefault");

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
	public void test010CoreGetRawTransaction() {
		final JSONArray params = new JSONArray();
		final String txHash = CONTROLLER.getLocalNodeData().getBlockDb().getFullBlockFromHeight(0).getTransactionList()
				.get(0).hash.toHexString();
		params.put(txHash);
		final String method = CoreRpcCommandEnum.GETRAWTRANSACTION.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test010CoreGetRawTransaction");

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
	public void test011CoreGetTransactionOutputNoOutputs() {
		final JSONArray params = new JSONArray();
		final Transaction transaction = CONTROLLER.getLocalNodeData().getBlockDb().getFullBlockFromHeight(0)
				.getTransactionList().get(0);
		final String txHash = transaction.hash.toHexString();
		params.put(txHash);
		params.put(0);
		final String method = CoreRpcCommandEnum.GETTXOUT.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test011CoreGetTransactionOutputNoOutputs");

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
	public void test012CoreGetTransactionOutput() {
		final JSONArray params = new JSONArray();
		final Block block = CONTROLLER.getLocalNodeData().getBlockDb().getFullBlockFromHeight(0);
		final Transaction transaction = block.getTransactionList().get(block.getTransactionList().size() - 1);
		final String txHash = transaction.hash.toHexString();
		params.put(txHash);
		params.put(transaction.outputs.size() - 1);
		final String method = CoreRpcCommandEnum.GETTXOUT.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test012CoreGetTransactionOutput");

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
	public void test013CityOfZionGetTransaction() {
		final JSONArray params = new JSONArray();
		final Block block = CONTROLLER.getLocalNodeData().getBlockDb().getFullBlockFromHeight(0);
		final Transaction transaction = block.getTransactionList().get(0);
		final String txHash = transaction.hash.toHexString();
		final String uri = CityOfZionCommandEnum.TRANSACTION.getUriPrefix() + txHash;
		final String method = CoreRpcCommandEnum.UNKNOWN.getName();
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test013CityOfZionGetTransaction");

		CityOfZionCommandEnum.getCommandStartingWith(uri);

		final String actualStrRaw = TestRpcServerUtil.getResponse(CONTROLLER, uri, RpcServerUtil.VERSION_2_0, params,
				method);

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test reading core block, verbose hash.
	 */
	@Test
	public void test014CoreGetBlockWithIndexNotVerbose() {
		final JSONArray params = new JSONArray();
		params.put(0);
		params.put(0);
		final String method = CoreRpcCommandEnum.GETBLOCK.getName();

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test014CoreGetBlockWithHashNotVerbose");

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

		final String expectedStr = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
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
