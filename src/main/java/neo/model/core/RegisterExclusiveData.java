package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.crypto.ecc.ECCurve;
import neo.model.crypto.ecc.ECPoint;
import neo.model.keystore.ByteArraySerializable;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

public class RegisterExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	public final AssetType assetType;
	private final byte[] nameBa;
	public final String name;
	public final Fixed8 amount;
	public final byte precision;
	public final ECPoint owner;
	public final UInt160 admin;

	public RegisterExclusiveData(final ByteBuffer bb) {
		assetType = AssetType.valueOf(ModelUtil.getByte(bb));
		nameBa = ModelUtil.getByteArray(bb);
		name = new String(nameBa, StandardCharsets.UTF_8);
		amount = ModelUtil.getFixed8(bb);
		precision = ModelUtil.getByte(bb);
		owner = ECPoint.DeserializeFrom(bb, ECCurve.Secp256r1);
		admin = ModelUtil.getUInt160(bb, false);
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			bout.write(new byte[] { assetType.getTypeByte() });
			NetworkUtil.writeByteArray(bout, nameBa);
			NetworkUtil.write(bout, amount, true);
			bout.write(new byte[] { precision });
			NetworkUtil.write(bout, owner.toByteArray(), true);
			NetworkUtil.write(bout, admin, true);
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		json.put("assetType", assetType);
		json.put("name", name);
		json.put("amount", amount.value);
		json.put("amountHex", ModelUtil.toReverseHexString(amount.toByteArray()));
		final int precisionInt = precision & 0xff;
		json.put("precision", precisionInt);
		json.put("precisionHex", ModelUtil.toReverseHexString(precision));
		json.put("owner", ModelUtil.toHexString(owner.toByteArray()));
		json.put("ownerError", owner.error);
		json.put("admin", admin.toHexString());

		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}

}
