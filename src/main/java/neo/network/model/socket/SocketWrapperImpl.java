package neo.network.model.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;

import org.apache.hadoop.net.SocketInputStream;
import org.apache.hadoop.net.SocketOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the socket wrapper implementation.
 *
 * @author coranos
 */
public final class SocketWrapperImpl implements SocketWrapper {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(SocketWrapperImpl.class);

	/**
	 * the socket write timeout.
	 */
	private static final int SOCKET_TIMEOUT_MS = 2000;

	/**
	 * the underlying socket.
	 */
	private final SocketChannel socketChannel;

	/**
	 * the constructor.
	 */
	public SocketWrapperImpl() {
		try {
			socketChannel = SocketChannel.open();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws Exception {
		socketChannel.close();
	}

	@Override
	public void connect(final SocketAddress endpoint, final int timeout) throws IOException {
		try {
			socketChannel.socket().connect(endpoint, timeout);
			socketChannel.finishConnect();
		} catch (final ClosedByInterruptException e) {
			LOG.debug("SocketWrapperImpl.connect:{}", e.getMessage());
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new SocketInputStream(socketChannel, SOCKET_TIMEOUT_MS);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new SocketOutputStream(socketChannel, SOCKET_TIMEOUT_MS);
	}

	@Override
	public void setSoTimeout(final int timeout) throws SocketException {
		socketChannel.socket().setSoTimeout(timeout);
	}

}
