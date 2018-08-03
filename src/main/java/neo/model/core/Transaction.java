package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.db.BlockDb;
import neo.model.network.Payload;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;
import neo.model.util.SHA256HashUtil;
import neo.model.util.TransactionUtil;
import neo.vm.IInteropInterface;
import neo.vm.IScriptContainer;

/**
 * the transaction.
 *
 * @author coranos
 *
 */
public final class Transaction implements ToJsonObject, ByteArraySerializable, Serializable, Payload, IScriptContainer {

	private static final long serialVersionUID = 1L;

	/**
	 * return the comparator used for comparing Transactions.
	 *
	 * @return the comparator used for comparing Transactions.
	 */
	public static Comparator<Transaction> getComparator() {
		final Comparator<Transaction> c = Comparator.comparing((final Transaction transaction) -> transaction.hash);
		return c;
	}

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
	 * the hash.
	 */
	private UInt256 hash;

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
		attributes = ModelUtil.readVariableLengthList(bb, TransactionAttribute.class);
		inputs = ModelUtil.readVariableLengthList(bb, CoinReference.class);
		outputs = ModelUtil.readVariableLengthList(bb, TransactionOutput.class);
		scripts = ModelUtil.readVariableLengthList(bb, Witness.class);

		recalculateHash();
	}

	/**
	 * return the hash, as calculated from the other parameters.
	 *
	 * @return the hash, as calculated from the other parameters.
	 */
	private UInt256 calculateHash() {
		final byte[] hashDataBa = getHashData();
		final byte[] hashBa = SHA256HashUtil.getDoubleSHA256Hash(hashDataBa);
		return new UInt256(hashBa);
	}

	@Override
	public int compareTo(final IInteropInterface object) {
		final Transaction that = (Transaction) object;
		return getComparator().compare(this, that);
	}

	/**
	 * returns the hash.
	 *
	 * @return the hash.
	 */
	public UInt256 getHash() {
		return hash;
	}

	/**
	 * returns the data used in hashing (which is everying but the scripts).
	 *
	 * @return the data used in hashing.
	 */
	private byte[] getHashData() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		writeHashData(bout);
		final byte[] hashDataBa = bout.toByteArray();
		return hashDataBa;
	}

	@Override
	public byte[] getMessage() {
		return getHashData();
	}

	/**
	 * return the script hashes used for verification.
	 *
	 * @param blockDb
	 *            the blockdb to use.
	 * @return the script hashes used for verification.
	 */
	public UInt160[] getScriptHashesForVerifying(final BlockDb blockDb) {
		final Set<UInt160> hashes = new LinkedHashSet<>();
		for (final CoinReference cr : inputs) {
			final TransactionOutput to = ModelUtil.getTransactionOutput(blockDb, cr);
			hashes.add(to.scriptHash);
		}
		for (final TransactionAttribute attribute : attributes) {
			if (attribute.usage.equals(TransactionAttributeUsage.SCRIPT)) {
				hashes.add(new UInt160(attribute.getCopyOfData()));
			}
		}

		if (type.equals(TransactionType.ISSUE_TRANSACTION)) {
			// TODO: handle issue transactions.
		}

		// TODO: add AssetState duty flag code.
		// foreach (var group in Outputs.GroupBy(p => p.AssetId))
		// {
		// AssetState asset = Blockchain.Default.GetAssetState(group.Key);
		// if (asset == null) throw new InvalidOperationException();
		// if (asset.AssetType.HasFlag(AssetType.DutyFlag))
		// {
		// hashes.UnionWith(group.Select(p => p.ScriptHash));
		// }
		// }

		return hashes.toArray(new UInt160[0]);
	}

	/**
	 * recalulates the hash.
	 */
	public void recalculateHash() {
		hash = calculateHash();
	}

	/**
	 * return a byte array containing only the base data, no inputs outputs or
	 * scripts.
	 *
	 * @return a byte array containing only the base data, no inputs outputs or
	 *         scripts.
	 */
	public byte[] toBaseByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		writeBaseData(bout);
		NetworkUtil.write(bout, Collections.emptyList());
		NetworkUtil.write(bout, Collections.emptyList());
		NetworkUtil.write(bout, Collections.emptyList());
		return bout.toByteArray();
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		writeHashData(bout);
		NetworkUtil.write(bout, scripts);
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();
		final int versionInt = version & 0xff;
		final boolean ifNullReturnEmpty = false;
		json.put("type", type);
		json.put("version", versionInt);
		json.put("attributes", ModelUtil.toJSONArray(ifNullReturnEmpty, attributes));
		json.put("inputs", ModelUtil.toJSONArray(ifNullReturnEmpty, inputs));
		json.put("outputs", ModelUtil.toJSONArray(ifNullReturnEmpty, outputs));
		json.put("scripts", ModelUtil.toJSONArray(ifNullReturnEmpty, scripts));
		json.put("exclusiveData", exclusiveData.toJSONObject());
		json.put("exclusiveDataType", exclusiveData.getClass().getSimpleName());

		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}

	/**
	 * writes the base data (type, version, attributes, exclusiveData) to the output
	 * stream.
	 *
	 * @param bout
	 *            the output stream to use.
	 */
	private void writeBaseData(final OutputStream bout) {
		NetworkUtil.write(bout, new byte[] { type.getTypeByte() });
		NetworkUtil.write(bout, new byte[] { version });
		NetworkUtil.write(bout, exclusiveData, false);
		NetworkUtil.write(bout, attributes);
	}

	/**
	 * writes the hash data to the output stream.
	 *
	 * @param out
	 *            the output stream to use.
	 */
	private void writeHashData(final OutputStream out) {
		writeBaseData(out);
		NetworkUtil.write(out, inputs);
		NetworkUtil.write(out, outputs);
	}
}
