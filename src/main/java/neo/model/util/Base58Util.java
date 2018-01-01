package neo.model.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility for base58 encoding.
 *
 * @author coranos
 *
 */
public final class Base58Util {

	/**
	 * the alphabet.
	 */
	public static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Base58Util.class);

	/**
	 * a BigInteger representing the number 58.
	 */
	private static final BigInteger BIGINT_58 = BigInteger.valueOf(58);

	/**
	 * decodes a Base58 byte array.
	 *
	 * @param input
	 *            the base58 string to use.
	 * @return the decoded byte array.
	 */
	public static byte[] decode(final String input) {

		BigInteger bi = BigInteger.ZERO;

		for (int ix = 0; ix < input.length(); ix++) {
			final char c = input.charAt(ix);
			final int index = ALPHABET.indexOf(c);
			if (index == -1) {
				throw new RuntimeException("invalid char:" + c);
			}

			bi = bi.multiply(BIGINT_58);
			bi = bi.add(BigInteger.valueOf(index));
		}
		final byte[] bytes = bi.toByteArray();

		final boolean stripSignByte = (bytes.length > 1) && (bytes[0] == 0) && (bytes[1] >= (byte) 0x80);

		ArrayUtils.reverse(bytes);

		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			if (stripSignByte) {
				bout.write(bytes, 0, bytes.length - 1);
			} else {
				bout.write(bytes);
			}
			for (int ix = 0; (ix < input.length()) && (input.charAt(ix) == ALPHABET.charAt(0)); ix++) {
				bout.write(new byte[1]);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		final byte[] ba = bout.toByteArray();
		return ba;
	}

	/**
	 * encodes a Base58 byte array.
	 *
	 * @param input
	 *            the byte array to use.
	 * @return the encoded Base58 string.
	 */
	public static String encode(final byte[] input) {
		try {
			final byte[] revInput = new byte[input.length];
			System.arraycopy(input, 0, revInput, 0, input.length);
			ArrayUtils.reverse(revInput);

			BigInteger value = new BigInteger(1, revInput);
			final StringBuilder sb = new StringBuilder();
			if (LOG.isDebugEnabled()) {
				LOG.debug("[0]val58: {}", ModelUtil.toHexString(BIGINT_58.toByteArray()));
				LOG.debug("[0]value: {}", ModelUtil.toHexString(value.toByteArray()));
			}

			while (value.compareTo(BigInteger.ZERO) != 0) {
				final BigInteger[] valueDivAndRemainder = value.divideAndRemainder(BIGINT_58);
				final BigInteger valueDiv = valueDivAndRemainder[0];
				final BigInteger valueRemainder = valueDivAndRemainder[1];

				sb.append(ALPHABET.charAt(valueRemainder.intValue()));
				value = valueDiv;
				if (LOG.isDebugEnabled()) {
					LOG.debug("[1]value: {}", ModelUtil.toHexString(value.toByteArray()));
					LOG.debug("[1]value.compareTo(val58): {}", value.compareTo(BIGINT_58));
				}
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("[2]value: {}", ModelUtil.toHexString(value.toByteArray()));
			}

			for (int ix = 0; (ix < revInput.length) && (revInput[ix] == 0); ix++) {
				sb.append(ALPHABET.charAt(0));
			}

			sb.reverse();

			return sb.toString();
		} catch (final Exception e) {
			throw new RuntimeException("error encoding \"" + ModelUtil.toHexString(input) + "\"", e);
		}
	}

	/**
	 * the constructor.
	 */
	private Base58Util() {

	}
}
