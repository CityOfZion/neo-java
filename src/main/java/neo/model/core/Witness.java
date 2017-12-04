package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * the witness.
 *
 * @author coranos
 *
 */
public final class Witness implements ToJsonObject, ByteArraySerializable, Serializable, Comparable<Witness> {

	private static final long serialVersionUID = 1L;

	/**
	 * the invocation script.
	 */
	private final byte[] invocationScript;

	/**
	 * the verification script.
	 */
	private final byte[] verificationScript;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public Witness(final ByteBuffer bb) {
		invocationScript = ModelUtil.getByteArray(bb);
		verificationScript = ModelUtil.getByteArray(bb);
	}

	@Override
	public int compareTo(final Witness that) {
		final int iC = ByteBuffer.wrap(invocationScript).compareTo(ByteBuffer.wrap(that.invocationScript));
		if (iC != 0) {
			return iC;
		}
		final int vC = ByteBuffer.wrap(verificationScript).compareTo(ByteBuffer.wrap(that.verificationScript));
		if (vC != 0) {
			return vC;
		}

		return 0;
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		NetworkUtil.writeByteArray(bout, invocationScript);
		NetworkUtil.writeByteArray(bout, verificationScript);
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		json.put("invocation", new String(Hex.encodeHex(invocationScript)));
		json.put("verification", new String(Hex.encodeHex(verificationScript)));

		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}
}
