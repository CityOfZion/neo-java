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
		type = TransactionType.valueOf(ModelUtil.getByte(bb));
		version = ModelUtil.getByte(bb);
		exclusiveData = deserializeExclusiveData(bb);
		attributes = ModelUtil.readArray(bb, TransactionAttribute.class);
		inputs = ModelUtil.readArray(bb, CoinReference.class);
		outputs = ModelUtil.readArray(bb, TransactionOutput.class);
		scripts = ModelUtil.readArray(bb, Witness.class);
	}

	/**
	 * deserialize the exclusive data based on the transaction type.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 *
	 * @return the exclusive data, or throw an exception if the transaction type is
	 *         not recognized.
	 */
	private ExclusiveData deserializeExclusiveData(final ByteBuffer bb) {
		switch (type) {
		case MINER_TRANSACTION:
			return new MinerExclusiveData(bb);
		case CLAIM_TRANSACTION:
			return new ClaimExclusiveData(bb);
		case CONTRACT_TRANSACTION:
			return new NoExclusiveData(bb);
		case ENROLLMENT_TRANSACTION:
			return new EnrollmentExclusiveData(bb);
		case INVOCATION_TRANSACTION:
			return new InvocationExclusiveData(version, bb);
		case ISSUE_TRANSACTION:
			return new NoExclusiveData(bb);
		case PUBLISH_TRANSACTION:
			return new PublishExclusiveData(version, bb);
		case REGISTER_TRANSACTION:
			return new RegisterExclusiveData(bb);
		default:
			throw new RuntimeException("unknown type:" + type);
		}
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			bout.write(new byte[] { type.getTypeByte() });
			bout.write(new byte[] { version });
			NetworkUtil.write(bout, exclusiveData, false);
			NetworkUtil.write(bout, attributes);
			NetworkUtil.write(bout, inputs);
			NetworkUtil.write(bout, outputs);
			NetworkUtil.write(bout, scripts);
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
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
