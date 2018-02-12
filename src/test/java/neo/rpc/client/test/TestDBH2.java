package neo.rpc.client.test;

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
import neo.model.core.Transaction;
import neo.model.db.BlockDb;
import neo.model.util.ConfigurationUtil;
import neo.model.util.GenesisBlockUtil;
import neo.model.util.JsonUtil;
import neo.network.LocalControllerNode;
import neo.rpc.client.test.util.MockUtil;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDBH2 {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestDBH2.class);

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

	public static TestLocalControllerNode getTestLocalControllerNode() {
		final JSONObject controllerNodeConfig = ConfigurationUtil.getConfiguration();
		final JSONObject localJson = controllerNodeConfig.getJSONObject(ConfigurationUtil.LOCAL);
		final JSONObject blockDbJson = localJson.getJSONObject(ConfigurationUtil.BLOCK_DB);
		blockDbJson.put(ConfigurationUtil.URL, "jdbc:h2:mem:db");
		blockDbJson.put(ConfigurationUtil.FILE_SIZE_DIR, "src/test/resources");
		blockDbJson.put(ConfigurationUtil.IMPL, "neo.model.db.h2.BlockDbH2Impl");
		localJson.put(ConfigurationUtil.TCP_PORT, 30333);
		final JSONObject remoteJson = controllerNodeConfig.getJSONObject(ConfigurationUtil.REMOTE);
		final JSONObject recycleIntervalJson = new JSONObject();
		recycleIntervalJson.put(JsonUtil.MILLISECONDS, 0);
		remoteJson.put(ConfigurationUtil.RECYCLE_INTERVAL, recycleIntervalJson);
		return new TestLocalControllerNode(new LocalControllerNode(controllerNodeConfig));
	}

	/**
	 * first test, blank, so beforeClass() time doesnt throw off the metrics.
	 */
	@Test
	public void aaaFirstTest() {
	}

	/**
	 * test containsBlockWithHash.
	 */
	@Test
	public void test001containsBlockWithHash() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final boolean actual = controller.getBlockDb().containsBlockWithHash(GenesisBlockUtil.GENESIS_HASH);
			Assert.assertEquals("containsBlockWithHash should return false with empty db.", false, actual);
		}
	}

	/**
	 * test getFullBlockFromHash.
	 */
	@Test
	public void test002getFullBlockFromHash() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final Block actual = controller.getBlockDb().getFullBlockFromHash(GenesisBlockUtil.GENESIS_HASH);
			Assert.assertEquals("getFullBlockFromHash should return null with empty db.", null, actual);
		}
	}

	/**
	 * test getBlockCount.
	 */
	@Test
	public void test003getBlockCount() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final long actual = controller.getBlockDb().getBlockCount();
			Assert.assertEquals("getBlockCount should return 0 with empty db.", 0, actual);
		}
	}

	/**
	 * test getHeaderOfBlockWithMaxIndex.
	 */
	@Test
	public void test004getHeaderOfBlockWithMaxIndex() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final Block actual = controller.getBlockDb().getHeaderOfBlockWithMaxIndex();
			Assert.assertEquals("getHeaderOfBlockWithMaxIndex should return null with empty db.", null, actual);
		}
	}

	/**
	 * test put, and getFullBlockFromHeight.
	 */
	@Test
	public void test005putAndGetFullBlockFromHeight() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final Block expectedBlock = MockUtil.getMockBlock001();
			controller.getBlockDb().put(true, expectedBlock);
			final Block actualBlock = controller.getBlockDb().getFullBlockFromHeight(0);
			Assert.assertEquals("blocks should match.", expectedBlock.toString(), actualBlock.toString());
		}
	}

	/**
	 * test put, and getTransactionWithHash.
	 */
	@Test
	public void test006putAndGetTransactionWithHash() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final Block block = MockUtil.getMockBlock001();
			controller.getBlockDb().put(true, block);
			final Transaction expectedTransaction = block.getTransactionList().get(0);
			final Transaction actualTransaction = controller.getBlockDb()
					.getTransactionWithHash(expectedTransaction.getHash());
			Assert.assertEquals("transactions should match.", expectedTransaction.toJSONObject().toString(2),
					actualTransaction.toJSONObject().toString(2));
		}
	}

	/**
	 * test put, and getFullBlockFromHash.
	 */
	@Test
	public void test007putAndGetFullBlockFromHash() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final Block expectedBlock = MockUtil.getMockBlock001();
			controller.getBlockDb().put(true, expectedBlock);
			final Block actualBlock = controller.getBlockDb().getFullBlockFromHash(expectedBlock.hash);
			Assert.assertEquals("blocks should match.", expectedBlock.toString(), actualBlock.toString());
		}
	}

	/**
	 * test getFileSize.
	 */
	@Test
	public void test008getFileSize() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final long size = controller.getBlockDb().getFileSize();
			Assert.assertTrue("size should be positive.", size >= 0);
		}
	}

	/**
	 * test put, and getFullBlockFromHash.
	 */
	@Test
	public void test009putAndGetHeaderOfBlockWithMaxIndex() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final Block expectedBlock = MockUtil.getMockBlock001();
			controller.getBlockDb().put(true, expectedBlock);
			expectedBlock.getTransactionList().clear();
			final Block actualBlock = controller.getBlockDb().getHeaderOfBlockWithMaxIndex();
			Assert.assertEquals("blocks should match.", expectedBlock.toString(), actualBlock.toString());
		}
	}

	/**
	 * test put, and getHeaderOfBlockFromHeight.
	 */
	@Test
	public void test010putAndGetHeaderOfBlockFromHeight() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final Block expectedBlock = MockUtil.getMockBlock001();
			controller.getBlockDb().put(true, expectedBlock);
			expectedBlock.getTransactionList().clear();
			final Block actualBlock = controller.getBlockDb().getHeaderOfBlockFromHeight(0);
			Assert.assertEquals("blocks should match.", expectedBlock.toString(), actualBlock.toString());
		}
	}

	/**
	 * test put, and getHeaderOfBlockFromHash.
	 */
	@Test
	public void test011putAndGetHeaderOfBlockFromHash() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			final Block expectedBlock = MockUtil.getMockBlock001();
			controller.getBlockDb().put(true, expectedBlock);
			expectedBlock.getTransactionList().clear();
			final Block actualBlock = controller.getBlockDb().getHeaderOfBlockFromHash(expectedBlock.hash);
			Assert.assertEquals("blocks should match.", expectedBlock.toString(), actualBlock.toString());
		}
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}

	public static class TestLocalControllerNode implements AutoCloseable {
		private final LocalControllerNode controller;

		public TestLocalControllerNode(final LocalControllerNode controller) {
			this.controller = controller;
		}

		@Override
		public void close() {
			getBlockDb().close();
		}

		public BlockDb getBlockDb() {
			return controller.getLocalNodeData().getBlockDb();
		}

		public LocalControllerNode getController() {
			return controller;
		}
	}

}
