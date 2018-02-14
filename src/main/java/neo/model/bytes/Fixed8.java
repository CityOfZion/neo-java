package neo.model.bytes;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.NumberFormat;

import neo.model.ByteArraySerializable;
import neo.model.util.ModelUtil;

/**
 * an unsigned 64 bit long. The long value is cached, rather than calculated
 * every time.
 *
 * @author coranos
 *
 */
public final class Fixed8 implements ByteArraySerializable {

	/**
	 * the size, 8 bytes.
	 */
	public static final int SIZE = UInt64.SIZE;

	/**
	 * the value.
	 */
	public final long value;

	/**
	 * the value as a UInt64.
	 */
	private final UInt64 valueObj;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the byte buffer to read.
	 */
	public Fixed8(final ByteBuffer bb) {
		valueObj = ModelUtil.getUInt64(bb);
		value = valueObj.toPositiveBigInteger().longValue();
	}

	@Override
	public byte[] toByteArray() {
		final byte[] ba = valueObj.toByteArray();
		return ba;
	}

	/**
	 * returns a positive BigInteger.
	 *
	 * @return this array as a BigInteger, assuming the bytes represent a signed
	 *         int.
	 */
	public BigInteger toPositiveBigInteger() {
		return valueObj.toPositiveBigInteger();
	}

	@Override
	public String toString() {
		return NumberFormat.getIntegerInstance().format(value);
	}
}
