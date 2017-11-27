package neo.model.bytes;

/**
 * an unsigned 32 bit byte array.
 *
 * @author coranos
 *
 */
public final class UInt32 extends AbstractByteArray {

	private static final long serialVersionUID = 1L;

	/**
	 * the size, 4 bytes.
	 */
	public static final int SIZE = 4;

	/**
	 * the constructor.
	 *
	 * @param bytes
	 *            the bytes to use.
	 */
	public UInt32(final byte[] bytes) {
		super(bytes);
	}

	/**
	 * return the value as a long.
	 *
	 * @return the value as a long.
	 */
	public long asLong() {
		return toPositiveBigInteger().longValue();
	}

	@Override
	public int getByteSize() {
		return SIZE;
	}

}