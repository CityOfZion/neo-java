package neo.model.core;

/**
 * the state type.
 *
 * @author coranos
 *
 */
public enum StateType {
	/** Miner Transaction. */
	ACCOUNT(0x40),
	/** Issue Transaction. */
	VALIDATOR(0x48),
	/** ending semicolon */
	;

	/**
	 * returns the transaction type that matches the given typeByte, or throw an
	 * error if the typeByte matches no transactions.
	 *
	 * @param typeByte
	 *            the type byte.
	 *
	 * @return the transaction type.
	 */
	public static StateType valueOfByte(final byte typeByte) {
		for (final StateType it : StateType.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	/**
	 * the type byte.
	 */
	private final byte typeByte;

	/**
	 * the constructor.
	 *
	 * @param typeInt
	 *            the typeByte as an int.
	 */
	StateType(final int typeInt) {
		typeByte = (byte) (typeInt & 0xff);
	}

	/**
	 * return the type byte.
	 *
	 * @return the type byte.
	 */
	public byte getTypeByte() {
		return typeByte;
	}
}
