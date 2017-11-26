package neo.model.bytes;

import java.nio.ByteBuffer;

import neo.model.keystore.ByteArraySerializable;
import neo.model.keystore.ValueSerializable;
import neo.model.util.ModelUtil;

/**
 * an unsigned 64 bit long. The long value is cached, rather than calculated
 * every time.
 *
 * @author coranos
 *
 */
public final class Fixed8 implements ValueSerializable, ByteArraySerializable {

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
		return valueObj.toByteArray();
	}

	@Override
	public Object toValue() {
		return value;
	}

}
