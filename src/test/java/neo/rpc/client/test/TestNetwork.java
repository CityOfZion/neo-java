package neo.rpc.client.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import org.apache.commons.io.output.NullOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.model.network.InvPayload;
import neo.model.network.InventoryType;
import neo.model.network.Message;
import neo.model.network.VersionPayload;
import neo.model.util.ConfigurationUtil;
import neo.model.util.GenesisBlockUtil;
import neo.model.util.JsonUtil;
import neo.network.LocalControllerNode;
import neo.network.RemoteNodeControllerRunnable;
import neo.network.model.NodeConnectionPhaseEnum;
import neo.network.model.RemoteNodeData;
import neo.network.model.socket.SocketFactory;
import neo.network.model.socket.SocketWrapper;
import neo.rpc.client.test.util.AbstractJsonMockBlockDb;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNetwork {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestNetwork.class);

	/**
	 * the controller.
	 */
	private static final LocalControllerNode CONTROLLER;

	static {
		final JSONObject controllerNodeConfig = ConfigurationUtil.getConfiguration();
		final JSONObject localJson = controllerNodeConfig.getJSONObject(ConfigurationUtil.LOCAL);
		final JSONObject blockDbJson = localJson.getJSONObject(ConfigurationUtil.BLOCK_DB);
		blockDbJson.put(ConfigurationUtil.IMPL, "neo.rpc.client.test.TestNetwork$JsonBlockDbImpl");
		localJson.put(ConfigurationUtil.SOCKET_FACTORY_IMPL, "neo.rpc.client.test.TestNetwork$SocketFactoryImpl");
		localJson.put(ConfigurationUtil.TCP_PORT, 30333);
		final JSONObject remoteJson = controllerNodeConfig.getJSONObject(ConfigurationUtil.REMOTE);
		final JSONObject recycleIntervalJson = new JSONObject();
		recycleIntervalJson.put(JsonUtil.MILLISECONDS, 0);
		remoteJson.put(ConfigurationUtil.RECYCLE_INTERVAL, recycleIntervalJson);
		CONTROLLER = new LocalControllerNode(controllerNodeConfig);
	}

	/**
	 * method for after class disposal.
	 */
	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
		CONTROLLER.stop();
		CONTROLLER.getLocalNodeData().getBlockDb().close();
	}

	/**
	 * method for before class setup.
	 */
	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");
		CONTROLLER.startThreadPool();
		CONTROLLER.startRefreshThread();
	}

	/**
	 * first test, blank, so beforeClass() time doesnt throw off the metrics.
	 */
	@Test
	public void aaaFirstTest() {
	}

	/**
	 * test waiting for network.
	 */
	@Test
	public void test001WaitForNetwork() {
		final RemoteNodeData data = CONTROLLER.getNewRemoteNodeData();
		data.setConnectionPhase(NodeConnectionPhaseEnum.UNKNOWN);
		final InetSocketAddress addressAndPort = new InetSocketAddress("127.0.0.1", 0);
		data.setTcpAddressAndPort(addressAndPort);
		final RemoteNodeControllerRunnable r = new RemoteNodeControllerRunnable(CONTROLLER, data);
		r.run();
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}

	/**
	 * returns a BlockDb implementation for this test class.
	 *
	 * @author coranos
	 *
	 */
	public static final class JsonBlockDbImpl extends AbstractJsonMockBlockDb {

		/**
		 * the json array of test blocks.
		 */
		private final JSONArray jsonArray;

		/**
		 * the constructor.
		 */
		public JsonBlockDbImpl(final JSONObject config) {
			jsonArray = new JSONArray();
		}

		@Override
		public JSONArray getMockBlockDb() {
			return jsonArray;
		}

		@Override
		public String toString() {
			return jsonArray.toString();
		}
	}

	/**
	 * the socket factory implementation.
	 *
	 * @author coranos
	 *
	 */
	public static final class SocketFactoryImpl implements SocketFactory {

		@Override
		public SocketWrapper newSocketWrapper() {
			return new SocketWrapperImpl();
		}

		/**
		 * the socket wrapper implementation.
		 *
		 * @author coranos
		 *
		 */
		private static final class SocketWrapperImpl implements SocketWrapper {
			@Override
			public void close() throws Exception {
			}

			@Override
			public void connect(final SocketAddress endpoint, final int timeout) throws IOException {
			}

			@Override
			public InputStream getInputStream() throws IOException {
				final byte[] ba;
				try (ByteArrayOutputStream bout = new ByteArrayOutputStream();) {
					writeVersionMessage(bout);
					writeVerackMessage(bout);
					writeInventoryMessage(bout);
					ba = bout.toByteArray();
				}
				return new ByteArrayInputStream(ba);
			}

			@Override
			public OutputStream getOutputStream() throws IOException {
				return new NullOutputStream();
			}

			@Override
			public void setSoTimeout(final int timeout) throws SocketException {
			}

			/**
			 * writes a "inv" message to an output stream.
			 *
			 * @param bout
			 *            the output stream.
			 * @throws IOException
			 *             if an error occurs.
			 * @throws UnsupportedEncodingException
			 *             if an error occurs.
			 */
			private void writeInventoryMessage(final ByteArrayOutputStream bout)
					throws UnsupportedEncodingException, IOException {
				final InvPayload invPayload = new InvPayload(InventoryType.BLOCK, GenesisBlockUtil.GENESIS_HASH);
				final Message message = new Message(CONTROLLER.getLocalNodeData().getMagic(), CommandEnum.INV,
						invPayload.toByteArray());
				bout.write(message.toByteArray());
			}

			/**
			 * writes a "verack" message to an output stream.
			 *
			 * @param bout
			 *            the output stream.
			 * @throws IOException
			 *             if an error occurs.
			 * @throws UnsupportedEncodingException
			 *             if an error occurs.
			 */
			private void writeVerackMessage(final ByteArrayOutputStream bout)
					throws UnsupportedEncodingException, IOException {
				final Message message = new Message(CONTROLLER.getLocalNodeData().getMagic(), CommandEnum.VERACK);
				bout.write(message.toByteArray());
			}

			/**
			 * writes a "version" message to an output stream.
			 *
			 * @param bout
			 *            the output stream.
			 * @throws IOException
			 *             if an error occurs.
			 * @throws UnsupportedEncodingException
			 *             if an error occurs.
			 */
			private void writeVersionMessage(final ByteArrayOutputStream bout)
					throws IOException, UnsupportedEncodingException {
				final VersionPayload vp = new VersionPayload(0L, 0, 0, "", 0L);
				final Message message = new Message(CONTROLLER.getLocalNodeData().getMagic(), CommandEnum.VERSION,
						vp.toByteArray());
				bout.write(message.toByteArray());
			}
		}

	}
}
