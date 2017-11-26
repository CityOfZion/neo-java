package neo.model.core;

public enum TransactionAttributeUsage {
	/** */
	CONTRACT_HASH(0x00),

	/** */
	ECDH02(0x02),
	/** */
	ECDH03(0x03),
	/** */
	SCRIPT(0x20),
	/** */
	VOTE(0x30),
	/** */
	DESCRIPTION_URL(0x81),
	/** */
	DESCRIPTION(0x90),
	/** */
	HASH_01(0xa1),
	/** */
	HASH_02(0xa2),
	/** */
	HASH_03(0xa3),
	/** */
	HASH_04(0xa4),
	/** */
	HASH_05(0xa5),
	/** */
	HASH_06(0xa6),
	/** */
	HASH_07(0xa7),
	/** */
	HASH_08(0xa8),
	/** */
	HASH_09(0xa9),
	/** */
	HASH_10(0xaa),
	/** */
	HASH_11(0xab),
	/** */
	HASH_12(0xac),
	/** */
	HASH_13(0xad),
	/** */
	HASH_14(0xae),
	/** */
	HASH_15(0xaf),
	/** */
	REMARK_00(0xf0),
	/** */
	REMARK_01(0xf1),
	/** */
	REMARK_02(0xf2),
	/** */
	REMARK_03(0xf3),
	/** */
	REMARK_04(0xf4),
	/** */
	REMARK_05(0xf5),
	/** */
	REMARK_06(0xf6),
	/** */
	REMARK_07(0xf7),
	/** */
	REMARK_08(0xf8),
	/** */
	REMARK_09(0xf9),
	/** */
	REMARK_10(0xfa),
	/** */
	REMARK_11(0xfb),
	/** */
	REMARK_12(0xfc),
	/** */
	REMARK_13(0xfd),
	/** */
	REMARK_14(0xfe),
	/** */
	REMARK_15(0xff),
	/** ending semicolon */
	;

	public static TransactionAttributeUsage valueOf(final byte typeByte) {
		for (final TransactionAttributeUsage it : TransactionAttributeUsage.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	private final byte typeByte;

	private TransactionAttributeUsage(final int typeInt) {
		typeByte = (byte) (typeInt & 0xff);
	}

	public byte getTypeByte() {
		return typeByte;
	}
}