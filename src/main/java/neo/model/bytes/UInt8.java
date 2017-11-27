package neo.model.bytes;

/**
 * an unsigned 8 bit byte array.
 *
 * @author coranos
 *
 */
public final class UInt8 extends AbstractByteArray {

	private static final long serialVersionUID = 1L;

	/**
	 * the size, 1 byte.
	 */
	public static final int SIZE = 1;

	/**
	 * the constructor.
	 *
	 * @param bytes
	 *            the bytes to use.
	 */
	public UInt8(final byte[] bytes) {
		super(bytes);
	}

	@Override
	public int getByteSize() {
		return SIZE;
	}

}
