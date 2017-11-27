package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.bytes.UInt16;
import neo.model.bytes.UInt256;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * a reference to a coin, used to claim coins, and used as transaction inputs.
 *
 * @author coranos
 *
 */
public final class CoinReference implements ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the hash of the block that contains this coin reference.
	 */
	public final UInt256 prevHash;

	/**
	 * the index of the transaction in the block.
	 */
	public final UInt16 prevIndex;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public CoinReference(final ByteBuffer bb) {
		prevHash = ModelUtil.getUInt256(bb);
		prevIndex = ModelUtil.getUInt16(bb);
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			NetworkUtil.write(bout, prevHash, true);
			NetworkUtil.write(bout, prevIndex, true);
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();
		json.put("previousblockhash", "0x" + prevHash.toHexString());
		json.put("previousindex", "0x" + prevIndex.toHexString());
		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}

}
