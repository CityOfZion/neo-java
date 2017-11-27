package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Comparator;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.bytes.UInt32;
import neo.model.bytes.UInt64;
import neo.model.util.Base58Util;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;
import neo.model.util.SHA256HashUtil;

/**
 * the common base of a block or a header.
 *
 * @author coranos
 *
 */
public abstract class AbstractBlockBase
		implements ToJsonObject, ByteArraySerializable, Comparable<AbstractBlockBase>, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the comparator for sorting AbstractBlockBase objects.
	 */
	private static final Comparator<AbstractBlockBase> ABSTRACT_BLOCK_BASE_COMPARATOR = getAbstractBlockBaseComparator();

	/**
	 * return the comparator for sorting AbstractBlockBase objects.
	 *
	 * @return the comparator for sorting AbstractBlockBase objects.
	 */
	public static Comparator<AbstractBlockBase> getAbstractBlockBaseComparator() {
		final Comparator<AbstractBlockBase> c = Comparator
				.comparing((final AbstractBlockBase abstractBlockBase) -> abstractBlockBase.getIndexAsLong())
				.thenComparing(abstractBlockBase -> abstractBlockBase.index)
				.thenComparing(abstractBlockBase -> abstractBlockBase.prevHash)
				.thenComparing(abstractBlockBase -> abstractBlockBase.merkleRoot)
				.thenComparing(abstractBlockBase -> abstractBlockBase.timestamp)
				.thenComparing(abstractBlockBase -> abstractBlockBase.version)
				.thenComparing(abstractBlockBase -> abstractBlockBase.consensusData)
				.thenComparing(abstractBlockBase -> abstractBlockBase.nextConsensus)
				.thenComparing(abstractBlockBase -> abstractBlockBase.script);
		return c;
	}

	/**
	 * the version.
	 */
	public final UInt32 version;

	/**
	 * the previous hash.
	 */
	public final UInt256 prevHash;

	/**
	 * the merkle root, used in validating the transactions in the block.
	 */
	public final UInt256 merkleRoot;

	/**
	 * the timestamp.
	 */
	public final UInt32 timestamp;

	/**
	 * the index.
	 */
	public final UInt32 index;

	/**
	 * the consensus data.
	 */
	public final UInt64 consensusData;

	/**
	 * the next consensus.
	 */
	public final UInt160 nextConsensus;

	/**
	 * the script.
	 */
	public final Witness script;

	/**
	 * the hash.
	 */
	public final UInt256 hash;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public AbstractBlockBase(final ByteBuffer bb) {
		version = ModelUtil.getUInt32(bb);
		prevHash = ModelUtil.getUInt256(bb, true);
		merkleRoot = ModelUtil.getUInt256(bb);
		timestamp = ModelUtil.getUInt32(bb);
		index = ModelUtil.getUInt32(bb);
		consensusData = ModelUtil.getUInt64(bb);
		nextConsensus = ModelUtil.getUInt160(bb, false);
		final byte checkWitnessByte = ModelUtil.getByte(bb);
		if ((checkWitnessByte != 1) && (checkWitnessByte != 0)) {
			throw new RuntimeException("checkWitnessByte should be 1 or 0, was " + checkWitnessByte);
		}
		script = new Witness(bb);
		hash = calculateHash();
	}

	/**
	 * adds the base fields to the JSONObject.
	 *
	 * @param json
	 *            the json object to add fields to.
	 */
	protected final void addBaseToJSONObject(final JSONObject json) {
		json.put("version", version.toPositiveBigInteger());
		json.put("previousblockhash", "0x" + prevHash.toHexString());
		json.put("merkleroot", "0x" + merkleRoot.toHexString());
		json.put("time", timestamp.toPositiveBigInteger());
		json.put("index", index.toPositiveBigInteger());
		final String nextConsensusAddress = ModelUtil.toAddress(nextConsensus);
		json.put("nextconsensus", nextConsensusAddress);
		final byte[] nextconsensusHash = Base58Util.decode(nextConsensusAddress);
		json.put("nextconsensusHash", ModelUtil.toHexString(nextconsensusHash));
		json.put("script", script.toJSONObject());
		json.put("hash", "0x" + hash.toReverseHexString());
	}

	/**
	 * return the hash, as calculated from the other parameters.
	 *
	 * @return the hash, as calculated from the other parameters.
	 */
	private UInt256 calculateHash() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			bout.write(nextConsensus.toByteArray());
			bout.write(consensusData.toByteArray());
			bout.write(index.toByteArray());
			bout.write(timestamp.toByteArray());
			bout.write(merkleRoot.toByteArray());
			final byte[] prevHashBa = prevHash.toByteArray();
			ArrayUtils.reverse(prevHashBa);
			bout.write(prevHashBa);
			bout.write(version.toByteArray());

			final byte[] hashDataBa = bout.toByteArray();
			ArrayUtils.reverse(hashDataBa);
			final byte[] hashBa = SHA256HashUtil.getDoubleSHA256Hash(hashDataBa);
			return new UInt256(hashBa);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final int compareTo(final AbstractBlockBase that) {
		return ABSTRACT_BLOCK_BASE_COMPARATOR.compare(this, that);
	}

	/**
	 * return the index, as a long.
	 *
	 * @return the index, as a long.
	 */
	public final long getIndexAsLong() {
		return index.asLong();
	}

	@Override
	public abstract byte[] toByteArray();

	/**
	 * write the object to an output stream.
	 *
	 * @param out
	 *            the output stream to write to.
	 *
	 * @throws IOException
	 *             if an error occurs.
	 */
	protected void writeBaseToOutputStream(final OutputStream out) throws IOException {
		NetworkUtil.write(out, version, true);
		NetworkUtil.write(out, prevHash, false);
		NetworkUtil.write(out, merkleRoot, true);
		NetworkUtil.write(out, timestamp, true);
		NetworkUtil.write(out, index, true);
		NetworkUtil.write(out, consensusData, true);
		NetworkUtil.write(out, nextConsensus, true);
		if (script == null) {
			out.write(new byte[] { 0 });
		} else {
			out.write(new byte[] { 1 });
		}
		NetworkUtil.write(out, script, false);
	}
}
