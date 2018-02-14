package neo.rpc.client.test.local;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.core.Block;
import neo.model.util.ConfigurationUtil;
import neo.network.LocalControllerNode;
import neo.rpc.client.test.util.TestRpcServerUtil;
import neo.rpc.client.test.util.TestUtil;
import neo.rpc.server.CoreRpcCommandEnum;
import neo.rpc.server.RpcServerUtil;

/**
 * tests the RPC server.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRpcServer {

	private static final String TEST_PACKAGE = "test/local";

	/**
	 * the controller.
	 */
	private static final LocalControllerNode CONTROLLER;

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestRpcServer.class);

	static {
		final JSONObject controllerNodeConfig = ConfigurationUtil.getConfiguration();
		CONTROLLER = new LocalControllerNode(controllerNodeConfig);
	}

	/**
	 * method for after class disposal.
	 */
	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
	}

	/**
	 * method for before class setup.
	 */
	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");
	}

	/**
	 * first test, blank, so beforeClass() time doesnt throw off the metrics.
	 */
	@Test
	public void aaaFirstTest() {
	}

	@Test
	public void test001GetAccountList() {
		final JSONArray params = new JSONArray();
		params.put(1512622800);
		params.put(1517806800);
		final String method = CoreRpcCommandEnum.GETACCOUNTLIST.getName();

		final Block block1674628 = CONTROLLER.getLocalNodeData().getBlockDb().getFullBlockFromHeight(1674628);
		LOG.error("block1674628 hash:{}", block1674628.hash);
		for (int ix = 0; ix < block1674628.getTransactionList().size(); ix++) {
			LOG.error("block1674628.tx[{}] hash:{}", ix, block1674628.getTransactionList().get(ix).getHash());
		}

		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test001GetAccountList");
		final JSONObject requestJson = TestRpcServerUtil.createInputJson(RpcServerUtil.VERSION_2_0, method, params);
		final String requestStr = requestJson.toString();

		final JSONObject actualJsonRaw = RpcServerUtil.process(CONTROLLER, "/", requestStr);
		final String actualStrRaw = actualJsonRaw.toString();

		final String expectedStr = new JSONObject(expectedStrRaw).toString(2);
		final String actualStr = new JSONObject(actualStrRaw).toString(2);

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}
}
