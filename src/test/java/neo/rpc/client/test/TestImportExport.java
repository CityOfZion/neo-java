package neo.rpc.client.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.export.BlockImportExportUtil;
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
public class TestImportExport {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestImportExport.class);

	private static final File TEMP_BLOCKCHAIN_DIR = new File("./test-java-chain");

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
		final File tempDbFile = new File(TEMP_BLOCKCHAIN_DIR, "db-mapdb/db.mapdb");
		blockDbJson.put(ConfigurationUtil.URL, tempDbFile.getPath());
		blockDbJson.put(ConfigurationUtil.FILE_SIZE_DIR, "src/test/resources");
		blockDbJson.put(ConfigurationUtil.IMPL, "neo.model.db.mapdb.BlockDbMapDbImpl");
		localJson.put(ConfigurationUtil.TCP_PORT, 30333);
		final JSONObject remoteJson = controllerNodeConfig.getJSONObject(ConfigurationUtil.REMOTE);
		final JSONObject recycleIntervalJson = new JSONObject();
		recycleIntervalJson.put(JsonUtil.MILLISECONDS, 0);
		remoteJson.put(ConfigurationUtil.RECYCLE_INTERVAL, recycleIntervalJson);

		final JSONObject importExportJson = localJson.getJSONObject(ConfigurationUtil.IMPORT_EXPORT);
		final File tempExportDataFile = new File(TEMP_BLOCKCHAIN_DIR, "chain.acc");
		final File tempExportStatsFile = new File(TEMP_BLOCKCHAIN_DIR, "chain-stats.json");

		importExportJson.put(ConfigurationUtil.DATA_FILE_NAME, tempExportDataFile.getPath());
		importExportJson.put(ConfigurationUtil.STATS_FILE_NAME, tempExportStatsFile.getPath());

		return new TestLocalControllerNode(new LocalControllerNode(controllerNodeConfig));
	}

	/**
	 * first test, blank, so beforeClass() time doesnt throw off the metrics.
	 */
	@Test
	public void aaaFirstTest() {
	}

	@After
	public void after() {
		try {
			FileUtils.deleteDirectory(TEMP_BLOCKCHAIN_DIR);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void before() {
		try {
			FileUtils.deleteDirectory(TEMP_BLOCKCHAIN_DIR);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * test test001ExportImport.
	 */
	@Test
	public void test001ExportImport() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			controller.getBlockDb().put(true, GenesisBlockUtil.GENESIS_BLOCK);

			BlockImportExportUtil.exportBlocks(controller.getController());
			BlockImportExportUtil.importBlocks(controller.getController());
		}
	}

	/**
	 * test test002DecapitateGenesis.
	 */
	@Test
	public void test002DecapitateGenesis() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			controller.getBlockDb().put(true, GenesisBlockUtil.GENESIS_BLOCK);
			controller.getBlockDb().deleteHighestBlock();
			controller.getBlockDb().put(true, GenesisBlockUtil.GENESIS_BLOCK);
		}
	}

	/**
	 * test test003DecapitateMockBlock000.
	 */
	@Test
	public void test003DecapitateMockBlock000() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			controller.getBlockDb().put(true, GenesisBlockUtil.GENESIS_BLOCK);
			controller.getBlockDb().put(true, MockUtil.getMockBlock000());
			controller.getBlockDb().deleteHighestBlock();
			controller.getBlockDb().put(true, MockUtil.getMockBlock000());
		}
	}

	/**
	 * test test003DecapitateMockBlock003.
	 */
	@Test
	public void test003DecapitateMockBlock003() {
		try (TestLocalControllerNode controller = getTestLocalControllerNode()) {
			controller.getBlockDb().put(true, GenesisBlockUtil.GENESIS_BLOCK);
			controller.getBlockDb().put(true, MockUtil.getMockBlock003());
			controller.getBlockDb().deleteHighestBlock();
			controller.getBlockDb().put(true, MockUtil.getMockBlock003());
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
