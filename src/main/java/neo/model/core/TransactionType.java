package neo.model.core;

public enum TransactionType {
	/** Miner Transaction */
	MINER_TRANSACTION(0x00),
	/** Issue Transaction */
	ISSUE_TRANSACTION(0x01),
	/** Claim Transaction */
	CLAIM_TRANSACTION(0x02),
	/** Enrollment Transaction */
	ENROLLMENT_TRANSACTION(0x20),
	/** Register Transaction */
	REGISTER_TRANSACTION(0x40),
	/** Contract Transaction */
	CONTRACT_TRANSACTION(0x80),
	/** Publish Transaction */
	PUBLISH_TRANSACTION(0xd0),
	/** Invocation Transaction */
	INVOCATION_TRANSACTION(0xd1),
	/** ending semicolon */
	;

	public static TransactionType valueOf(final byte typeByte) {
		for (final TransactionType it : TransactionType.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	private final byte typeByte;

	private TransactionType(final int typeInt) {
		typeByte = (byte) (typeInt & 0xff);
	}

	public byte getTypeByte() {
		return typeByte;
	}
}
