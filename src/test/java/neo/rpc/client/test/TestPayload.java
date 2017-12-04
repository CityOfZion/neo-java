package neo.rpc.client.test;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
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

import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.Header;
import neo.model.network.HeadersPayload;
import neo.model.util.Base58Util;
import neo.model.util.GenesisBlockUtil;
import neo.rpc.client.CityOfZionUtil;
import neo.rpc.client.RpcClientUtil;
import neo.rpc.client.test.util.TestUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPayload {

	private static final Logger LOG = LoggerFactory.getLogger(TestPayload.class);

	private static final int TIMEOUT_MS = 2000;

	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
	}

	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");
	}

	@Test
	public void test001HeadersPayload()
			throws ClientProtocolException, IOException, DecoderException, InterruptedException {
		final String payloadJsonStr = IOUtils.toString(
				this.getClass().getResourceAsStream("/neo/rpc/client/test/TestPayload.test002HeadersPayload.json"),
				"UTF-8");
		final JSONArray payloadJson = new JSONArray(payloadJsonStr);
		final StringBuilder payloadSb = new StringBuilder();
		for (int ix = 0; ix < payloadJson.length(); ix++) {
			payloadSb.append(payloadJson.getString(ix));
		}
		final String payloadStr = payloadSb.toString();
		final byte[] payloadBa = Hex.decodeHex(payloadStr.toCharArray());
		final HeadersPayload hp = new HeadersPayload(ByteBuffer.wrap(payloadBa));
		Assert.assertNotNull("HeadersPayload should not be null", hp);
		Assert.assertEquals("HeadersPayload size", 2000, hp.getHeaderList().size());
	}

	@Test
	@Ignore
	public void test002GenesisHeadersPayload()
			throws ClientProtocolException, IOException, DecoderException, InterruptedException {
		final String rpcNode = CityOfZionUtil.getMainNetRpcNode();
		final String blockHexStr = RpcClientUtil.getBlockHex(TIMEOUT_MS, rpcNode, 0, false);
		final byte[] blockBa = Hex.decodeHex(blockHexStr.toCharArray());
		blockBa[108] = 0;
		final Header header = new Header(ByteBuffer.wrap(blockBa));
		Assert.assertNotNull("header should not be null", header);
		final String hashStr = RpcClientUtil.getHeaderHashHex(TIMEOUT_MS, rpcNode, 0, false);
		Assert.assertNotNull("hashStr should not be null", hashStr);
		final byte[] actualGenesisBa = Hex.decodeHex(hashStr.toCharArray());
		ArrayUtils.reverse(actualGenesisBa);
		final UInt256 actualGenesisHash = new UInt256(ByteBuffer.wrap(actualGenesisBa));
		Assert.assertEquals("genesisHash", GenesisBlockUtil.GENESIS_HASH, actualGenesisHash);

		final JSONObject expectedJsonBlock = TestUtil
				.getSorted(RpcClientUtil.getJSONBlock(TIMEOUT_MS, rpcNode, 0, false));
		expectedJsonBlock.remove("tx");
		expectedJsonBlock.remove("confirmations");
		expectedJsonBlock.remove("nonce");
		expectedJsonBlock.remove("size");
		expectedJsonBlock.remove("nextblockhash");
		expectedJsonBlock.put("nextconsensusHash",
				new String(Hex.encodeHex(Base58Util.decode(expectedJsonBlock.getString("nextconsensus")))));

		LOG.debug("expectedJSONBlock {}", expectedJsonBlock);
		LOG.debug("expectedJSONBlock.nextconsensusHash {}", expectedJsonBlock.getString("nextconsensusHash"));
		final JSONObject actualJsonBlock = TestUtil.getSorted(header.toJSONObject());
		LOG.debug("actualJSONBlock {}", actualJsonBlock);
		Assert.assertEquals("JSONBlock", expectedJsonBlock.toString(2), actualJsonBlock.toString(2));

		Assert.assertEquals("genesisHash", GenesisBlockUtil.GENESIS_HASH, header.hash);
	}

	@Test
	@Ignore
	public void test003GenesisBlockPayload()
			throws ClientProtocolException, IOException, DecoderException, InterruptedException {
		final String rpcNode = CityOfZionUtil.getMainNetRpcNode();
		final JSONObject expectedJsonBlock = TestUtil
				.getSorted(RpcClientUtil.getJSONBlock(TIMEOUT_MS, rpcNode, 0, false));
		expectedJsonBlock.remove("confirmations");
		expectedJsonBlock.remove("nonce");
		expectedJsonBlock.remove("size");
		expectedJsonBlock.remove("nextblockhash");
		expectedJsonBlock.put("nextconsensusHash",
				new String(Hex.encodeHex(Base58Util.decode(expectedJsonBlock.getString("nextconsensus")))));

		LOG.info("expectedJSONBlock {}", expectedJsonBlock.toString(2));

		final String blockHexStr = RpcClientUtil.getBlockHex(TIMEOUT_MS, rpcNode, 0, false);
		final byte[] blockBa = Hex.decodeHex(blockHexStr.toCharArray());
		final Block block = new Block(ByteBuffer.wrap(blockBa));
		Assert.assertNotNull("block should not be null", block);
		final String hashStr = RpcClientUtil.getHeaderHashHex(TIMEOUT_MS, rpcNode, 0, false);
		Assert.assertNotNull("hashStr should not be null", hashStr);
		final byte[] actualGenesisBa = Hex.decodeHex(hashStr.toCharArray());
		ArrayUtils.reverse(actualGenesisBa);
		final UInt256 actualGenesisHash = new UInt256(ByteBuffer.wrap(actualGenesisBa));
		Assert.assertEquals("genesisHash", GenesisBlockUtil.GENESIS_HASH, actualGenesisHash);

		final JSONObject actualJsonBlock = TestUtil.getSorted(block.toJSONObject());
		LOG.info("actualJSONBlock {}", actualJsonBlock);
		Assert.assertEquals("JSONBlock", expectedJsonBlock.toString(2), actualJsonBlock.toString(2));

		Assert.assertEquals("genesisHash", GenesisBlockUtil.GENESIS_HASH, block.hash);
	}

}
