package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.keystore.ByteArraySerializable;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

public class TransactionOutput implements ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	public final UInt256 assetId;
	public final Fixed8 value;
	public final UInt160 scriptHash;

	public TransactionOutput(final ByteBuffer bb) {
		assetId = ModelUtil.getUInt256(bb);
		value = ModelUtil.getFixed8(bb);
		scriptHash = ModelUtil.getUInt160(bb, false);
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			NetworkUtil.write(bout, assetId, true);
			NetworkUtil.write(bout, value, true);
			NetworkUtil.write(bout, scriptHash, true);
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		json.put("assetId", assetId.toHexString());
		json.put("value", value.value);
		json.put("valueHex", ModelUtil.toReverseHexString(value.toByteArray()));
		json.put("scriptHash", scriptHash.toReverseHexString());

		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}

}
