package neo.model.core;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.keystore.ByteArraySerializable;

public class NoExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

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