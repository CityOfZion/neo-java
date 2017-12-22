package neo.model.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility for reading input streams.
 *
 * @author coranos
 *
 */
public final class InputStreamUtil {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(InputStreamUtil.class);

	/**
	 * reads the input stream until full.
	 *
	 * @param readTimeOut
	 *            the read timeout.
	 * @param in
	 *            the input stream to read.
	 * @param ba
	 *            the byte array to read into.
	 * @throws IOException
	 *             if an error occurs.
	 */
	public static void readUntilFull(final long readTimeOut, final InputStream in, final byte[] ba) throws IOException {
		int bytesRead = 0;
		while (bytesRead < ba.length) {
			final long currentTimeMillis = System.currentTimeMillis();
			final long timeoutMs = currentTimeMillis + readTimeOut;
			if (currentTimeMillis > timeoutMs) {
				throw new SocketTimeoutException();
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("STARTED readUntilFull {} of {} ", bytesRead, ba.length);
			}
			final int readBlock = in.read(ba, bytesRead, ba.length - bytesRead);
			if (readBlock == -1) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("FAILURE readUntilFull {} of {} ", bytesRead, ba.length);
				}
				throw new SocketTimeoutException();
			}
			bytesRead += readBlock;
		}
		if (LOG.isTraceEnabled()) {
			LOG.trace("SUCCESS readUntilFull {} of {} ", bytesRead, ba.length);
		}
	}

	/**
	 * the constructor.
	 */
	private InputStreamUtil() {

	}
}
