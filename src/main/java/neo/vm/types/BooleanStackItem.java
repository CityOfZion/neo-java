package neo.vm.types;

import java.math.BigInteger;

import neo.model.util.ModelUtil;
import neo.vm.AbstractStackItem;

/**
 * the boolean stack item.
 *
 * @author coranos
 *
 */
public final class BooleanStackItem extends AbstractStackItem {

	/**
	 * true, as an array.
	 */
	private static final byte[] TRUE = { 1 };

	/**
	 * false as an array.
	 */
	private static final byte[] FALSE = { 0 };

	/**
	 * the value.
	 */
	private final boolean value;

	/**
	 * the constructor.
	 *
	 * @param value
	 *            the value.
	 */
	public BooleanStackItem(final boolean value) {
		this.value = value;
	}

	@Override
	public int compareTo(final AbstractStackItem o) {
		final BooleanStackItem that = (BooleanStackItem) o;
		return ModelUtil.compareTo(getByteArray(), that.getByteArray());
	}

	@Override
	public BigInteger getBigInteger() {
		return value ? BigInteger.ONE : BigInteger.ZERO;
	}

	@Override
	public boolean getBoolean() {
		return value;
	}

	@Override
	public byte[] getByteArray() {
		return value ? TRUE : FALSE;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public boolean isStruct() {
		return false;
	}
}

// using System.Linq;
// using System.Numerics;
//
// namespace Neo.VM.Types
// {
// internal class Boolean : StackItem
// {
// private static readonly byte[] TRUE = { 1 };
// private static readonly byte[] FALSE = new byte[0];
//
// private bool value;
//
// public Boolean(bool value)
// {
// this.value = value;
// }
//
// public override bool Equals(StackItem other)
// {
// if (ReferenceEquals(this, other)) return true;
// if (ReferenceEquals(null, other)) return false;
// Boolean b = other as Boolean;
// if (b == null)
// return GetByteArray().SequenceEqual(other.GetByteArray());
// else
// return value == b.value;
// }
//
// public override BigInteger GetBigInteger()
// {
// return value ? BigInteger.One : BigInteger.Zero;
// }
//
// public override bool GetBoolean()
// {
// return value;
// }
//
// public override byte[] GetByteArray()
// {
// return value ? TRUE : FALSE;
// }
// }
// }
