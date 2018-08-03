package neo.model.util;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;

/**
 * utilities having to do with the RIPEMD160 hash.
 *
 * @author coranos
 *
 */
public final class RIPEMD160HashUtil {

	/**
	 * returns the RIPEMD160 hash of the bytes.
	 *
	 * @param bytes
	 *            the bytes to hash.
	 * @return the hash.
	 */
	public static byte[] getRIPEMD160Hash(final byte[] bytes) {
		final RIPEMD160Digest d = new RIPEMD160Digest();
		d.update(bytes, 0, bytes.length);

		final byte[] hash = new byte[d.getDigestSize()];
		d.doFinal(hash, 0);
		return hash;
	}

	/**
	 * the constructor.
	 */
	private RIPEMD160HashUtil() {

	}
}
