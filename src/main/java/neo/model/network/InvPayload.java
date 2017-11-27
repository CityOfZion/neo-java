package neo.model.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.bytes.UInt256;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

public class InvPayload implements Payload, ByteArraySerializable {

	public static final int MAX_HASHES = 2000;

	private final byte type;

	private final List<UInt256> hashes;

	private final InventoryType typeEnum;

	public InvPayload(final ByteBuffer bb) {
		type = bb.get();
		typeEnum = InventoryType.valueOf(type);
		hashes = ModelUtil.readArray(bb, UInt256.class);
	}

	public InvPayload(final InventoryType typeEnum, final UInt256... hashes) {
		this.typeEnum = typeEnum;
		type = typeEnum.getTypeByte();
		this.hashes = new ArrayList<>(Arrays.asList(hashes));
	}

	public List<UInt256> getHashes() {
		return hashes;
	}

	public InventoryType getType() {
		return typeEnum;
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(new byte[] { type });
			NetworkUtil.writeVarInt(out, hashes.size());
			for (int ix = 0; ix < hashes.size(); ix++) {
				out.write(hashes.get(ix).toByteArray());
			}
			return out.toByteArray();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		final JSONObject json = new JSONObject();

		json.put("type", type);

		final JSONArray hashesJson = new JSONArray();
		json.put("hashes", hashesJson);

		for (final UInt256 hash : hashes) {
			hashesJson.put(hash.toHexString());
		}

		return json.toString();
	}
}
