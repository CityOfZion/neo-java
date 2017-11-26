package neo.model.bytes;

/**
 * an unsigned 16 bit byte array.
 *
 * @author coranos
 *
 */
public final class UInt16 extends AbstractByteArray {

	private static final long serialVersionUID = 1L;

	/**
	 * the size, 2 bytes.
	 */
	public static final int SIZE = 2;

	/**
	 * the constructor.
	 *
	 * @param bytes
	 *            the bytes to use.
	 */
	public UInt16(final byte[] bytes) {
		super(bytes);
	}

	@Override
	public int getByteSize() {
		return SIZE;
	}

}