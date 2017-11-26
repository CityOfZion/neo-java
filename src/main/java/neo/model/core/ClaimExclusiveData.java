package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.keystore.ByteArraySerializable;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

public class ClaimExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	public final List<CoinReference> claims;

	public ClaimExclusiveData(final ByteBuffer bb) {
		claims = ModelUtil.readArray(bb, CoinReference.class);
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			NetworkUtil.write(bout, claims);
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		json.put("claims", ModelUtil.toJSONArray(claims));

		return json;
	}

}