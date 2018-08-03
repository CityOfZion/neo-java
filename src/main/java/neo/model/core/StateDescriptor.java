package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * the state descriptor.
 *
 * @author coranos
 *
 */

public final class StateDescriptor implements ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the type.
	 */
	public final StateType type;

	/**
	 * the key.
	 */
	public final byte[] key;

	/**
	 * the field.
	 */
	public final String field;

	/**
	 * the value.
	 */
	public final byte[] value;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public StateDescriptor(final ByteBuffer bb) {
		type = StateType.valueOfByte(ModelUtil.getByte(bb));
		key = ModelUtil.getVariableLengthByteArray(bb);
		field = ModelUtil.getVariableLengthString(bb);
		value = ModelUtil.getVariableLengthByteArray(bb);
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		NetworkUtil.write(bout, new byte[] { type.getTypeByte() });
		NetworkUtil.writeByteArray(bout, key);
		NetworkUtil.writeString(bout, field);
		NetworkUtil.writeByteArray(bout, value);
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();
		json.put("type", type);
		json.put("key", ModelUtil.toHexString(key));
		json.put("field", field);
		json.put("value", ModelUtil.toHexString(value));
		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}

}
