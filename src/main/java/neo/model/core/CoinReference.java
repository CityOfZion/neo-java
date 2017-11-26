package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.bytes.UInt16;
import neo.model.bytes.UInt256;
import neo.model.keystore.ByteArraySerializable;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

public class CoinReference implements ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	public UInt256 prevHash;
	public UInt16 prevIndex;

	public CoinReference(final ByteBuffer bb) {
		prevHash = ModelUtil.getUInt256(bb);
		prevIndex = ModelUtil.getUInt16(bb);
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			NetworkUtil.write(bout, prevHash, true);
			NetworkUtil.write(bout, prevIndex, true);
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();
		json.put("previousblockhash", "0x" + prevHash.toHexString());
		json.put("previousindex", "0x" + prevIndex.toHexString());
		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}

}
