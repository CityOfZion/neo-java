package neo.vm;

import java.util.ArrayList;
import java.util.List;

/**
 * the random access stack.
 *
 * @author coranos
 *
 * @param <T>
 *            the type to use.
 */
public class RandomAccessStack<T> {

	/**
	 * index exceeds the list size.
	 */
	private static final String INDEX_EXCEEDS_LIST_SIZE = "index > list.size()";

	/**
	 * the stack.
	 */
	private final List<T> stack = new ArrayList<>();

	/**
	 * clears the stack.
	 */
	public void clear() {
		stack.clear();
	}

	/**
	 * return the count.
	 *
	 * @return the count.
	 */
	public int getCount() {
		return stack.size();
	}

	/**
	 * inserts into the stack.
	 *
	 * @param index
	 *            the index to use.
	 * @param item
	 *            the item to use.
	 */
	public void insert(final int index, final T item) {
		if (index > stack.size()) {
			throw new RuntimeException(INDEX_EXCEEDS_LIST_SIZE);
		}
		stack.add(stack.size() - index, item);
	}

	/**
	 * peeks at the next entry.
	 *
	 * @return the item.
	 */
	public T peek() {
		return peek(0);
	}

	/**
	 * peeks at the given entry.
	 *
	 * @param index
	 *            the index to look at.
	 * @return the item.
	 */
	public T peek(final int index) {
		if (index > stack.size()) {
			throw new RuntimeException(INDEX_EXCEEDS_LIST_SIZE);
		}
		return stack.get(stack.size() - 1 - index);
	}

	/**
	 * pops the top entry.
	 *
	 * @return the top entry.
	 */
	public T pop() {
		return remove(0);
	}

	/**
	 * pushes an item onto the stack.
	 *
	 * @param item
	 *            the item to push.
	 */
	public void push(final T item) {
		stack.add(item);
	}

	/**
	 * removes the object at the index.
	 *
	 * @param index
	 *            the index to use.
	 * @return the object.
	 */
	public T remove(final int index) {
		if (index > stack.size()) {
			throw new RuntimeException(INDEX_EXCEEDS_LIST_SIZE);
		}
		final T item = stack.get(stack.size() - index - 1);
		stack.remove(stack.size() - index - 1);
		return item;
	}

	/**
	 * sets the value at the given index to the given value, discarding the old
	 * value.
	 *
	 * @param index
	 *            the index to use.
	 * @param item
	 *            the item to use.
	 */
	public void set(final int index, final T item) {
		if (index > stack.size()) {
			throw new RuntimeException(INDEX_EXCEEDS_LIST_SIZE);
		}
		stack.set(stack.size() - index - 1, item);
	}
}

// using System;
// using System.Collections;
// using System.Collections.Generic;
//
// namespace Neo.VM
// {
// public class RandomAccessStack<T> : IReadOnlyCollection<T>
// {
// private readonly List<T> list = new List<T>();
//
// public int Count => list.Count;
//
// public void Clear()
// {
// list.Clear();
// }
//
// public IEnumerator<T> GetEnumerator()
// {
// return list.GetEnumerator();
// }
//
// IEnumerator IEnumerable.GetEnumerator()
// {
// return GetEnumerator();
// }
//
// public void Insert(int index, T item)
// {
// if (index > list.Count) throw new InvalidOperationException();
// list.Insert(list.Count - index, item);
// }
//
// public T Peek(int index = 0)
// {
// if (index >= list.Count) throw new InvalidOperationException();
// return list[list.Count - 1 - index];
// }
//
// public T Pop()
// {
// return Remove(0);
// }
//
// public void Push(T item)
// {
// list.Add(item);
// }
//
// public T Remove(int index)
// {
// if (index >= list.Count) throw new InvalidOperationException();
// T item = list[list.Count - index - 1];
// list.RemoveAt(list.Count - index - 1);
// return item;
// }
//
// public void Set(int index, T item)
// {
// if (index >= list.Count) throw new InvalidOperationException();
// list[list.Count - index - 1] = item;
// }
// }
// }
