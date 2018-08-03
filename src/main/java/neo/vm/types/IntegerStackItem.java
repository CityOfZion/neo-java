package neo.vm.types;

import java.math.BigInteger;

import neo.vm.AbstractStackItem;

/**
 * integer stack item.
 *
 * @author coranos
 *
 */
public final class IntegerStackItem extends AbstractStackItem {

	/**
	 * the value.
	 */
	private final BigInteger value;

	/**
	 * the constructor.
	 *
	 * @param value
	 *            the value.
	 */
	public IntegerStackItem(final BigInteger value) {
		this.value = value;
	}

	@Override
	public int compareTo(final AbstractStackItem o) {
		final IntegerStackItem that = (IntegerStackItem) o;
		return value.compareTo(that.value);
	}

	@Override
	public BigInteger getBigInteger() {
		return value;
	}

	@Override
	public boolean getBoolean() {
		return value != BigInteger.ZERO;
	}

	@Override
	public byte[] getByteArray() {
		return value.toByteArray();
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
// internal class Integer : StackItem
// {
// private BigInteger value;
//
// public Integer(BigInteger value)
// {
// this.value = value;
// }
//
// public override bool Equals(StackItem other)
// {
// if (ReferenceEquals(this, other)) return true;
// if (ReferenceEquals(null, other)) return false;
// Integer i = other as Integer;
// if (i == null)
// return GetByteArray().SequenceEqual(other.GetByteArray());
// else
// return value == i.value;
// }
//
// public override BigInteger GetBigInteger()
// {
// return value;
// }
//
// public override bool GetBoolean()
// {
// return value != BigInteger.Zero;
// }
//
// public override byte[] GetByteArray()
// {
// return value.ToByteArray();
// }
// }
// }
