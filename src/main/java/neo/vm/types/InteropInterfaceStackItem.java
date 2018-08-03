package neo.vm.types;

import org.apache.commons.lang3.NotImplementedException;

import neo.vm.AbstractStackItem;
import neo.vm.IInteropInterface;

/**
 * the interop interface stack item.
 *
 * @author coranos
 *
 */
public final class InteropInterfaceStackItem extends AbstractStackItem {

	/**
	 * the interop interface.
	 */
	private final IInteropInterface object;

	/**
	 * the cosntructor.
	 *
	 * @param value
	 *            the value.
	 */
	public InteropInterfaceStackItem(final IInteropInterface value) {
		object = value;
	}

	@Override
	public int compareTo(final AbstractStackItem o) {
		if (o instanceof InteropInterfaceStackItem) {
			final InteropInterfaceStackItem that = (InteropInterfaceStackItem) o;
			return object.compareTo(that.object);
		}
		throw new RuntimeException("object is not a InteropInterface:" + o);
	}

	@Override
	public boolean getBoolean() {
		return object != null;
	}

	@Override
	public byte[] getByteArray() {
		throw new NotImplementedException("GetByteArray");
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IInteropInterface> T getInterface(final Class<T> cl) {
		return (T) object;
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

// using System;
//
// namespace Neo.VM.Types
// {
// internal class InteropInterface : StackItem
// {
// private IInteropInterface _object;
//
// public InteropInterface(IInteropInterface value)
// {
// this._object = value;
// }
//
// public override bool Equals(StackItem other)
// {
// if (ReferenceEquals(this, other)) return true;
// if (ReferenceEquals(null, other)) return false;
// InteropInterface i = other as InteropInterface;
// if (i == null) return false;
// return _object.Equals(i._object);
// }
//
// public override bool GetBoolean()
// {
// return _object != null;
// }
//
// public override byte[] GetByteArray()
// {
// throw new NotSupportedException();
// }
//
// public override T GetInterface<T>()
// {
// return _object as T;
// }
// }
// }
