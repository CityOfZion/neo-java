package neo.rpc.client.test;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.core.Block;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class TestLoadSave {

	private static final Logger LOG = LoggerFactory.getLogger(TestLoadSave.class);

	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
	}

	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");
	}

	@Test
	public void test001Block() throws IOException, DecoderException {
		final String expected = IOUtils.toString(
				this.getClass().getResourceAsStream("/neo/rpc/client/test/TestLoadSave.test001Block.hex"), "UTF-8");
		testBlockRoundTrip(expected);
	}

	@Test
	public void test002Block() throws IOException, DecoderException {
		final String expected = IOUtils.toString(
				this.getClass().getResourceAsStream("/neo/rpc/client/test/TestLoadSave.test002Block.hex"), "UTF-8");
		testBlockRoundTrip(expected);
	}

	@Test
	public void test003Block() throws IOException, DecoderException {
		final String expected = IOUtils.toString(
				this.getClass().getResourceAsStream("/neo/rpc/client/test/TestLoadSave.test003Block.hex"), "UTF-8");
		testBlockRoundTrip(expected);
	}

	@Test
	public void test004Block() throws IOException, DecoderException {
		final String expected = IOUtils.toString(
				this.getClass().getResourceAsStream("/neo/rpc/client/test/TestLoadSave.test004Block.hex"), "UTF-8");
		testBlockRoundTrip(expected);
	}

	@Test
	public void test005Block() throws IOException, DecoderException {
		final String expected = IOUtils.toString(
				this.getClass().getResourceAsStream("/neo/rpc/client/test/TestLoadSave.test005Block.hex"), "UTF-8");
		testBlockRoundTrip(expected);
	}

	@Test
	public void test006Block() throws IOException, DecoderException {
		final String expected = IOUtils.toString(
				this.getClass().getResourceAsStream("/neo/rpc/client/test/TestLoadSave.test006Block.hex"), "UTF-8");
		testBlockRoundTrip(expected);
	}

	public void testBlockRoundTrip(final String expected) throws DecoderException {
		final ByteBuffer bb = ByteBuffer.wrap(Hex.decodeHex(expected.toCharArray()));
		final Block block = new Block(bb);
		final String actual = Hex.encodeHexString(block.toByteArray());
		if (!expected.equals(actual)) {
			LOG.error("block:{}", block);
			LOG.error("expected:{}", expected);
			LOG.error("actual:  {}", actual);
		}

		Assert.assertEquals(expected, actual);
	}
}
