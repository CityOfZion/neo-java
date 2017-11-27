package neo.model.core;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.keystore.ByteArraySerializable;

/**
 * exclusive data for transactions that have no exclusive data..
 *
 * @author coranos
 */
public final class NoExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public NoExclusiveData(final ByteBuffer bb) {
	}

	@Override
	public byte[] toByteArray() {
		return new byte[0];
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();
		return json;
	}

}