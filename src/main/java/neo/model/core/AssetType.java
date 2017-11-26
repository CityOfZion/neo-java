package neo.model.core;

/**
 * the type of assets, used in the Register transaction.
 *
 * @author coranos
 *
 */
public enum AssetType {
	/** credit flag. */
	CreditFlag(0x40),
	/** duty flag. */
	DutyFlag(0x80),
	/** governing token. */
	GoverningToken(0x00),
	/** utility token. */
	UtilityToken(0x01),
	/** currency. */
	Currency(0x08),
	/** share. */
	Share(0x80 | 0x10),
	/** invoice. */
	Invoice(0x80 | 0x18),
	/** token. */
	Token(0x40 | 0x20),
	/** ending semicolon */
	;

	/**
	 * @param typeByte
	 *            type type byte.
	 * @return the asset type, or throw an exception if there is no match.
	 */
	public static AssetType valueOf(final byte typeByte) {
		for (final AssetType it : AssetType.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	/**
	 * the byte representing the asset type.
	 */
	private final byte typeByte;

	/**
	 * the constructor.
	 *
	 * @param typeInt
	 *            the asset type as an integer.
	 */
	AssetType(final int typeInt) {
		typeByte = (byte) (typeInt & 0xff);
	}

	/**
	 * @return the type byte.
	 */
	public byte getTypeByte() {
		return typeByte;
	}
}
