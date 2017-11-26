package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt64;
import neo.model.keystore.ByteArraySerializable;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

public class InvocationExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	public byte[] script;

	public Fixed8 gas;

	private final byte version;

	public InvocationExclusiveData(final byte version, final ByteBuffer bb) {
		this.version = version;
		script = ModelUtil.getByteArray(bb);
		if (version >= 1) {
			gas = ModelUtil.getFixed8(bb);
		} else {
			gas = ModelUtil.getFixed8(ByteBuffer.wrap(new byte[UInt64.SIZE]));
		}
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			NetworkUtil.writeByteArray(bout, script);
			if (version >= 1) {
				NetworkUtil.write(bout, gas, true);
			}
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		json.put("script", ModelUtil.toHexString(script));
		json.put("gas", gas.value);

		return json;
	}

}
