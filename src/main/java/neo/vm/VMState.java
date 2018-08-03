package neo.vm;

/**
 * the VM State.
 *
 * @author coranos
 *
 */
public enum VMState {

	/** none. */
	NONE(0),
	/** halt. */
	HALT(1 << 0),
	/** fault. */
	FAULT(1 << 1),
	/** break. */
	BREAK(1 << 2),
	/** ending semicolon */
	;

	/**
	 * return the VMState, or throw an exception if there is no match.
	 *
	 * @param typeByte
	 *            type type byte.
	 * @return the VMState, or throw an exception if there is no match.
	 */
	public static VMState valueOfByte(final byte typeByte) {
		for (final VMState it : VMState.values()) {
			if (it.typeByte == typeByte) {
				return it;
			}
		}
		throw new RuntimeException("unknown typeByte:" + typeByte);
	}

	/**
	 * the byte representing the VMState.
	 */
	private final byte typeByte;

	/**
	 * the constructor.
	 *
	 * @param typeInt
	 *            the VMState as an integer.
	 */
	VMState(final int typeInt) {
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

	public boolean HasFlag(final VMState state) {
		// TODO: this probably isn't right.
		return equals(state);
	}
}

// using System;
//
// namespace Neo.VM
// {
// [Flags]
// public enum VMState : byte
// {
// NONE = 0,
//
// HALT = 1 << 0,
// FAULT = 1 << 1,
// BREAK = 1 << 2,
// }
// }
