package neo.vm;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;

public abstract class StateBase implements IInteropInterface, ToJsonObject, ByteArraySerializable {

	public static final byte STATE_VERSION = 0;

	public static final int SIZE = 1;

	public int getSize() {
		return SIZE;
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { STATE_VERSION };
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject object = new JSONObject();
		object.put("version", STATE_VERSION);
		return object;
	}
}
