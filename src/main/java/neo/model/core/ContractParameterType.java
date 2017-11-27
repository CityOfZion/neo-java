package neo.model.core;

/**
 * the parameter types of a published contract.
 *
 * @author coranos
 *
 */
public enum ContractParameterType {
	/** Signature. */
	Signature(0x00),
	/** Boolean. */
	Boolean(0x01),
	/** Integer. */
	Integer(0x02),
	/** Hash160. */
	Hash160(0x03),
	/** Hash256. */
	Hash256(0x04),
	/** Byte Array. */
	ByteArray(0x05),
	/** Public Key. */
	PublicKey(0x06),
	/** String. */
	String(0x07),
	/** Array. */
	Array(0x10),
	/** Interop Interface. */
	InteropInterface(0xf0),
	/** Void. */
	Void(0xff),
	/** ending semicolon */
	;

	/**
	 * return the ContractParameterType with the given typeByte, or throws an error
	 * if the typeBytes has no ContractParameterType.
	 *
	 * @param typeByte
	 *            the type byte to use.
	 * @return the ContractParameterType with the given typeByte.
	 */
	public static ContractParameterType valueOf(final byte typeByte) {
		for (final ContractParameterType it : ContractParameterType.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	/**
	 * return an array of ContractParameterTypes corresponding to the typeBytes, or
	 * throws an error if one of the typeBytes has no ContractParameterType.
	 *
	 * @param byteArray
	 *            the array of type bytes to use.
	 * @return an array of ContractParameterTypes corresponding to the typeBytes.
	 */
	public static ContractParameterType[] valuesOf(final byte[] byteArray) {
		final ContractParameterType[] typeArray = new ContractParameterType[byteArray.length];
		for (int ix = 0; ix < typeArray.length; ix++) {
			typeArray[ix] = ContractParameterType.valueOf(byteArray[ix]);
		}
		return typeArray;
	}

	/**
	 * the type byte.
	 */
	private final byte typeByte;

	/**
	 * the constructor.
	 *
	 * @param typeInt
	 *            the type byte as an int.
	 */
	ContractParameterType(final int typeInt) {
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
