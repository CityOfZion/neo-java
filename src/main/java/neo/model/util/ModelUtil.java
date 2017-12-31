package neo.model.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt128;
import neo.model.bytes.UInt16;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.bytes.UInt32;
import neo.model.bytes.UInt64;

/**
 * the utilities for editing the neo model.
 *
 * @author coranos
 *
 */
public final class ModelUtil {

	/**
	 * gas.
	 */
	public static final String GAS = "gas";

	/**
	 * neo.
	 */
	public static final String NEO = "neo";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ModelUtil.class);

	/**
	 * the encoded byte to mean a variable length is a long.
	 */
	private static final byte LENGTH_LONG = (byte) 0xFF;

	/**
	 * the encoded byte to mean a variable length is a int.
	 */
	private static final byte LENGTH_INT = (byte) 0xFE;

	/**
	 * the encoded byte to mean a variable length is a short.
	 */
	private static final byte LENGTH_SHORT = (byte) 0xFD;

	/**
	 * the NEO coin hash.
	 */
	public static final String NEO_HASH_HEX_STR = "c56f33fc6ecfcd0c225c4ab356fee59390af8560be0e930faebe74a6daff7c9b";

	/**
	 * the GAS coin hash.
	 */
	public static final String GAS_HASH_HEX_STR = "602c79718b16e442de58778e148d0b1084e3b2dffd5de6b7b16cee7969282de7";

	/**
	 * the hash of the NEO registration transaction.
	 */
	public static final UInt256 NEO_HASH;

	/**
	 * the hash of the GAS registration transaction.
	 */
	public static final UInt256 GAS_HASH;

	/**
	 * the divisor to use to convert a Fixed8 value to a decimal.
	 */
	public static final long DECIMAL_DIVISOR = 100000000;

	static {

		try {
			final byte[] neoBa = Hex.decodeHex(NEO_HASH_HEX_STR.toCharArray());
			ArrayUtils.reverse(neoBa);
			NEO_HASH = new UInt256(neoBa);

			final byte[] gasBa = Hex.decodeHex(GAS_HASH_HEX_STR.toCharArray());
			ArrayUtils.reverse(gasBa);
			GAS_HASH = new UInt256(gasBa);
		} catch (final DecoderException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * the address version.
	 */
	public static final byte ADDRESS_VERSION = 23;

	public static byte[] copyAndReverse(final byte[] input) {
		final byte[] revInput = new byte[input.length];
		System.arraycopy(input, 0, revInput, 0, input.length);
		ArrayUtils.reverse(revInput);
		return revInput;
	}

	public static byte[] decodeHex(final String string) {
		try {
			return Hex.decodeHex(string.toCharArray());
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static BigInteger getBigInteger(final byte[] ba) {
		return getBigInteger(ByteBuffer.wrap(ba));
	}

	public static BigInteger getBigInteger(final ByteBuffer bb) {
		final byte lengthType = bb.get();

		int length;
		if (lengthType == LENGTH_SHORT) {
			length = 2;
		} else if (lengthType == LENGTH_INT) {
			length = 4;
		} else if (lengthType == LENGTH_LONG) {
			length = 8;
		} else {
			length = -1;
		}

		if (length == -1) {
			final BigInteger retval = new BigInteger(1, new byte[] { lengthType });
			return retval;
		}

		final byte[] ba = new byte[length];
		bb.get(ba);

		ArrayUtils.reverse(ba);
		final BigInteger retval = new BigInteger(1, ba);

		return retval;
	}

	public static boolean getBoolean(final ByteBuffer bb) {
		return bb.get() != 0;
	}

	public static byte getByte(final ByteBuffer bb) {
		return bb.get();
	}

	public static byte[] getByteArray(final ByteBuffer bb) {
		final BigInteger length = getBigInteger(bb);
		final byte[] ba = new byte[length.intValue()];
		bb.get(ba);
		return ba;
	}

	public static byte[] getByteArray(final ByteBuffer bb, final int size, final boolean reverse) {
		final byte[] ba = new byte[size];
		bb.get(ba);
		if (reverse) {
			ArrayUtils.reverse(ba);
		}
		return ba;
	}

	public static Fixed8 getFixed8(final ByteBuffer bb) {
		return new Fixed8(bb);
	}

	public static String getString(final ByteBuffer bb) {
		final byte[] ba = getByteArray(bb);
		try {
			return new String(ba, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getString(final ByteBuffer bb, final int length) {
		final byte[] ba = getByteArray(bb, length, false);
		try {
			return new String(ba, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static UInt128 getUInt128(final ByteBuffer bb) {
		final byte[] ba = getByteArray(bb, UInt128.SIZE, true);
		return new UInt128(ba);
	}

	public static UInt16 getUInt16(final ByteBuffer bb) {
		final byte[] ba = getByteArray(bb, UInt16.SIZE, true);
		return new UInt16(ba);
	}

	public static UInt160 getUInt160(final ByteBuffer bb, final boolean reverse) {
		final byte[] ba = getByteArray(bb, UInt160.SIZE, true);
		if (reverse) {
			ArrayUtils.reverse(ba);
		}
		return new UInt160(ba);
	}

	/**
	 * return a UInt256.
	 *
	 * @param bb
	 *            the byte buffer to use.
	 * @return a UInt256
	 */
	public static UInt256 getUInt256(final ByteBuffer bb) {
		return getUInt256(bb, false);
	}

	public static UInt256 getUInt256(final ByteBuffer bb, final boolean reverse) {
		final byte[] ba = getByteArray(bb, UInt256.SIZE, true);
		if (reverse) {
			ArrayUtils.reverse(ba);
		}
		return new UInt256(ba);
	}

	public static UInt32 getUInt32(final ByteBuffer bb) {
		final byte[] ba = getByteArray(bb, UInt32.SIZE, true);
		return new UInt32(ba);
	}

	public static UInt64 getUInt64(final ByteBuffer bb) {
		final byte[] ba = getByteArray(bb, UInt64.SIZE, true);
		return new UInt64(ba);
	}

	public static <T extends ByteArraySerializable> List<T> readArray(final ByteBuffer bb, final Class<T> cl) {
		final BigInteger lengthBi = getBigInteger(bb);
		final int length = lengthBi.intValue();

		LOG.trace("readArray length {} class {}", length, cl.getSimpleName());

		final List<T> list = new ArrayList<>();
		for (int ix = 0; ix < length; ix++) {

			LOG.trace("STARTED readArray class {} [{}]", cl.getSimpleName(), ix);
			final T t;
			try {
				final Constructor<T> con = cl.getConstructor(ByteBuffer.class);
				t = con.newInstance(bb);
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(
						"error reading record " + (ix + 1) + " of " + length + " class " + cl.getSimpleName(), e);
			}

			LOG.trace("SUCCESS readArray class {} [{}]: {} {}", cl.getSimpleName(), ix,
					Hex.encodeHexString(t.toByteArray()), t);

			list.add(t);
		}
		return list;
	}

	public static String toAddress(final UInt160 scriptHash) {
		final byte[] data = new byte[21];

		if (LOG.isTraceEnabled()) {
			LOG.trace("toAddress ADDRESS_VERSION {}", ModelUtil.toHexString(ADDRESS_VERSION));
		}

		final byte[] scriptHashBa = scriptHash.toByteArray();
		System.arraycopy(scriptHashBa, 0, data, 0, scriptHashBa.length);

		data[data.length - 1] = ADDRESS_VERSION;
		if (LOG.isTraceEnabled()) {
			LOG.info("toAddress data {}", ModelUtil.toHexString(data));
		}

		final byte[] dataAndChecksum = new byte[25];
		System.arraycopy(data, 0, dataAndChecksum, 4, data.length);

		ArrayUtils.reverse(data);
		final byte[] hash = SHA256HashUtil.getDoubleSHA256Hash(data);
		final byte[] hash4 = new byte[4];
		System.arraycopy(hash, 0, hash4, 0, 4);
		ArrayUtils.reverse(hash4);
		System.arraycopy(hash4, 0, dataAndChecksum, 0, 4);
		if (LOG.isTraceEnabled()) {
			LOG.info("toAddress dataAndChecksum {}", ModelUtil.toHexString(dataAndChecksum));
		}

		final String address = toBase58String(dataAndChecksum);
		return address;
	}

	public static String toBase58String(final byte[] bytes) {
		return Base58Util.encode(bytes);
	}

	public static String toBase64String(final byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * returns the list of byte arrays as a encoded byte array.
	 *
	 * @param baList
	 *            the byte array list.
	 * @return the encoded byte array.
	 */
	public static byte[] toByteArray(final byte[]... baList) {
		return toByteArray(Arrays.asList(baList));
	}

	/**
	 * converts a list of byte arrays into a byte array.
	 *
	 * @param baList
	 *            the byte array list to use.
	 * @return the byte array.
	 */
	public static byte[] toByteArray(final List<byte[]> baList) {
		final ByteArrayOutputStream bout;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			NetworkUtil.writeLong(out, baList.size());
			for (final byte[] ba : baList) {
				NetworkUtil.writeByteArray(out, ba);
			}
			bout = out;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return bout.toByteArray();
	}

	/**
	 * converts a byte array into a list of byte arrays.
	 *
	 * @param ba
	 *            the byte array to use.
	 * @return the byte array.
	 */
	public static List<byte[]> toByteArrayList(final byte[] ba) {
		final List<byte[]> baList = new ArrayList<>();
		final ByteBuffer listBb = ByteBuffer.wrap(ba);
		final long size = listBb.getLong();
		for (long ix = 0; ix < size; ix++) {
			final byte[] keyBa = ModelUtil.getByteArray(listBb);
			baList.add(keyBa);
		}
		return baList;
	}

	public static String toHexString(final byte... ba) {
		return new String(Hex.encodeHex(ba));
	}

	public static <T extends ToJsonObject> JSONArray toJSONArray(final List<T> list) {
		final JSONArray jsonArray = new JSONArray();

		for (final T t : list) {
			jsonArray.put(t.toJSONObject());
		}

		return jsonArray;
	}

	public static String toReverseHexString(final byte... bytes) {
		final byte[] ba = new byte[bytes.length];
		System.arraycopy(bytes, 0, ba, 0, bytes.length);
		ArrayUtils.reverse(ba);
		final BigInteger bi = new BigInteger(1, ba);
		return bi.toString(16);
	}

	/**
	 * converts the value to a double, by dividing by DECIMAL_DIVISOR. then formats
	 * it to a string with two decimal places.
	 *
	 * @param value
	 *            the long value to convert.
	 * @return the converted value as a string.
	 */
	public static String toRoundedDoubleAsString(final long value) {
		final double input = value / DECIMAL_DIVISOR;
		return String.format("%.2f", input);
	}

	/**
	 * the constructor.
	 */
	private ModelUtil() {

	}

}
