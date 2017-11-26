package neo.rpc.client.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

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
import neo.rpc.client.RpcUtil;
import neo.rpc.client.test.util.TestUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestLocalHostTestNetUtil {

	private static final Logger LOG = LoggerFactory.getLogger(TestLocalHostTestNetUtil.class);

	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
	}

	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");
	}

	@Test
	@Ignore
	public void test001Version() throws ClientProtocolException, IOException, DecoderException, InterruptedException {
		final String host = "localhost";
		LOG.info("host:{}", host);
		final int port = 30333;
		LOG.info("port:{}", port);

		final List<byte[]> baList = new ArrayList<>();
		try (final ServerSocket serverSocket = TestUtil.startServerSocketThread(port, baList);) {
			try (Socket s = new Socket();) {
				final JSONObject error = new JSONObject();
				TestUtil.initSocket(s, host, port, error);
				Assert.assertEquals("(error.length() != 0):" + error, 0, error.length());
				try (OutputStream out = s.getOutputStream(); InputStream in = s.getInputStream()) {
					TestUtil.sendAsynchCommand(out, "version",
							TestUtil.getVersionPayload(port, ThreadLocalRandom.current().nextInt(), 0), error);
					out.flush();
					Thread.sleep(0000);
					final byte[] responseBa = baList.get(0);
					final ByteBuffer bb = ByteBuffer.wrap(responseBa);
					final Message response = new Message(bb);
					Assert.assertNotNull("response cannot be null", response);
				}
			}
		}
	}

	@Test
	// @Ignore
	public void test002GetHeaders()
			throws ClientProtocolException, IOException, DecoderException, InterruptedException {
		final String genisisHashHexStr = "b3181718ef6167105b70920e4a8fbbd0a0a56aacf460d70e10ba6fa1668f1fef";
		final UInt256 genisisHash = new UInt256(Hex.decodeHex(genisisHashHexStr.toCharArray()));
		final String host = "localhost";
		LOG.info("host:{}", host);
		final int port = 10333;
		LOG.info("port:{}", port);

		final List<byte[]> baList = new ArrayList<>();
		final ServerSocket serverSocket = TestUtil.startServerSocketThread(port, baList);

		final JSONObject error = new JSONObject();
		{
			final Message versionResponse = TestUtil.sendAsynchCommand(host, TestUtil.TEST_NET_PORT, "version",
					TestUtil.getVersionPayload(port, ThreadLocalRandom.current().nextInt(), 0), error);
			Assert.assertNull("versionResponse should be null", versionResponse);
			final Message getheadersResponse = TestUtil.sendAsynchCommand(host, TestUtil.TEST_NET_PORT, "getheaders",
					Optional.of(new GetBlocksPayload(genisisHash, null)), error);
			Assert.assertNull("getheadersResponse should not be null", getheadersResponse);
		}
		serverSocket.close();
	}

	@Test
	@Ignore
	public void test003GetHeaders()
			throws ClientProtocolException, IOException, DecoderException, InterruptedException {
		final String genisisHashHexStr = "d42561e3d30e15be6400b6df2f328e02d2bf6354c41dce433bc57687c82144bf";
		final UInt256 genisisHash = new UInt256(Hex.decodeHex(genisisHashHexStr.toCharArray()));
		final String host = "localhost";
		LOG.info("host:{}", host);
		final int port = 30333;
		LOG.info("port:{}", port);
		final int nonce = ThreadLocalRandom.current().nextInt();

		final List<byte[]> baList = new ArrayList<>();

		try (final ServerSocket serverSocket = TestUtil.startServerSocketThread(port, baList);) {
			final JSONObject error = new JSONObject();
			final Message versionRequest = new Message(TestUtil.TEST_NET_MAGIC, "version",
					TestUtil.getVersionPayload(port, nonce, 0).get().toByteArray());
			final Message getheadersRequest = new Message(TestUtil.TEST_NET_MAGIC, "getheaders",
					new GetBlocksPayload(genisisHash, null).toByteArray());
			try (Socket s = new Socket();) {
				TestUtil.initSocket(s, host, TestUtil.TEST_NET_PORT, error);
				Assert.assertEquals("(error.length() != 0):" + error, 0, error.length());
				try (OutputStream out = s.getOutputStream(); InputStream in = s.getInputStream()) {
					out.write(versionRequest.toByteArray());
					out.write(versionRequest.toByteArray());
					out.flush();
					final byte[] responseBa = TestUtil.readFully(in);
					final ByteBuffer bb = ByteBuffer.wrap(responseBa);
					final Message versionResponse = new Message(bb);
					if (versionResponse.magic != TestUtil.TEST_NET_MAGIC) {
						throw new RuntimeException("versionResponse magic was " + versionResponse.magic + " expected "
								+ TestUtil.TEST_NET_MAGIC);
					}
					Assert.assertEquals("version", versionResponse.command);
					final Message verackResponse = new Message(bb);
					if (verackResponse.magic != TestUtil.TEST_NET_MAGIC) {
						throw new RuntimeException("verackResponse magic was " + verackResponse.magic + " expected "
								+ TestUtil.TEST_NET_MAGIC);
					}
					Assert.assertEquals("verack", verackResponse.command);

					out.write(getheadersRequest.toByteArray());
					out.flush();

					Thread.sleep(1000);
					final byte[] getheadersBa = baList.get(0);
					final ByteBuffer getheadersBb = ByteBuffer.wrap(getheadersBa);
					final Message response = new Message(bb);
					Assert.assertNotNull("response cannot be null", response);

					final Message getheadersResponse = new Message(getheadersBb);
					if (getheadersResponse.magic != TestUtil.TEST_NET_MAGIC) {
						throw new RuntimeException("getheadersResponse magic was " + getheadersResponse.magic
								+ " expected " + TestUtil.TEST_NET_MAGIC);
					}
					Assert.assertEquals("headers", getheadersResponse.command);
				}
			}
		}
	}

	@Test
	@Ignore
	public void test004GetAddr() throws ClientProtocolException, IOException, DecoderException, InterruptedException {
		final String host = "seed1.neo.org";
		LOG.info("host:{}", host);
		final int rcvPort = 30333;
		LOG.info("rcvPort:{}", rcvPort);
		final int sendPort = TestUtil.MAIN_NET_PORT;
		LOG.info("sendPort:{}", sendPort);
		final int nonce = ThreadLocalRandom.current().nextInt();
		LOG.info("nonce:{}", rcvPort);
		final long magic = TestUtil.MAIN_NET_MAGIC;
		LOG.info("magic:{}", magic);

		final List<byte[]> baList = new ArrayList<>();

		try (final ServerSocket serverSocket = TestUtil.startServerSocketThread(rcvPort, baList);) {
			final JSONObject error = new JSONObject();
			final Message versionRequest = new Message(magic, "version",
					TestUtil.getVersionPayload(rcvPort, nonce, 0).get().toByteArray());
			final Message verackRequest = new Message(magic, "verack");
			final Message getaddrRequest = new Message(magic, "getaddr");
			try (Socket s = new Socket();) {
				TestUtil.initSocket(s, host, sendPort, error);
				Assert.assertEquals("(error.length() != 0):" + error, 0, error.length());
				try (OutputStream out = s.getOutputStream(); InputStream in = s.getInputStream()) {
					out.write(versionRequest.toByteArray());
					out.write(verackRequest.toByteArray());
					out.flush();
					final Message versionResponse = new Message(in);
					if (versionResponse.magic != magic) {
						throw new RuntimeException(
								"versionResponse magic was " + versionResponse.magic + " expected " + magic);
					}
					Assert.assertEquals("version", versionResponse.command);
					final Message verackResponse = new Message(in);
					if (verackResponse.magic != magic) {
						throw new RuntimeException(
								"verackResponse magic was " + verackResponse.magic + " expected " + magic);
					}
					Assert.assertEquals("verack", verackResponse.command);

					Thread.sleep(1000);
					out.write(getaddrRequest.toByteArray());
					out.flush();
					Thread.sleep(1000);
					LOG.info("baList:{}", baList);
					while (true) {
						final Message getaddrResponse = new Message(in);
						if (getaddrResponse.magic != magic) {
							throw new RuntimeException(
									"getaddrResponse magic was " + getaddrResponse.magic + " expected " + magic);
						}
						LOG.info("getaddrResponse:{}", getaddrResponse);
						Assert.assertEquals("addr", getaddrResponse.command);
					}
				}
			}
		}
	}

	@Test
	public void test005LocalHostGenesis() throws ClientProtocolException, IOException, DecoderException {
		final String expectedGenesisHashHexStr = "0xb3181718ef6167105b70920e4a8fbbd0a0a56aacf460d70e10ba6fa1668f1fef";

		final String rpcNode = "http://localhost:10332";
		final String actualGenesisHashHexStr = RpcUtil.getBlockHash(rpcNode, 0, 1, false);

		Assert.assertEquals("GenesisHash should be equal", expectedGenesisHashHexStr, actualGenesisHashHexStr);
	}
}
