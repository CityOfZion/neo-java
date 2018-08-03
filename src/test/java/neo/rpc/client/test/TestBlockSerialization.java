package neo.rpc.client.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
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
import neo.model.core.TransactionType;
import neo.model.db.BlockDb;
import neo.model.util.ModelUtil;
import neo.rpc.client.test.util.AbstractJsonMockBlockDb;
import neo.rpc.client.test.util.TestUtil;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBlockSerialization {
	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestBlockSerialization.class);

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

	/**
	 * reads in the test json for the given test name, gets the tx at the given
	 * index, and verifys that it's transaction type is the expected transaction
	 * type.
	 *
	 * @param testFunctionName
	 *            the test function name to use.
	 * @param txIx
	 *            the transaction index to use.
	 * @param expectedTransactionType
	 *            the expected transaction type to use.
	 */
	private void assertTransactionTypeEquals(final String testFunctionName, final int txIx,
			final TransactionType expectedTransactionType) {
		try {
			final String blockJsonStr = TestUtil.getJsonTestResourceAsString("test", getClass().getSimpleName(),
					testFunctionName);
			final JSONObject blockJson = new JSONObject(blockJsonStr);
			final String blockStr = TestUtil.fromHexJsonObject(blockJson);
			final byte[] blockBa = Hex.decodeHex(blockStr.toCharArray());
			final Block block = new Block(ByteBuffer.wrap(blockBa));
			final Transaction tx = block.getTransactionList().get(txIx);
			final TransactionType actulaTransactionType = tx.type;
			Assert.assertEquals("transaction types must match", expectedTransactionType, actulaTransactionType);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * test reading a miner transaction.
	 */
	@Test
	public void test001TxTypeMiner() {
		assertTransactionTypeEquals("test001TxTypeMiner", 0, TransactionType.MINER_TRANSACTION);
	}

	/**
	 * test reading a register transaction.
	 */
	@Test
	public void test002TxTypeRegister() {
		assertTransactionTypeEquals("test002TxTypeRegister", 1, TransactionType.REGISTER_TRANSACTION);
	}

	/**
	 * test reading a issue transaction.
	 */
	@Test
	public void test003TxTypeIssue() {
		assertTransactionTypeEquals("test003TxTypeIssue", 3, TransactionType.ISSUE_TRANSACTION);
	}

	/**
	 * test reading a contract transaction.
	 */
	@Test
	public void test004TxTypeContract() {
		assertTransactionTypeEquals("test004TxTypeContract", 1, TransactionType.CONTRACT_TRANSACTION);
	}

	/**
	 * test reading a claim transaction.
	 */
	@Test
	public void test005TxTypeClaim() {
		assertTransactionTypeEquals("test005TxTypeClaim", 1, TransactionType.CLAIM_TRANSACTION);
	}

	/**
	 * test reading a enrollment transaction.
	 */
	@Test
	public void test006TxTypeEnrollment() {
		assertTransactionTypeEquals("test006TxTypeEnrollment", 1, TransactionType.ENROLLMENT_TRANSACTION);
	}

	/**
	 * test reading a publish transaction.
	 */
	@Test
	public void test007TxTypePublish() {
		assertTransactionTypeEquals("test007TxTypePublish", 1, TransactionType.PUBLISH_TRANSACTION);
	}

	// 2017-12-01 16:41:44 ERROR TestBlockSerialization:75 - getBlock 1271700
	// 8ee34f98ba2eb385b2e49f9091e41098c82055d87d08e8ec38eaff444269dfdc tx 2
	// INVOCATION_TRANSACTION

	/**
	 * test reading a invocation transaction.
	 */
	@Test
	public void test008TxTypeInvocation() {
		assertTransactionTypeEquals("test008TxTypeInvocation", 2, TransactionType.INVOCATION_TRANSACTION);
	}

	/**
	 * test reading a state transaction.
	 */
	@Test
	public void test009TxTypeState() {
		assertTransactionTypeEquals("test009TxTypeState", 6, TransactionType.STATE_TRANSACTION);
	}

	/**
	 * pulls all the blocks (slow) to check for full coverage.
	 *
	 * @throws ClientProtocolException
	 *             if an error occurs.
	 * @throws IOException
	 *             if an error occurs.
	 * @throws DecoderException
	 *             if an error occurs.
	 * @throws InterruptedException
	 *             if an error occurs.
	 */
	@Test
	@Ignore
	public void test00ZAllBlocks() throws ClientProtocolException, IOException, DecoderException, InterruptedException {
		final BlockDb blockDb = new AbstractJsonMockBlockDb() {
			@Override
			public JSONArray getMockBlockDb() {
				return new JSONArray();
			}
		};
		final long maxBlockIx = blockDb.getHeaderOfBlockWithMaxIndex().getIndexAsLong();

		final Set<TransactionType> knownTypeSet = new TreeSet<>();

		for (long blockIx = 0; blockIx <= maxBlockIx; blockIx++) {
			final Block block = blockDb.getFullBlockFromHeight(blockIx);
			for (int txIx = 0; txIx < block.getTransactionList().size(); txIx++) {
				final Transaction tx = block.getTransactionList().get(txIx);
				if (!knownTypeSet.contains(tx.type)) {
					LOG.error("getBlock {} {} tx {} {}", block.getIndexAsLong(), block.prevHash, txIx, tx.type);
					knownTypeSet.add(tx.type);
				}
			}
		}
		blockDb.close();
	}

	/**
	 * test reading a miner transaction.
	 */
	@Test
	@Ignore
	public void testGetBlock() {
		final BlockDb blockDb = new AbstractJsonMockBlockDb() {
			@Override
			public JSONArray getMockBlockDb() {
				return new JSONArray();
			}
		};
		try {
			final String testName = "test001TxTypeMiner1";
			final int blockIx = 1271700;
			final String expectedBlockJsonStr = IOUtils.toString(this.getClass()
					.getResourceAsStream("/neo/rpc/client/test/TestBlockSerialization." + testName + ".json"), "UTF-8");
			final Block block = blockDb.getFullBlockFromHeight(blockIx);

			final String actualBlockHex = ModelUtil.toHexString(block.toByteArray());

			final JSONObject actualBlockJson = TestUtil.toHexJsonObject(actualBlockHex);
			final String actualBlockJsonStr = actualBlockJson.toString(2);
			Assert.assertEquals("hex encodings of blocks must match", expectedBlockJsonStr, actualBlockJsonStr);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		} finally {
			blockDb.close();
		}
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}

}
