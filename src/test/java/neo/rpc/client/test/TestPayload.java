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

import neo.model.network.AddrPayload;
import neo.model.network.GetBlocksPayload;
import neo.model.network.HeadersPayload;
import neo.model.util.GenesisBlockUtil;
import neo.model.util.ModelUtil;
import neo.rpc.client.test.util.TestUtil;

/**
 * test the network payloads.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPayload {

	private static final String TEST_PACKAGE = "test";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestPayload.class);

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
	 * tests the AddrPayload.
	 */
	@Test
	public void test001AddrPayload() {
		final String payloadJsonStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test001AddrPayloadHex");

		final String payloadJsonStr = TestUtil.fromHexJsonObject(new JSONObject(payloadJsonStrRaw));

		final byte[] payloadBa = ModelUtil.decodeHex(payloadJsonStr);
		final AddrPayload payload = new AddrPayload(ByteBuffer.wrap(payloadBa));
		Assert.assertNotNull("AddrPayload should not be null", payload);
		Assert.assertEquals("AddrPayload size", 1, payload.getAddressList().size());
		Assert.assertEquals("AddrPayload[0] ToString", "00000000000000000000000000000000:0",
				payload.getAddressList().get(0).toString());

		final String expectedStr = new JSONObject(
				TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(), "test001AddrPayload"))
						.toString();

		final String actualStr = payload.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);

		final String expectedHexStr = new JSONObject(payloadJsonStrRaw).toString(2);

		final String actualHexStr = TestUtil.toHexJsonObject(ModelUtil.toHexString(payload.toByteArray())).toString(2);
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedHexStr, actualHexStr);
	}

	/**
	 * tests the HeadersPayload.
	 */
	@Test
	public void test002HeadersPayload() {
		final String payloadJsonStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test002HeadersPayloadHex");

		final String payloadJsonStr = TestUtil.fromHexJsonObject(new JSONObject(payloadJsonStrRaw));

		final byte[] payloadBa = ModelUtil.decodeHex(payloadJsonStr);
		final HeadersPayload payload = new HeadersPayload(ByteBuffer.wrap(payloadBa));
		Assert.assertNotNull("HeadersPayload should not be null", payload);
		Assert.assertEquals("HeadersPayload size", 2, payload.getHeaderList().size());

		final String expectedStr = new JSONObject(
				TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(), "test002HeadersPayload"))
						.toString();

		final String actualStr = payload.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);

		final String expectedHexStr = new JSONObject(payloadJsonStrRaw).toString(2);

		final String actualHexStr = TestUtil.toHexJsonObject(ModelUtil.toHexString(payload.toByteArray())).toString(2);
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedHexStr, actualHexStr);
	}

	/**
	 * tests the GetBlocksPayload.
	 */
	@Test
	public void test003GetBlocksPayload() {
		final String payloadJsonStrRaw = TestUtil.getJsonTestResourceAsString(TEST_PACKAGE, getClass().getSimpleName(),
				"test003GetBlocksPayloadHex");

		final String payloadJsonStr = TestUtil.fromHexJsonObject(new JSONObject(payloadJsonStrRaw));

		final byte[] payloadBa = ModelUtil.decodeHex(payloadJsonStr);
		final GetBlocksPayload payload = new GetBlocksPayload(ByteBuffer.wrap(payloadBa));
		Assert.assertNotNull("GetBlocksPayload should not be null", payload);

		final String expectedStr = new JSONObject(TestUtil.getJsonTestResourceAsString(TEST_PACKAGE,
				getClass().getSimpleName(), "test003GetBlocksPayload")).toString();

		final String actualStr = payload.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);

		final String expectedHexStr = new JSONObject(payloadJsonStrRaw).toString(2);

		final String actualHexStr = TestUtil.toHexJsonObject(ModelUtil.toHexString(payload.toByteArray())).toString(2);
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedHexStr, actualHexStr);
	}

	/**
	 * tests the GetBlocksPayload getting the genesis block.
	 */
	@Test
	public void test004GetBlocksPayloadGenesis() {
		final GetBlocksPayload payload = new GetBlocksPayload(GenesisBlockUtil.GENESIS_HASH, null);

		final String expectedStr = new JSONObject(TestUtil.getJsonTestResourceAsString(TEST_PACKAGE,
				getClass().getSimpleName(), "test004GetBlocksPayloadGenesis")).toString();

		final String actualStr = payload.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}

	/**
	 * tests the GetBlocksPayload getting the genesis block.
	 */
	@Test
	public void test005GetBlocksPayloadGenesisToGenesis() {
		final GetBlocksPayload payload = new GetBlocksPayload(GenesisBlockUtil.GENESIS_HASH,
				GenesisBlockUtil.GENESIS_HASH);

		final String expectedStr = new JSONObject(TestUtil.getJsonTestResourceAsString(TEST_PACKAGE,
				getClass().getSimpleName(), "test005GetBlocksPayloadGenesisToGenesis")).toString();

		final String actualStr = payload.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);
	}
}
