package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.crypto.ecc.ECCurve;
import neo.model.crypto.ecc.ECPoint;
import neo.model.keystore.ByteArraySerializable;
import neo.model.util.ModelUtil;

/**
 * exclusive data for enrollment transactions.
 *
 * @author coranos
 */
public final class EnrollmentExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the public key.
	 */
	public final ECPoint publicKey;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public EnrollmentExclusiveData(final ByteBuffer bb) {
		publicKey = ECPoint.DeserializeFrom(bb, ECCurve.Secp256r1);
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			bout.write(publicKey.toByteArray());
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		json.put("publicKey", ModelUtil.toHexString(publicKey.toByteArray()));

		return json;
	}

}
