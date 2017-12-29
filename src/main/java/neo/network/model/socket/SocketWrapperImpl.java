package neo.network.model.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import org.apache.hadoop.net.SocketInputStream;
import org.apache.hadoop.net.SocketOutputStream;

/**
 * the socket wrapper implementation.
 *
 * @author coranos
 */
public final class SocketWrapperImpl implements SocketWrapper {

	/**
	 * the socket write timeout.
	 */
	private static final int SOCKET_WRITE_TIMEOUT_MS = 2000;

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
	public void connect(final SocketAddress endpoint, final int timeout) throws IOException {
		socket.connect(endpoint, timeout);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new SocketInputStream(socket);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new SocketOutputStream(socket, SOCKET_WRITE_TIMEOUT_MS);
	}

	@Override
	public void setSoTimeout(final int timeout) throws SocketException {
		socket.setSoTimeout(timeout);
	}

}
