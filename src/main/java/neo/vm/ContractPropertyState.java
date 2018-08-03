package neo.vm;

import neo.model.ByteArraySerializable;
import neo.model.ByteSizeable;

/**
 * the contract property state.
 *
 * @author coranos
 *
 */
public enum ContractPropertyState implements ByteSizeable, ByteArraySerializable {

	/** NoProperty. */
	NoProperty(0),
	/** HasStorage. */
	HasStorage(1 << 0),
	/** HasDynamicInvoke. */
	HasDynamicInvoke(1 << 1),
	/** ending semicolon */
	;

	/**
	 * return the ContractPropertyState, or throw an exception if there is no match.
	 *
	 * @param typeByte
	 *            type type byte.
	 * @return the ContractPropertyState, or throw an exception if there is no
	 *         match.
	 */
	public static ContractPropertyState valueOfByte(final byte typeByte) {
		for (final ContractPropertyState it : ContractPropertyState.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	/**
	 * the byte representing the ContractPropertyState.
	 */
	private final byte typeByte;

	/**
	 * the constructor.
	 *
	 * @param typeInt
	 *            the ContractPropertyState as an integer.
	 */
	ContractPropertyState(final int typeInt) {
		typeByte = (byte) (typeInt & 0xff);
	}

	@Override
	public int getByteSize() {
		// TODO Auto-generated method stub
		return 1;
	}

	/**
	 * return the type byte.
	 *
	 * @return the type byte.
	 */
	public byte getTypeByte() {
		return typeByte;
	}

	public boolean HasFlag(final ContractPropertyState state) {
		// TODO: this probably isn't right.
		return equals(state);
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { typeByte };
	}
}

// using System;
//
// namespace Neo.Core
// {
// [Flags]
// public enum ContractPropertyState : byte
// {
// NoProperty = 0,
//
// HasStorage = 1 << 0,
// HasDynamicInvoke = 1 << 1,
// }
// }
