package neo.network.model.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * the socket wrapper implementation.
 *
 * @author coranos
 */
public final class SocketWrapperImpl implements SocketWrapper {

	/**
	 * the underlying socket.
	 */
	private final Socket socket;

	/**
	 * the constructor.
	 */
	public SocketWrapperImpl() {
		socket = new Socket();
	}

	@Override
	public void close() throws Exception {
		socket.close();
	}

	@Override
	public void connect(final SocketAddress endpoint, final int timeout) {
		try {
			socket.connect(endpoint, timeout);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputStream getInputStream() {
		try {
			return socket.getInputStream();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public OutputStream getOutputStream() {
		try {
			return socket.getOutputStream();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setSoTimeout(final int timeout) {
		try {
			socket.setSoTimeout(timeout);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

}
