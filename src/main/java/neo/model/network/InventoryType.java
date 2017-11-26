package neo.model.network;

public enum InventoryType {
	/** Transaction */
	TRANSACTION(0x01),
	/** Block */
	BLOCK(0x02),
	/** Consensus */
	CONSENSUS(0xe0);
	/** ending semicolon */
	;

	public static InventoryType valueOf(final byte typeByte) {
		for (final InventoryType it : InventoryType.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	private final byte typeByte;

	private InventoryType(final int typeInt) {
		typeByte = (byte) (typeInt & 0xff);
	}

	public byte getTypeByte() {
		return typeByte;
	}
}
