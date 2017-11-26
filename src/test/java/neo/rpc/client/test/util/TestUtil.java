package neo.rpc.client.test.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.keystore.ByteArraySerializable;
import neo.model.network.Message;
import neo.model.util.PayloadUtil;

public class TestUtil {

	private static final Logger LOG = LoggerFactory.getLogger(TestUtil.class);

	public static final int MAIN_NET_PORT = 10333;

	public static final int TEST_NET_PORT = 20333;

	public static final long MAIN_NET_MAGIC = 7630401;

	public static final long TEST_NET_MAGIC = 1953787457;

	public static Optional<ByteArraySerializable> getMainNetVersionPayload() {
		return getVersionPayload(MAIN_NET_PORT, ThreadLocalRandom.current().nextInt(), 0);
	}

	public static JSONObject getSorted(final JSONObject obj) {
		final JSONObject json = new JSONObject();
		try {
			final Field map = json.getClass().getDeclaredField("map");
			map.setAccessible(true);
			map.set(json, new TreeMap<>());
			map.setAccessible(false);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		for (final String key : obj.keySet()) {
			json.put(key, obj.get(key));
		}

		return json;
	}

	public static Optional<ByteArraySerializable> getTestNetVersionPayload() {
		return getVersionPayload(TEST_NET_PORT, ThreadLocalRandom.current().nextInt(), 0);
	}

	public static final Optional<ByteArraySerializable> getVersionPayload(final int port, final int nonce,
			final long startHeight) {
		return Optional.of(PayloadUtil.getVersionPayload(port, nonce, startHeight));
	}

	public static void initSocket(final Socket s, final String host, final int port, final JSONObject error)
			throws SocketException, IOException {
		final SocketAddress endpoint = new InetSocketAddress(host, port);
		try {
			s.setSoTimeout(1000);
			s.connect(endpoint, 1000);
		} catch (SocketTimeoutException | ConnectException | UnknownHostException e) {
			error.put("class", e.getClass().getSimpleName());
			error.put("message", e.getMessage());
		}
	}

	public static Message readAsynchCommand(final InputStream in, final JSONObject error) throws IOException {
		final byte[] responseBa = readFully(in);
		LOG.info("<<<:{}", Hex.encodeHexString(responseBa));
		if (responseBa.length == 0) {
			return null;
		}
		final ByteBuffer bb = ByteBuffer.wrap(responseBa);
		final Message responseMessage = new Message(bb);
		if (responseMessage.magic != MAIN_NET_MAGIC) {
			throw new RuntimeException("response magic was " + responseMessage.magic + " expected " + MAIN_NET_MAGIC);
		}
		return responseMessage;
	}

	public static byte[] readFully(final InputStream in) throws IOException {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		final byte[] ba = new byte[1];
		while (in.read(ba) > 0) {
			bout.write(ba);
		}

		return bout.toByteArray();
	}

	private static byte[] sendAsynch(final String host, final int port, final byte[] message, final JSONObject error)
			throws IOException {
		try (Socket s = new Socket();) {
			initSocket(s, host, port, error);
			if (error.length() != 0) {
				return new byte[0];
			}
			try (OutputStream out = s.getOutputStream(); InputStream in = s.getInputStream()) {
				out.write(message);
				return readFully(in);
			} catch (final SocketTimeoutException e) {
				error.put("class", e.getClass().getSimpleName());
				error.put("message", e.getMessage());
				return new byte[0];
			}
		}
	}

	public static void sendAsynchCommand(final OutputStream out, final String command,
			final Optional<ByteArraySerializable> payload, final JSONObject error) throws IOException {
		final byte[] payloadBa;
		if (payload.isPresent()) {
			payloadBa = payload.get().toByteArray();
		} else {
			payloadBa = new byte[0];
		}
		final Message requestMessage = new Message(TestUtil.MAIN_NET_MAGIC, command, payloadBa);
		final byte[] requestBa = requestMessage.toByteArray();
		LOG.info(">>>:{}", Hex.encodeHexString(requestBa));
		out.write(requestBa);
	}

	public static Message sendAsynchCommand(final String host, final int port, final String command,
			final JSONObject error) throws IOException {
		return sendAsynchCommand(host, port, command, Optional.empty(), error);
	}

	public static Message sendAsynchCommand(final String host, final int port, final String command,
			final Optional<ByteArraySerializable> payload, final JSONObject error) throws IOException {
		final byte[] payloadBa;
		if (payload.isPresent()) {
			payloadBa = payload.get().toByteArray();
		} else {
			payloadBa = new byte[0];
		}
		final Message requestMessage = new Message(TestUtil.MAIN_NET_MAGIC, command, payloadBa);
		final byte[] requestBa = requestMessage.toByteArray();
		LOG.info(">>>:{}", Hex.encodeHexString(requestBa));
		final byte[] responseBa = sendAsynch(host, port, requestBa, error);
		LOG.info("<<<:{}", Hex.encodeHexString(responseBa));
		if (responseBa.length == 0) {
			return null;
		}
		final ByteBuffer bb = ByteBuffer.wrap(responseBa);
		final Message responseMessage = new Message(bb);
		if (responseMessage.magic != MAIN_NET_MAGIC) {
			throw new RuntimeException("response magic was " + responseMessage.magic + " expected " + MAIN_NET_MAGIC);
		}
		return responseMessage;
	}

	public static ServerSocket startServerSocketThread(final int port, final List<byte[]> baList) throws IOException {
		final ServerSocket serverSocket = new ServerSocket(port);
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					LOG.info("socket accepting");
					final Socket s = serverSocket.accept();
					LOG.info("socket accepted!");
					try (InputStream in = s.getInputStream()) {
						final byte[] ba = readFully(in);
						baList.add(ba);
					} catch (final SocketTimeoutException e) {
						throw new RuntimeException(e);
					}
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();
		return serverSocket;
	}

}
