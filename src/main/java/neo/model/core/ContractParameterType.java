package neo.model.core;

public enum ContractParameterType {
	/** */
	Signature(0x00),
	/** */
	Boolean(0x01),
	/** */
	Integer(0x02),
	/** */
	Hash160(0x03),
	/** */
	Hash256(0x04),
	/** */
	ByteArray(0x05),
	/** */
	PublicKey(0x06),
	/** */
	String(0x07),
	/** */
	Array(0x10),
	/** */
	InteropInterface(0xf0),
	/** */
	Void(0xff),
	/** ending semicolon */
	;

	public static ContractParameterType valueOf(final byte typeByte) {
		for (final ContractParameterType it : ContractParameterType.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	public static ContractParameterType[] valuesOf(final byte[] byteArray) {
		final ContractParameterType[] typeArray = new ContractParameterType[byteArray.length];
		for (int ix = 0; ix < typeArray.length; ix++) {
			typeArray[ix] = ContractParameterType.valueOf(byteArray[ix]);
		}
		return typeArray;
	}

	private final byte typeByte;

	private ContractParameterType(final int typeInt) {
		typeByte = (byte) (typeInt & 0xff);
	}

	public byte getTypeByte() {
		return typeByte;
	}

}
