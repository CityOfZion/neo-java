package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.crypto.ecc.ECCurve;
import neo.model.crypto.ecc.ECPoint;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * exclusive data for registration transactions.
 *
 * @author coranos
 */
public final class RegisterExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the asset type.
	 */
	public final AssetType assetType;

	/**
	 * the internal byte array that represents the name.
	 */
	private final byte[] nameBa;

	/**
	 * the name.
	 */
	public final String name;

	/**
	 * the amount.
	 */
	public final Fixed8 amount;

	/**
	 * the precision.
	 */
	public final byte precision;

	/**
	 * the owner.
	 */
	public final ECPoint owner;

	/**
	 * the admin.
	 */
	public final UInt160 admin;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public RegisterExclusiveData(final ByteBuffer bb) {
		assetType = AssetType.valueOfByte(ModelUtil.getByte(bb));
		nameBa = ModelUtil.getVariableLengthByteArray(bb);
		name = new String(nameBa, StandardCharsets.UTF_8);
		amount = ModelUtil.getFixed8(bb);
		precision = ModelUtil.getByte(bb);
		owner = ECPoint.DeserializeFrom(bb, ECCurve.Secp256r1);
		admin = ModelUtil.getUInt160(bb, false);
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		NetworkUtil.write(bout, new byte[] { assetType.getTypeByte() });
		NetworkUtil.writeByteArray(bout, nameBa);
		NetworkUtil.write(bout, amount, true);
		NetworkUtil.write(bout, new byte[] { precision });
		NetworkUtil.write(bout, owner.toByteArray(), true);
		NetworkUtil.write(bout, admin, true);
		return bout.toByteArray();
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
