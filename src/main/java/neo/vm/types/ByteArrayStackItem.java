package neo.vm.types;

import neo.model.util.ModelUtil;
import neo.vm.AbstractStackItem;

/**
 * the byte array stack item.
 *
 * @author coranos
 *
 */
public final class ByteArrayStackItem extends AbstractStackItem {

	/**
	 * the value.
	 */
	private final byte[] value;

	/**
	 * the constructor.
	 *
	 * @param value
	 *            the value to use.
	 */
	public ByteArrayStackItem(final byte[] value) {
		this.value = value;
	}

	@Override
	public int compareTo(final AbstractStackItem o) {
		final ByteArrayStackItem that = (ByteArrayStackItem) o;
		return ModelUtil.compareTo(value, that.value);
	}

	@Override
	public byte[] getByteArray() {
		return value;
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
//
// namespace Neo.VM.Types
// {
// internal class ByteArray : StackItem
// {
// private byte[] value;
//
// public ByteArray(byte[] value)
// {
// this.value = value;
// }
//
// public override bool Equals(StackItem other)
// {
// if (ReferenceEquals(this, other)) return true;
// if (ReferenceEquals(null, other)) return false;
// return value.SequenceEqual(other.GetByteArray());
// }
//
// public override byte[] GetByteArray()
// {
// return value;
// }
// }
// }
