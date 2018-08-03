package neo.vm;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import neo.vm.types.BooleanStackItem;
import neo.vm.types.ByteArrayStackItem;
import neo.vm.types.IntegerStackItem;
import neo.vm.types.InteropInterfaceStackItem;

/**
 * the stack item, for the execution engine stack.
 *
 * @author coranos
 *
 */
public abstract class AbstractStackItem implements Comparable<AbstractStackItem> {

	/**
	 * creates a stack item form an IInteropInterface.
	 *
	 * @param value
	 *            the IInteropInterface to use.
	 * @return the new stack item.
	 */
	public static AbstractStackItem fromInterface(final IInteropInterface value) {
		return new InteropInterfaceStackItem(value);
	}

	/**
	 * return a stack item for that value.
	 *
	 * @param value
	 *            the value to use.
	 * @return a stack item for that value.
	 */
	public static AbstractStackItem valueOf(final BigInteger value) {
		return new IntegerStackItem(value);
	}

	/**
	 * return a stack item for that value.
	 *
	 * @param value
	 *            the value to use.
	 * @return a stack item for that value.
	 */
	public static AbstractStackItem valueOf(final boolean value) {
		return new BooleanStackItem(value);
	}

	/**
	 * return a stack item for that value.
	 *
	 * @param value
	 *            the value to use.
	 * @return a stack item for that value.
	 */
	public static AbstractStackItem valueOf(final byte[] value) {
		return new ByteArrayStackItem(value);
	}

	/**
	 * return a stack item for that value.
	 *
	 * @param value
	 *            the value to use.
	 * @return a stack item for that value.
	 */
	public static AbstractStackItem valueOf(final long value) {
		return new IntegerStackItem(BigInteger.valueOf(value));
	}

	/**
	 * return the stack item as an array.
	 *
	 * @return the array.
	 */
	public List<AbstractStackItem> getArray() {
		throw new NotImplementedException("GetArray");
	}

	/**
	 * return the stack item as a BigInteger.
	 *
	 * @return the BigInteger.
	 */
	public BigInteger getBigInteger() {
		return new BigInteger(getByteArray());
	}

	/**
	 * return the stack item as a boolean.
	 *
	 * @return the boolean.
	 */
	public boolean getBoolean() {
		for (final byte b : getByteArray()) {
			if (b != 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * return the stack item as a byte[].
	 *
	 * @return the byte[].
	 */
	public abstract byte[] getByteArray();

	/**
	 * return the stack item as a IInteropInterface.
	 *
	 * @param cl
	 *            the class.
	 * @param <T>
	 *            the type.
	 * @return the IInteropInterface.
	 */
	public <T extends IInteropInterface> T getInterface(final Class<T> cl) {
		throw new NotImplementedException("GetInterface");
	}

	/**
	 * return the stack item as a String.
	 *
	 * @return the String.
	 */
	public String getString() {
		return new String(getByteArray(), Charset.forName("UTF8"));
	}

	/**
	 * return true if an array.
	 *
	 * @return true if an array.
	 */
	public abstract boolean isArray();

	/**
	 * return true if a struct.
	 *
	 * @return true if a struct.
	 */
	public abstract boolean isStruct();

	// TODO: figure out what these do.
	// public static StackItem valueOf(uint value) {
	// return (BigInteger) value;
	// }
	//
	// public static StackItem valueOf(long value) {
	// return (BigInteger) value;
	// }
	//
	// public static StackItem valueOf(Fixed8 value) {
	// return (BigInteger) value;
	// }
	//
	// public static StackItem valueOf(StackItem[] value) {
	// return new ArrayList(value);
	// }
	//
	// public static StackItem valueOf(List<StackItem> value) {
	// return new ArrayList(value);
	// }
}
//
// namespace Neo.VM
// {
// public abstract class StackItem : IEquatable<StackItem>
// {
// public virtual bool IsArray => false;
// public virtual bool IsStruct => false;
//
// public abstract bool Equals(StackItem other);
//
// public static StackItem FromInterface(IInteropInterface value)
// {
// return new InteropInterface(value);
// }
//
// public virtual IList<StackItem> GetArray()
// {
// throw new NotSupportedException();
// }
//
// public virtual BigInteger GetBigInteger()
// {
// return new BigInteger(GetByteArray());
// }
//
// public virtual bool GetBoolean()
// {
// return GetByteArray().Any(p => p != 0);
// }
//
// public abstract byte[] GetByteArray();
//
// public virtual T GetInterface<T>() where T : class, IInteropInterface
// {
// throw new NotSupportedException();
// }
//
// public virtual string GetString()
// {
// return Encoding.UTF8.GetString(GetByteArray());
// }
//
// public static implicit operator StackItem(int value)
// {
// return (BigInteger)value;
// }
//
// public static implicit operator StackItem(uint value)
// {
// return (BigInteger)value;
// }
//
// public static implicit operator StackItem(long value)
// {
// return (BigInteger)value;
// }
//
// public static implicit operator StackItem(ulong value)
// {
// return (BigInteger)value;
// }
//
// public static implicit operator StackItem(BigInteger value)
// {
// return new Integer(value);
// }
//
// public static implicit operator StackItem(bool value)
// {
// return new Boolean(value);
// }
//
// public static implicit operator StackItem(byte[] value)
// {
// return new ByteArray(value);
// }
//
// public static implicit operator StackItem(StackItem[] value)
// {
// return new Array(value);
// }
//
// public static implicit operator StackItem(List<StackItem> value)
// {
// return new Array(value);
// }
// }
// }
