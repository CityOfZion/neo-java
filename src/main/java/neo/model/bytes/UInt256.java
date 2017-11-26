package neo.model.bytes;

import java.nio.ByteBuffer;

/**
 * an unsigned 256 bit byte array.
 *
 * @author coranos
 *
 */
public final class UInt256 extends AbstractByteArray {

	private static final long serialVersionUID = 1L;

	/**
	 * the size, 32 bytes.
	 */
	public static final int SIZE = 32;

	/**
	 * the constructor.
	 *
	 * @param bytes
	 *            the bytes to use.
	 */
	public UInt256(final byte[] bytes) {
		super(bytes);
	}

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to use.
	 */
	public UInt256(final ByteBuffer bb) {
		super(bb);
	}

	@Override
	public int getByteSize() {
		return SIZE;
	}

}
