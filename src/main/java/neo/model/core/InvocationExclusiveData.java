package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt64;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * exclusive data for enrollment transactions.
 *
 * @author coranos
 */
public final class InvocationExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the script.
	 */
	public final byte[] script;

	/**
	 * the gas.
	 */
	public final Fixed8 gas;

	/**
	 * the version.
	 */
	private final byte version;

	/**
	 * the constructor.
	 *
	 * @param version
	 *            the trsansaction version.
	 *
	 * @param bb
	 *            the byte buffer to read.
	 */
	public InvocationExclusiveData(final byte version, final ByteBuffer bb) {
		this.version = version;
		script = ModelUtil.getVariableLengthByteArray(bb);
		if (version >= 1) {
			gas = ModelUtil.getFixed8(bb);
		} else {
			gas = ModelUtil.getFixed8(ByteBuffer.wrap(new byte[UInt64.SIZE]));
		}
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		NetworkUtil.writeByteArray(bout, script);
		if (version >= 1) {
			NetworkUtil.write(bout, gas, true);
		}
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		json.put("script", ModelUtil.toHexString(script));
		json.put("gas", gas.value);

		return json;
	}

}
