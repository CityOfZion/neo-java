package neo.rpc.client.test.testnet;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.apache.kerby.util.Hex;
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

import neo.model.bytes.UInt16;
import neo.model.core.CoinReference;
import neo.model.core.Transaction;
import neo.model.core.TransactionAttributeUsage;
import neo.model.core.TransactionOutput;
import neo.model.core.TransactionType;
import neo.model.util.ModelUtil;
import neo.rpc.client.CityOfZionUtil;
import neo.rpc.client.RpcClientUtil;

/**
 * tests the RPC server.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMinTx {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestMinTx.class);

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
	public void test001Remark() {
		// final String rpcNode = "http://seed2.neo.org:20332";//
		// CityOfZionUtil.getTestNetRpcNode();
		final String rpcNode = CityOfZionUtil.getTestNetRpcNode();
		LOG.info("test001Remark blockCount:{}:", RpcClientUtil.getBlockCount(1000, rpcNode, false));
		final byte[] txBa = new byte[800];
		txBa[0] = TransactionType.CONTRACT_TRANSACTION.getTypeByte();
		txBa[2] = 1;
		txBa[3] = TransactionAttributeUsage.REMARK_00.getTypeByte();
		txBa[4] = 4;
		final Transaction tx = new Transaction(ByteBuffer.wrap(txBa));

		tx.outputs.add(new TransactionOutput(ModelUtil.NEO_HASH, ModelUtil.getFixed8(BigInteger.ONE),
				ModelUtil.addressToScriptHash("AeKd54zJdgqXy41NgH1PicXTVcz3RdRFdh")));
		tx.inputs.add(new CoinReference(
				ModelUtil.getUInt256(ByteBuffer
						.wrap(Hex.decode("24ef2db3a509cd065c85ae33b5b905f30699d69237631598c5f182076619acc8"))),
				new UInt16(1)));
		LOG.info("test001Remark tx:{}:", tx.toJSONObject().toString(2));

		final JSONArray paramsJson = new JSONArray();
		paramsJson.put(Hex.encode(tx.toByteArray()));
		final JSONObject inputJson = new JSONObject();
		inputJson.put("jsonrpc", "2.0");
		inputJson.put("method", "sendrawtransaction");
		inputJson.put("params", paramsJson);
		inputJson.put("id", 1);

		final JSONObject outputJson = RpcClientUtil.post(1000, rpcNode, false, inputJson);
		Assert.assertNotNull("outputJson acnnot be null", outputJson);
		LOG.info("test001Remark outputJson:{}:", outputJson.toString(2));
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}
}
