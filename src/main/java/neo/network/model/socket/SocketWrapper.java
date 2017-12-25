package neo.network.model.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketException;

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
	 * @throws IOException
	 *             if an error occurs.
	 */
	void connect(SocketAddress endpoint, int timeout) throws IOException;

	/**
	 * return an input stream.
	 *
	 * @return an input stream.
	 * @throws IOException
	 *             if an error occurs.
	 */
	InputStream getInputStream() throws IOException;

	/**
	 * return an output stream.
	 *
	 * @return an output stream.
	 * @throws IOException
	 *             if an error occurs.
	 */
	OutputStream getOutputStream() throws IOException;

	/**
	 * set the socket timeout.
	 *
	 * @param timeout
	 *            the timeout.
	 * @throws SocketException
	 *             if an error occurs.
	 */
	void setSoTimeout(int timeout) throws SocketException;

}
