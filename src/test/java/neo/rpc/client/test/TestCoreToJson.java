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

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.bytes.UInt32;
import neo.model.bytes.UInt64;
import neo.model.core.ClaimExclusiveData;
import neo.model.core.CoinReference;
import neo.model.core.EnrollmentExclusiveData;
import neo.model.core.Header;
import neo.model.core.InvocationExclusiveData;
import neo.model.core.MinerExclusiveData;
import neo.model.core.NoExclusiveData;
import neo.model.core.PublishExclusiveData;
import neo.model.core.RegisterExclusiveData;
import neo.model.core.Transaction;
import neo.model.core.TransactionOutput;
import neo.model.core.Witness;
import neo.model.util.ModelUtil;
import neo.rpc.client.test.util.MockUtil;
import neo.rpc.client.test.util.TestUtil;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCoreToJson {

	private static final String TEST_PACKAGE = "test";
	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestCoreToJson.class);

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
	 * test NoExclusiveData.
	 */
	@Test
	public void test001NoExclusiveData() {
		final String expectedStr = "{}";
		final String actualStr = new NoExclusiveData(ByteBuffer.wrap(new byte[0])).toJSONObject().toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test MinerExclusiveData.
	 */
	@Test
	public void test002MinerExclusiveData() {
		final String expectedStr = "{\"nonce\":\"0\"}";
		final String actualStr = new MinerExclusiveData(ByteBuffer.wrap(new byte[UInt32.SIZE])).toJSONObject()
				.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test ClaimExclusiveData.
	 */
	@Test
	public void test003ClaimExclusiveData() {
		final String expectedStr = "{\"claims\":[]}";
		final String actualStr = new ClaimExclusiveData(ByteBuffer.wrap(new byte[UInt32.SIZE])).toJSONObject()
				.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test EnrollmentExclusiveData.
	 */
	@Test
	public void test004EnrollmentExclusiveData() {
		final String expectedStr = "{\"publicKey\":\"00\"}";
		final String actualStr = new EnrollmentExclusiveData(ByteBuffer.wrap(new byte[UInt32.SIZE])).toJSONObject()
				.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test InvocationExclusiveData.
	 */
	@Test
	public void test005InvocationExclusiveData() {
		final String expectedStr = "{\"gas\":0,\"script\":\"\"}";
		final String actualStr = new InvocationExclusiveData((byte) 0, ByteBuffer.wrap(new byte[UInt32.SIZE]))
				.toJSONObject().toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test Block.
	 */
	@Test
	public void test006Block() {
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test006Block");
		final String expectedStr = new JSONObject(expectedStrRaw).toString();
		final String actualStr = MockUtil.getMockBlock000().toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test Witness.
	 */
	@Test
	public void test007Witness() {
		final Witness witness = MockUtil.getWitness000();
		final String expectedStr = "{\"invocation\":\"\",\"verification\":\"\"}";
		final String actualStr = witness.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test Header.
	 */
	@Test
	public void test008Header() {
		final int minSize = (UInt32.SIZE * 3) + (UInt256.SIZE * 2) + (UInt64.SIZE) + (UInt160.SIZE) + 4;
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test008Header");
		final String expectedStr = new JSONObject(expectedStrRaw).toString();
		final String actualStr = new Header(ByteBuffer.wrap(new byte[minSize])).toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test CoinReference.
	 */
	@Test
	public void test009CoinReference() {
		final CoinReference coinReference = MockUtil.getCoinReference000();
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test009CoinReference");
		final String expectedStr = new JSONObject(expectedStrRaw).toString();
		final String actualStr = coinReference.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test TransactionOutput.
	 */
	@Test
	public void test010TransactionOutput() {
		final TransactionOutput transactionOutput = MockUtil.getTransactionOutput000();
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test010TransactionOutput");
		final String expectedStr = new JSONObject(expectedStrRaw).toString();
		final String actualStr = transactionOutput.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test Transaction.
	 */
	@Test
	public void test011Transaction() {
		final Transaction transaction = MockUtil.getMockTransaction000();
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test011Transaction");
		final String expectedStr = new JSONObject(expectedStrRaw).toString();
		final String actualStr = transaction.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);

		final String expectedHexStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test011TransactionHex");
		final String expectedHexStr = new JSONObject(expectedHexStrRaw).toString();
		final String actualHexStr = TestUtil.toHexJsonObject(ModelUtil.toHexString(transaction.toByteArray()))
				.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedHexStr, actualHexStr);
	}

	/**
	 * test RegisterExclusiveData.
	 */
	@Test
	public void test012RegisterExclusiveData() {
		final int minSize = Fixed8.SIZE + UInt160.SIZE + 4;
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test012RegisterExclusiveData");
		final String expectedStr = new JSONObject(expectedStrRaw).toString();
		final String actualStr = new RegisterExclusiveData(ByteBuffer.wrap(new byte[minSize])).toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * test PublishExclusiveData.
	 */
	@Test
	public void test013PublishExclusiveDataNoStorage() {
		final int minSize = 9;
		final byte[] ba = new byte[minSize];
		ba[3] = (byte) 1;
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test013PublishExclusiveDataNoStorage");
		final String expectedStr = new JSONObject(expectedStrRaw).toString();
		final PublishExclusiveData publishExclusiveData = new PublishExclusiveData((byte) 1, ByteBuffer.wrap(ba));

		final String actualStr = publishExclusiveData.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);

		final String expectedHexStr = "000000010000000000";
		final String actualHexStr = ModelUtil.toHexString(publishExclusiveData.toByteArray());
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedHexStr, actualHexStr);
	}

	/**
	 * test PublishExclusiveData.
	 */
	@Test
	public void test014PublishExclusiveDataWithStorage() {
		final int minSize = 10;
		final byte[] ba = new byte[minSize];
		ba[1] = (byte) 1;
		final String expectedStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test014PublishExclusiveDataWithStorage");
		final String expectedStr = new JSONObject(expectedStrRaw).toString();
		final PublishExclusiveData publishExclusiveData = new PublishExclusiveData((byte) 1, ByteBuffer.wrap(ba));

		final String actualStr = publishExclusiveData.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);

		final String expectedHexStr = "00010000000000000000";
		final String actualHexStr = ModelUtil.toHexString(publishExclusiveData.toByteArray());
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedHexStr, actualHexStr);
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}

}
