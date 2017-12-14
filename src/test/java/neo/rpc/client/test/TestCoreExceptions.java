package neo.rpc.client.test;

import java.nio.ByteBuffer;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.bytes.UInt32;
import neo.model.bytes.UInt64;
import neo.model.core.AssetType;
import neo.model.core.ContractParameterType;
import neo.model.core.Header;
import neo.model.core.TransactionAttributeUsage;
import neo.model.core.TransactionType;
import neo.model.util.GenesisBlockUtil;
import neo.rpc.client.test.util.TestUtil;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCoreExceptions {

	/**
	 * unknown typeByte:-1.
	 */
	private static final String UNKNOWN_TYPE_BYTE_NEG1 = "unknown typeByte:-1";

	/**
	 * unknown typeByte:-1.
	 */
	private static final String UNKNOWN_TYPE_BYTE_NEG2 = "unknown typeByte:-2";

	/**
	 * unknown typeByte:1.
	 */
	private static final String UNKNOWN_TYPE_BYTE_POS1 = "unknown typeByte:1";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestCoreExceptions.class);

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
	public void test001TransactionTypeValueOf() {
		try {
			TransactionType.valueOf("");
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals("No enum constant neo.model.core.TransactionType.", e.getMessage());
		}
	}

	/**
	 * test TransactionType.valueOfByte.
	 */
	@Test
	public void test002TransactionTypeValueOfByte() {
		try {
			TransactionType.valueOfByte((byte) -1);
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals(UNKNOWN_TYPE_BYTE_NEG1, e.getMessage());
		}
	}

	/**
	 * test TransactionAttributeUsage.valueOf.
	 */
	@Test
	public void test003TransactionAttributeUsageValueOf() {
		try {
			TransactionAttributeUsage.valueOf("");
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals("No enum constant neo.model.core.TransactionAttributeUsage.", e.getMessage());
		}
	}

	/**
	 * test TransactionAttributeUsage.valueOfByte.
	 */
	@Test
	public void test004TransactionAttributeUsageValueOfByte() {
		try {
			TransactionAttributeUsage.valueOfByte((byte) 1);
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals(UNKNOWN_TYPE_BYTE_POS1, e.getMessage());
		}
	}

	/**
	 * test ContractParameterType.valueOf.
	 */
	@Test
	public void test005ContractParameterTypeValueOf() {
		try {
			ContractParameterType.valueOf("");
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals("No enum constant neo.model.core.ContractParameterType.", e.getMessage());
		}
	}

	/**
	 * test ContractParameterType.valueOfByte.
	 */
	@Test
	public void test006ContractParameterTypeValueOfByte() {
		try {
			ContractParameterType.valueOfByte((byte) -2);
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals(UNKNOWN_TYPE_BYTE_NEG2, e.getMessage());
		}
	}

	/**
	 * test AssetType.valueOf.
	 */
	@Test
	public void test007AssetTypeValueOf() {
		try {
			AssetType.valueOf("");
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals("No enum constant neo.model.core.AssetType.", e.getMessage());
		}
	}

	/**
	 * test AssetType.valueOfByte.
	 */
	@Test
	public void test008AssetTypeValueOfByte() {
		try {
			AssetType.valueOfByte((byte) -1);
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals(UNKNOWN_TYPE_BYTE_NEG1, e.getMessage());
		}
	}

	/**
	 * test Header.Constructor.
	 */
	@Test
	public void test009HeaderConstructor() {
		try {
			final int minSize = (UInt32.SIZE * 3) + (UInt256.SIZE * 2) + (UInt64.SIZE) + (UInt160.SIZE) + 4;
			final byte[] ba = new byte[minSize];
			ba[ba.length - 1] = (byte) 2;
			new Header(ByteBuffer.wrap(ba));
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals("headerLastByte should be 0, was 2 at position 108", e.getMessage());
		}
	}

	/**
	 * test Header.Constructor.
	 */
	@Test
	public void test010AbstractBlockBaseConstructor() {
		try {
			final int minSize = (UInt32.SIZE * 3) + (UInt256.SIZE * 2) + (UInt64.SIZE) + (UInt160.SIZE) + 1;
			final byte[] ba = new byte[minSize];
			ba[ba.length - 1] = (byte) 2;
			new Header(ByteBuffer.wrap(ba));
			Assert.fail(TestUtil.EXPECTED_A_RUNTIME_EXCEPTION_TO_BE_THROWN);
		} catch (final RuntimeException e) {
			Assert.assertEquals("checkWitnessByte should be 1 or 0, was 2", e.getMessage());
		}
	}

	/**
	 * test GenesisBlockData.static.
	 */
	@Test
	public void test011GenesisBlockDataStatic() {
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, GenesisBlockUtil.GENESIS_HASH_HEX_STR,
				GenesisBlockUtil.GENESIS_HASH.toReverseHexString());
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}

}
