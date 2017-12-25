package neo.network.model.socket;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;

/**
 * the socket wrapper.
 *
 * @author coranos
 */
public interface SocketWrapper extends AutoCloseable {

	/**
	 * connect to the endpoint.
	 *
	 * @param endpoint
	 *            the endpoint to connect to.
	 * @param timeout
	 *            the timeout.
	 */
	void connect(SocketAddress endpoint, int timeout);

	/**
	 * return an input stream.
	 *
	 * @return an input stream.
	 */
	InputStream getInputStream();

	/**
	 * return an output stream.
	 *
	 * @return an output stream.
	 */
	OutputStream getOutputStream();

	/**
	 * set the socket timeout.
	 *
	 * @param timeout
	 *            the timeout.
	 */
	void setSoTimeout(int timeout);

}
