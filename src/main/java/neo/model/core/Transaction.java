package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;
import neo.model.util.TransactionUtil;

/**
 * the transaction.
 *
 * @author coranos
 *
 */
public final class Transaction implements ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the transaction type.
	 */
	public final TransactionType type;

	/**
	 * the version.
	 */
	public final byte version;

	/**
	 * the exclusive data.
	 */
	public final ExclusiveData exclusiveData;

	/**
	 * the transaction attributes.
	 */
	public final List<TransactionAttribute> attributes;

	/**
	 * the transaction inputs.
	 */
	public final List<CoinReference> inputs;

	/**
	 * the transaction outputs.
	 */
	public final List<TransactionOutput> outputs;

	/**
	 * the scripts.
	 */
	public final List<Witness> scripts;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public Transaction(final ByteBuffer bb) {
		type = TransactionType.valueOfByte(ModelUtil.getByte(bb));
		version = ModelUtil.getByte(bb);
		exclusiveData = TransactionUtil.deserializeExclusiveData(type, version, bb);
		attributes = ModelUtil.readArray(bb, TransactionAttribute.class);
		inputs = ModelUtil.readArray(bb, CoinReference.class);
		outputs = ModelUtil.readArray(bb, TransactionOutput.class);
		scripts = ModelUtil.readArray(bb, Witness.class);
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		NetworkUtil.write(bout, new byte[] { type.getTypeByte() });
		NetworkUtil.write(bout, new byte[] { version });
		NetworkUtil.write(bout, exclusiveData, false);
		NetworkUtil.write(bout, attributes);
		NetworkUtil.write(bout, inputs);
		NetworkUtil.write(bout, outputs);
		NetworkUtil.write(bout, scripts);
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();
		final int versionInt = version & 0xff;
		json.put("type", type);
		json.put("version", versionInt);
		json.put("attributes", ModelUtil.toJSONArray(attributes));
		json.put("inputs", ModelUtil.toJSONArray(inputs));
		json.put("outputs", ModelUtil.toJSONArray(outputs));
		json.put("scripts", ModelUtil.toJSONArray(scripts));
		json.put("exclusiveData", exclusiveData.toJSONObject());
		json.put("exclusiveDataType", exclusiveData.getClass().getSimpleName());

		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}

}
