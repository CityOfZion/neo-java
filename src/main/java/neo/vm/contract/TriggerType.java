package neo.vm.contract;

public enum TriggerType {
	/** Verification. */
	Verification(0x00),
	/** Application. */
	Application(0x10),
	/** ending semicolon */
	;

	/**
	 * return the TriggerType, or throw an exception if there is no match.
	 *
	 * @param typeByte
	 *            type type byte.
	 * @return the TriggerType, or throw an exception if there is no match.
	 */
	public static TriggerType valueOfByte(final byte typeByte) {
		for (final TriggerType it : TriggerType.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	/**
	 * the byte representing the TriggerType.
	 */
	private final byte typeByte;

	/**
	 * the constructor.
	 *
	 * @param typeInt
	 *            the TriggerType as an integer.
	 */
	TriggerType(final int typeInt) {
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

// namespace Neo.SmartContract
// {
// public enum TriggerType : byte
// {
// Verification = 0x00,
// Application = 0x10
// }
// }
