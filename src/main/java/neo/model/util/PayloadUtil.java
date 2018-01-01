package neo.model.util;

import neo.model.network.VersionPayload;

/**
 * utilities having to do with the payload.
 *
 * @author coranos
 *
 */
public final class PayloadUtil {

	/**
	 * returns a new VersionPayload.
	 *
	 * @param port
	 *            the port to use.
	 * @param nonce
	 *            the nonce to use.
	 * @param startHeight
	 *            the block height to use.
	 * @return the new VersionPayload.
	 */
	public static VersionPayload getVersionPayload(final int port, final int nonce, final long startHeight) {
		return new VersionPayload(System.currentTimeMillis() / 1000, port, nonce, "/NEO:2.3.4/", startHeight);
	}

	/**
	 * the constructor.
	 */
	private PayloadUtil() {

	}

}
