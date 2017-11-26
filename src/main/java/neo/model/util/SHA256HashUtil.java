package neo.model.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256HashUtil {

	public static byte[] getDoubleSHA256Hash(final byte[] bytes) {
		return getSHA256Hash(getSHA256Hash(bytes));
	}

	public static byte[] getSHA256Hash(final byte[] bytes) {
		final String digestName = "SHA-256";
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(digestName);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException("exception getting MessageDigest \"" + digestName + "\"", e);
		}
		final byte[] hash = digest.digest(bytes);
		return hash;
	}
}
