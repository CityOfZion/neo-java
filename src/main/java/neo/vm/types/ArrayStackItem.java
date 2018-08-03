package neo.vm.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import neo.model.util.ModelUtil;
import neo.vm.AbstractStackItem;

/**
 * the array stack item.
 *
 * @author coranos
 *
 */
public final class ArrayStackItem extends AbstractStackItem {

	/**
	 * the list of items in the array.
	 */
	private final List<AbstractStackItem> array;

	/**
	 * the constructor.
	 *
	 * @param values
	 *            the values to use.
	 */
	public ArrayStackItem(final AbstractStackItem... values) {
		array = new ArrayList<>(Arrays.asList(values));
	}

	@Override
	public int compareTo(final AbstractStackItem o) {
		final ArrayStackItem that = (ArrayStackItem) o;
		return ModelUtil.compareTo(array, that.array);
	}

	@Override
	public List<AbstractStackItem> getArray() {
		return array;
	}

	@Override
	public boolean getBoolean() {
		return array.size() > 0;
	}

	@Override
	public byte[] getByteArray() {
		throw new NotImplementedException("GetByteArray");
	}

	/**
	 * return the array size.
	 *
	 * @return the array size.
	 */
	public int getCount() {
		return array.size();
	}

	@Override
	public boolean isArray() {
		return true;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	/**
	 * reverses the array.
	 */
	public void reverse() {
		Collections.reverse(array);
	}
}

// using System;
// using System.Collections.Generic;
// using System.Linq;
//
// namespace Neo.VM.Types
// {
// internal class Array : StackItem
// {
// protected readonly List<StackItem> _array;
//
// public override bool IsArray => true;
//
// public Array(IEnumerable<StackItem> value)
// {
// this._array = value as List<StackItem> ?? value.ToList();
// }
//
// public override bool Equals(StackItem other)
// {
// if (ReferenceEquals(this, other)) return true;
// if (ReferenceEquals(null, other)) return false;
// Array a = other as Array;
// if (a == null)
// return false;
// else
// return _array.SequenceEqual(a._array);
// }
//
// public override IList<StackItem> GetArray()
// {
// return _array;
// }
//
// public override bool GetBoolean()
// {
// return _array.Count > 0;
// }
//
// public override byte[] GetByteArray()
// {
// throw new NotSupportedException();
// }
//
// public void Reverse()
// {
// _array.Reverse();
// }
// }
// }
