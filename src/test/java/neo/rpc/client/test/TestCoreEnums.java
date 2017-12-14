package neo.rpc.client.test;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.core.AssetType;
import neo.model.core.ContractParameterType;
import neo.model.core.TransactionAttributeUsage;
import neo.model.core.TransactionType;
import neo.rpc.server.CoreRpcCommandEnum;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCoreEnums {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestCoreEnums.class);

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
	 * test TransactionType.
	 */
	@Test
	public void test001TransactionType() {
		Assert.assertEquals(TransactionType.CLAIM_TRANSACTION.name(),
				TransactionType.valueOf(TransactionType.CLAIM_TRANSACTION.name()).name());
	}

	/**
	 * test TransactionAttributeUsage.
	 */
	@Test
	public void test002TransactionAttributeUsage() {
		Assert.assertEquals(TransactionAttributeUsage.CONTRACT_HASH.name(),
				TransactionAttributeUsage.valueOf(TransactionAttributeUsage.CONTRACT_HASH.name()).name());
	}

	/**
	 * test ContractParameterType.
	 */
	@Test
	public void test003ContractParameterType() {
		Assert.assertEquals(ContractParameterType.SIGNATURE.name(),
				ContractParameterType.valueOf(ContractParameterType.SIGNATURE.name()).name());
	}

	/**
	 * test AssetType.
	 */
	@Test
	public void test004AssetType() {
		Assert.assertEquals(AssetType.CREDIT_FLAG.name(), AssetType.valueOf(AssetType.CREDIT_FLAG.name()).name());
	}

	/**
	 * test CoreRpcCommandEnum.
	 */
	@Test
	public void test005CoreRpcCommandEnum() {
		Assert.assertEquals(CoreRpcCommandEnum.GETBESTBLOCKHASH.name(),
				CoreRpcCommandEnum.valueOf(CoreRpcCommandEnum.GETBESTBLOCKHASH.name()).name());
	}

	/**
	 * test CoreRpcCommandEnum.
	 */
	@Test
	public void test006CoreRpcCommandEnumValuesJSONArray() {
		Assert.assertNotNull(CoreRpcCommandEnum.getValuesJSONArray());
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}

}
