package neo.rpc.client.test.local;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.export.BlockImportExportUtil;
import neo.model.util.ConfigurationUtil;
import neo.network.LocalControllerNode;

/**
 * tests the RPC server.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestImport {

	/**
	 * the controller.
	 */
	private static final LocalControllerNode CONTROLLER;

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestImport.class);

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
	public void test001Import() {
		BlockImportExportUtil.importBlocks(CONTROLLER);
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}
}
