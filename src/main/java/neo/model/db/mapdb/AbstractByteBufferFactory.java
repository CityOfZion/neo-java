package neo.model.db.mapdb;

import java.nio.ByteBuffer;

/**
 * an object for evaluating a Map[String,Object] and creating a class instance
 * containing the same data.
 *
 * @author coranos
 *
 * @param <T>
 *            the type of object.
 */
public abstract class AbstractByteBufferFactory<T> {

	/**
	 * creates an object from a map.
	 *
	 * @param bb
	 *            the byte buffer to use.
	 * @return the object that was created.
	 */
	public abstract T toObject(ByteBuffer bb);
}
