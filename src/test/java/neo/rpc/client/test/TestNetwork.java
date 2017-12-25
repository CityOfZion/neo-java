package neo.rpc.client.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketException;

import org.apache.commons.io.input.NullInputStream;
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

import neo.model.util.ConfigurationUtil;
import neo.network.LocalControllerNode;
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
		localJson.put(ConfigurationUtil.BLOCK_DB_IMPL, "neo.rpc.client.test.TestNetwork$JsonBlockDbImpl");
		localJson.put(ConfigurationUtil.SOCKET_FACTORY_IMPL, "neo.rpc.client.test.TestNetwork$SocketFactoryImpl");
		localJson.put(ConfigurationUtil.PORT, 30333);
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
	 * test reading best block.
	 */
	@Test
	public void test001CoreGetBestBlockHash() {
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
		public JsonBlockDbImpl() {
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
				return new NullInputStream(0);
			}

			@Override
			public OutputStream getOutputStream() throws IOException {
				return new NullOutputStream();
			}

			@Override
			public void setSoTimeout(final int timeout) throws SocketException {
			}
		}

	}
}
