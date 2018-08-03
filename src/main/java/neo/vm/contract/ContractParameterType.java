package neo.vm.contract;

import org.apache.kerby.util.Hex;
import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ByteSizeable;
import neo.model.ToJsonObject;

public enum ContractParameterType implements ByteSizeable, ByteArraySerializable, ToJsonObject {
	/** Signature. */
	Signature(0x00),
	/** Boolean */
	Boolean(0x01),
	/** Integer */
	Integer(0x02),
	/** Hash160 */
	Hash160(0x03),
	/** Hash256 */
	Hash256(0x04),
	/** ByteArray */
	ByteArray(0x05),
	/** PublicKey */
	PublicKey(0x06),
	/** String */
	String(0x07),
	/** Array */
	Array(0x10),
	/** InteropInterface */
	InteropInterface(0xf0),
	/** Void */
	Void(0xff),
	/** ending semicolon */
	;

	/**
	 * the size of the enum, serialized.
	 */
	public static final int SIZE = 1;

	/**
	 * return the ContractParameterType, or throw an exception if there is no match.
	 *
	 * @param typeByte
	 *            type type byte.
	 * @return the ContractParameterType, or throw an exception if there is no
	 *         match.
	 */
	public static ContractParameterType valueOfByte(final byte typeByte) {
		for (final ContractParameterType it : ContractParameterType.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	/**
	 * the byte representing the ContractParameterType.
	 */
	private final byte typeByte;

	/**
	 * the constructor.
	 *
	 * @param typeInt
	 *            the ContractParameterType as an integer.
	 */
	ContractParameterType(final int typeInt) {
		typeByte = (byte) (typeInt & 0xff);
	}

	@Override
	public int getByteSize() {
		return SIZE;
	}

	/**
	 * return the type byte.
	 *
	 * @return the type byte.
	 */
	public byte getTypeByte() {
		return typeByte;
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { typeByte };
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject object = new JSONObject();
		object.put("name", name());
		object.put("typeByte", Hex.encode(toByteArray()));
		return object;
	}
}
// namespace Neo.SmartContract
// {
// /// <summary>
// /// 表示智能合约的参数类型
// /// </summary>
// public enum ContractParameterType : byte
// {
// /// <summary>
// /// 签名
// /// </summary>
// Signature = 0x00,
// Boolean = 0x01,
// /// <summary>
// /// 整数
// /// </summary>
// Integer = 0x02,
// /// <summary>
// /// 160位散列值
// /// </summary>
// Hash160 = 0x03,
// /// <summary>
// /// 256位散列值
// /// </summary>
// Hash256 = 0x04,
// /// <summary>
// /// 字节数组
// /// </summary>
// ByteArray = 0x05,
// PublicKey = 0x06,
// String = 0x07,
//
// Array = 0x10,
//
// InteropInterface = 0xf0,
//
// Void = 0xff
// }
// }
