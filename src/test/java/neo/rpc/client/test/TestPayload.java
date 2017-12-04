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

import neo.model.network.HeadersPayload;
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
	 * tests the HeadersPayload.
	 */
	@Test
	public void test001HeadersPayload() {
		final String payloadJsonStrRaw = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
				"test002HeadersPayloadHex");

		final String payloadJsonStr = TestUtil.fromHexJsonObject(new JSONObject(payloadJsonStrRaw));

		final byte[] payloadBa = ModelUtil.decodeHex(payloadJsonStr);
		final HeadersPayload payload = new HeadersPayload(ByteBuffer.wrap(payloadBa));
		Assert.assertNotNull("HeadersPayload should not be null", payload);
		Assert.assertEquals("HeadersPayload size", 2, payload.getHeaderList().size());

		final String expectedStr = new JSONObject(
				TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(), "test002HeadersPayload")).toString();

		final String actualStr = payload.toString();
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedStr, actualStr);

		final String expectedHexStr = payloadJsonStrRaw;

		final String actualHexStr = TestUtil.toHexJsonObject(ModelUtil.toHexString(payload.toByteArray())).toString(2);
		Assert.assertEquals(TestUtil.RESPONSES_MUST_MATCH, expectedHexStr, actualHexStr);
	}

}
