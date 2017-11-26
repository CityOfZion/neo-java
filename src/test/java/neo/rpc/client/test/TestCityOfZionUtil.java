package neo.rpc.client.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.ClientProtocolException;
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
import neo.model.network.GetBlocksPayload;
import neo.model.network.Message;
import neo.model.network.VersionPayload;
import neo.rpc.client.CityOfZionUtil;
import neo.rpc.client.RpcUtil;
import neo.rpc.client.test.util.TestUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCityOfZionUtil {

	private static final Logger LOG = LoggerFactory.getLogger(TestCityOfZionUtil.class);

	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
	}

	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");
	}

	@Test
	public void test001TestNetUrl() throws ClientProtocolException, IOException {
		final String actualRpcNode = CityOfZionUtil.getTestNetRpcNode();
		Assert.assertNotNull("RpcNode cannot be null", actualRpcNode);
	}

	@Test
	public void test002MainNetUrl() throws ClientProtocolException, IOException {
		final String actualRpcNode = CityOfZionUtil.getMainNetRpcNode();
		Assert.assertNotNull("RpcNode cannot be null", actualRpcNode);
	}

	@Test
	public void test003MainNetGetHeaders() throws ClientProtocolException, IOException, DecoderException {
		final String genisisHashHexStr = "d42561e3d30e15be6400b6df2f328e02d2bf6354c41dce433bc57687c82144bf";
		final UInt256 genisisHash = new UInt256(Hex.decodeHex(genisisHashHexStr.toCharArray()));
		final String host = "seed1.neo.org";
		LOG.info("host:{}", host);
		final JSONObject error = new JSONObject();
		{
			final Message response1 = TestUtil.sendAsynchCommand(host, TestUtil.MAIN_NET_PORT, "version", error);
			Assert.assertNotNull("response cannot be null", response1);
			LOG.info("error:{}", error);
			Assert.assertEquals("error should be empty :" + error, 0, error.length());
			LOG.info("response:{}", response1.command);
			Assert.assertEquals("response1.command should be version", "version", response1.command);
		}

		{
			final Message response = TestUtil.sendAsynchCommand(host, TestUtil.MAIN_NET_PORT, "verack", error);
			Assert.assertNotNull("response cannot be null", response);
			LOG.info("error:{}", error);
			Assert.assertEquals("error should be empty :" + error, 0, error.length());
			LOG.info("response:{}", response.command);
			Assert.assertEquals("response.command should be verack", "verack", response.command);

		}
		final Message response2 = TestUtil.sendAsynchCommand(host, TestUtil.MAIN_NET_PORT, "getheaders",
				Optional.of(new GetBlocksPayload(genisisHash, null)), error);
		Assert.assertNotNull("response cannot be null", response2);
		LOG.info("error:{}", error);
		Assert.assertEquals("error should be empty :" + error, 0, error.length());
		LOG.info("response:{}", response2.command);
		Assert.assertEquals("response2.command should be headers", "headers", response2.command);
	}

	@Test
	@Ignore
	public void test004MainNetMempools() throws ClientProtocolException, IOException, DecoderException {
		final String host = "seed1.neo.org";
		LOG.info("host:{}", host);
		final JSONObject error = new JSONObject();
		final Message response1 = TestUtil.sendAsynchCommand(host, TestUtil.MAIN_NET_PORT, "version", error);
		Assert.assertNotNull("response cannot be null", response1);
		LOG.info("error:{}", error);
		Assert.assertEquals("error should be empty :" + error, 0, error.length());
		LOG.info("response:{}", response1.command);
		Assert.assertEquals("response1.command should be version", "version", response1.command);

		final Message response2 = TestUtil.sendAsynchCommand(host, TestUtil.MAIN_NET_PORT, "mempool", error);
		Assert.assertNotNull("response cannot be null", response2);
		LOG.info("error:{}", error);
		Assert.assertEquals("error should be empty :" + error, 0, error.length());
		LOG.info("response:{}", response2.command);
		Assert.assertEquals("response2.command should be inv", "inv", response2.command);
	}

	@Test
	@Ignore
	public void test005MainNetGetaddr() throws ClientProtocolException, IOException, DecoderException {
		final String host = "seed1.neo.org";
		LOG.info("host:{}", host);
		final JSONObject error = new JSONObject();
		TestUtil.sendAsynchCommand(host, TestUtil.MAIN_NET_PORT, "version", TestUtil.getMainNetVersionPayload(), error);

		final Message response2 = TestUtil.sendAsynchCommand(host, TestUtil.MAIN_NET_PORT, "getaddr", error);
		Assert.assertNotNull("response cannot be null", response2);
		LOG.info("error:{}", error);
		Assert.assertEquals("error should be empty :" + error, 0, error.length());
		LOG.info("response:{}", response2.command);
		Assert.assertEquals("response2.command should be addr", "addr", response2.command);
	}

	@Test
	public void test006MainNetVersion()
			throws ClientProtocolException, IOException, DecoderException, InterruptedException {
		final String host = "seed1.neo.org";
		LOG.info("host:{}", host);

		final List<byte[]> baList = new ArrayList<>();
		final ServerSocket serverSocket = TestUtil.startServerSocketThread(TestUtil.MAIN_NET_PORT, baList);

		final JSONObject error = new JSONObject();
		{
			final Message versionResponse = TestUtil.sendAsynchCommand(host, TestUtil.MAIN_NET_PORT, "version",
					TestUtil.getMainNetVersionPayload(), error);
			Assert.assertNull("versionResponse should be null", versionResponse);
			Thread.sleep(0000);
			final byte[] responseBa = baList.get(0);
			final ByteBuffer bb = ByteBuffer.wrap(responseBa);
			final Message response = new Message(bb);
			Assert.assertNotNull("response cannot be null", response);
			LOG.info("error:{}", error);
			Assert.assertEquals("error should be empty :" + error, 0, error.length());
			LOG.info("response:{}", response.command);
			Assert.assertEquals("response.command should be version", "version", response.command);
			final VersionPayload payload = new VersionPayload(ByteBuffer.wrap(response.getPayloadByteArray()));
			LOG.info("payload:{}", payload.userAgent);
		}
		{
			final Message response = TestUtil.sendAsynchCommand(host, TestUtil.MAIN_NET_PORT, "version",
					TestUtil.getMainNetVersionPayload(), error);
			Assert.assertNotNull("response cannot be null", response);
			LOG.info("error:{}", error);
			Assert.assertEquals("error should be empty :" + error, 0, error.length());
			LOG.info("response:{}", response.command);
			Assert.assertEquals("response.command should be version", "version", response.command);
			final VersionPayload payload = new VersionPayload(ByteBuffer.wrap(response.getPayloadByteArray()));
			LOG.info("payload:{}", payload.userAgent);
		}

		serverSocket.close();
	}

	@Test
	public void test007MainNetGenesis() throws ClientProtocolException, IOException, DecoderException {
		final String expectedGenesisHashHexStr = "0xd42561e3d30e15be6400b6df2f328e02d2bf6354c41dce433bc57687c82144bf";

		final String rpcNode = CityOfZionUtil.getMainNetRpcNode();
		final String actualGenesisHashHexStr = RpcUtil.getBlockHash(rpcNode, 0, 1, false);

		Assert.assertEquals("GenesisHash should be equal", expectedGenesisHashHexStr, actualGenesisHashHexStr);
	}

	@Test
	public void test008TestNetGenesis() throws ClientProtocolException, IOException, DecoderException {
		final String expectedGenesisHashHexStr = "0xb3181718ef6167105b70920e4a8fbbd0a0a56aacf460d70e10ba6fa1668f1fef";

		final String rpcNode = CityOfZionUtil.getTestNetRpcNode();
		final String actualGenesisHashHexStr = RpcUtil.getBlockHash(rpcNode, 0, 1, false);

		Assert.assertEquals("GenesisHash should be equal", expectedGenesisHashHexStr, actualGenesisHashHexStr);
	}

}
