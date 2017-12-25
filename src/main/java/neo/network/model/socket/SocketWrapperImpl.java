package neo.network.model.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

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
	public void connect(final SocketAddress endpoint, final int timeout) throws IOException {
		socket.connect(endpoint, timeout);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	@Override
	public void setSoTimeout(final int timeout) throws SocketException {
		socket.setSoTimeout(timeout);
	}

}
