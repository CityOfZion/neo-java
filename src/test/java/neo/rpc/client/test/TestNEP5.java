package neo.rpc.client.test;

import java.nio.ByteBuffer;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.core.Transaction;
import neo.model.util.ModelUtil;
import neo.rpc.client.test.util.TestUtil;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNEP5 {

	private static final String TEST_PACKAGE = "test";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestNEP5.class);

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
	 * test TransactionType.valueOfByte.
	 */
	@Test
	public void test001Error6d07() {
		final String expectedHexStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test001Error6d07Hex");
		final String transactionHex = TestUtil.fromHexJsonObject(new JSONObject(expectedHexStrRaw));

		final Transaction transaction = new Transaction(ByteBuffer.wrap(ModelUtil.decodeHex(transactionHex)));

		final String expectedStr = new JSONObject(
				TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(), "test001Error6d07"))
						.toString();
		final String actualStr = transaction.toString();

		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);

		final String expectedHexStr = new JSONObject(expectedHexStrRaw).toString();
		final String actualHexStr = TestUtil.toHexJsonObject(ModelUtil.toHexString(transaction.toByteArray()))
				.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedHexStr, actualHexStr);
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}

}
