package neo.vm.types;

import java.util.ArrayList;
import java.util.List;

import neo.vm.AbstractStackItem;

/**
 * the Struct stack item.
 *
 * @author coranos
 *
 */
public final class StructStackItem extends AbstractStackItem {

	/**
	 * the internal array.
	 */
	private final ArrayStackItem array;

	/**
	 * the constructor.
	 *
	 * @param values
	 *            the values to use.
	 */
	public StructStackItem(final AbstractStackItem... values) {
		array = new ArrayStackItem(values);
	}

	/**
	 * clones this struct.
	 *
	 * @return the clone.
	 */
	public AbstractStackItem cloneStackItem() {
		final List<AbstractStackItem> array = getArray();
		final List<AbstractStackItem> newArray = new ArrayList<>(array.size());
		for (int i = 0; i < array.size(); i++) {
			if (array.get(i) instanceof StructStackItem) {
				final StructStackItem s = (StructStackItem) array.get(i);
				newArray.add(s.cloneStackItem());
			} else {
				newArray.add(array.get(i));
			}
		}
		return new StructStackItem(newArray.toArray(new AbstractStackItem[0]));
	}

	@Override
	public int compareTo(final AbstractStackItem o) {
		return array.compareTo(o);
	}

	@Override
	public byte[] getByteArray() {
		return array.getByteArray();
	}

	@Override
	public boolean isArray() {
		return true;
	}

	@Override
	public boolean isStruct() {
		return true;
	}
}

// using System.Collections.Generic;
// using System.Linq;
//
// namespace Neo.VM.Types
// {
// internal class Struct : Array
// {
// public override bool IsStruct => true;
//
// public Struct(IEnumerable<StackItem> value) : base(value)
// {
// }
//
// public StackItem Clone()
// {
// List<StackItem> newArray = new List<StackItem>(this._array.Count);
// for (var i = 0; i < _array.Count; i++)
// {
// if (_array[i] is Struct s)
// {
// newArray.Add(s.Clone());
// }
// else
// {
// newArray.Add(_array[i]); //array = 是引用
// //其他的由于是固定值类型，不会改内部值，所以虽然需要复制，直接= 就行
// }
// }
// return new Struct(newArray);
// }
//
// public override bool Equals(StackItem other)
// {
// if (ReferenceEquals(this, other)) return true;
// if (ReferenceEquals(null, other)) return false;
// Struct a = other as Struct;
// if (a == null)
// return false;
// else
// return _array.SequenceEqual(a._array);
// }
// }
// }
