package neo.rpc.client.test.local;

import java.sql.Timestamp;
import java.text.NumberFormat;

import org.apache.commons.lang3.time.FastDateFormat;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.ScriptVerificationResultEnum;
import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.core.TransactionType;
import neo.model.db.BlockDb;
import neo.model.util.ConfigurationUtil;
import neo.model.util.VerifyScriptUtil;
import neo.network.LocalControllerNode;
import neo.network.model.LocalNodeData;

/**
 * tests the RPC server.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestVm {

	/**
	 * the integer format.
	 */
	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance();

	/**
	 * the date format.
	 */
	private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy.MM.dd");

	/**
	 * the controller.
	 */
	private static final LocalControllerNode CONTROLLER;

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestVm.class);

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
	@Ignore
	public void test001tx3631f66024ca6f5b033d7e0809eb993443374830025af904fb51b0334f127cda() {
		final long blockHeight = 0;
		final int txIx = 3;
		testTransaction(blockHeight, txIx);
	}

	@Test
	// @Ignore
	public void test002ee85d489e4428a538f39c1903771e1f222a383f8327c96ed19cc02079149a1fd() {
		final long blockHeight = 4130;
		final int txIx = 1;
		testTransaction(blockHeight, txIx);
	}

	@Test
	public void test999Vm() {
		LOG.info("STARTED vm");
		final LocalNodeData localNodeData = CONTROLLER.getLocalNodeData();
		final BlockDb blockDb = localNodeData.getBlockDb();
		final long maxIndex = blockDb.getHeaderOfBlockWithMaxIndex().getIndexAsLong();
		long startMs = -1;
		for (long blockHeight = 0; blockHeight <= maxIndex; blockHeight++) {
			LOG.info("STARTED block {} of {} ", blockHeight, maxIndex);
			final Block block = blockDb.getFullBlockFromHeight(blockHeight);

			final int maxTxIx = block.getTransactionList().size();
			for (int txIx = 0; txIx < maxTxIx; txIx++) {
				final Transaction tx = block.getTransactionList().get(txIx);
				if (tx.type.equals(TransactionType.ISSUE_TRANSACTION)) {
					LOG.info("SKIPPED block {} of {} tx {} of {} : {}", blockHeight, maxIndex, txIx, maxTxIx,
							tx.getHash());
				} else {
					LOG.info("STARTED block {} of {} tx {} of {} : {} {}", blockHeight, maxIndex, txIx, maxTxIx,
							tx.type, tx.getHash());
					final ScriptVerificationResultEnum verifyScriptsResult = VerifyScriptUtil.verifyScripts(blockDb,
							tx);
					if (!verifyScriptsResult.equals(ScriptVerificationResultEnum.PASS)) {
						LOG.error("FAILURE block {} of {} tx {} of {} : {} {} : {}", blockHeight, maxIndex, txIx,
								maxTxIx, tx.type, tx.getHash(), verifyScriptsResult);
						throw new RuntimeException("script failed : " + tx.type + ":" + verifyScriptsResult);
					} else {
						LOG.info("SUCCESS block {} of {} tx {} of {} : {} {}", blockHeight, maxIndex, txIx, maxTxIx,
								tx.type, tx.getHash());
					}
				}
			}

			final Timestamp blockTs = block.getTimestamp();
			if (startMs < 0) {
				startMs = blockTs.getTime();
			}
			final long ms = blockTs.getTime() - startMs;
			if (ms > (86400 * 1000)) {
				final String targetDateStr = DATE_FORMAT.format(blockTs);

				LOG.info("INTERIM vm {} of {}, date {}", INTEGER_FORMAT.format(blockHeight),
						INTEGER_FORMAT.format(maxIndex), targetDateStr);

				startMs = blockTs.getTime();
			}
		}
		LOG.debug("SUCCESS vm");
	}

	private void testTransaction(final long blockHeight, final int txIx) {
		final LocalNodeData localNodeData = CONTROLLER.getLocalNodeData();
		final BlockDb blockDb = localNodeData.getBlockDb();
		final Block block = blockDb.getFullBlockFromHeight(blockHeight);
		final int maxTxIx = block.getTransactionList().size();
		final Transaction tx = block.getTransactionList().get(txIx);
		final long maxIndex = blockDb.getHeaderOfBlockWithMaxIndex().getIndexAsLong();
		LOG.info("STARTED block {} of {} tx {} of {} : {} {}", blockHeight, maxIndex, txIx, maxTxIx, tx.type,
				tx.getHash());
		final ScriptVerificationResultEnum verifyScriptsResult = VerifyScriptUtil.verifyScripts(blockDb, tx);
		if (!verifyScriptsResult.equals(ScriptVerificationResultEnum.PASS)) {
			LOG.error("FAILURE block {} of {} tx {} of {} : {} {} : {}", blockHeight, maxIndex, txIx, maxTxIx, tx.type,
					tx.getHash(), verifyScriptsResult);
			throw new RuntimeException("script failed : " + verifyScriptsResult);
		} else {
			LOG.info("SUCCESS block {} of {} tx {} of {} : {} {}", blockHeight, maxIndex, txIx, maxTxIx, tx.type,
					tx.getHash());
		}
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}
}
