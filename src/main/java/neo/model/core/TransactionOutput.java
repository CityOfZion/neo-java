package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * the transaction output.
 *
 * @author coranos
 *
 */
public final class TransactionOutput
		implements ToJsonObject, ByteArraySerializable, Serializable, Comparable<TransactionOutput> {

	private static final long serialVersionUID = 1L;

	/**
	 * the asset id.
	 */
	public final UInt256 assetId;

	/**
	 * the value.
	 */
	public final Fixed8 value;

	/**
	 * the script hash.
	 */
	public final UInt160 scriptHash;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public TransactionOutput(final ByteBuffer bb) {
		assetId = ModelUtil.getUInt256(bb);
		value = ModelUtil.getFixed8(bb);
		scriptHash = ModelUtil.getUInt160(bb, false);
	}

	/**
	 * the constructor.
	 *
	 * @param assetId
	 *            the asset id to use.
	 * @param value
	 *            the value to use.
	 * @param scriptHash
	 *            the script hash to use.
	 */
	public TransactionOutput(final UInt256 assetId, final Fixed8 value, final UInt160 scriptHash) {
		this.assetId = assetId;
		this.value = value;
		this.scriptHash = scriptHash;
	}

	@Override
	public int compareTo(final TransactionOutput that) {
		final int scriptHashC = scriptHash.compareTo(that.scriptHash);
		if (scriptHashC != 0) {
			return scriptHashC;
		}
		final int assetIdC = assetId.compareTo(that.assetId);
		if (assetIdC != 0) {
			return scriptHashC;
		}
		final int valueC = value.compareTo(that.value);
		if (valueC != 0) {
			return scriptHashC;
		}
		return 0;
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		NetworkUtil.write(bout, assetId, true);
		NetworkUtil.write(bout, value, true);
		NetworkUtil.write(bout, scriptHash, true);
		return bout.toByteArray();
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
