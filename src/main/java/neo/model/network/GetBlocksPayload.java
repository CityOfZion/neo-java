package neo.model.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import neo.model.bytes.UInt256;
import neo.model.keystore.ByteArraySerializable;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

public class GetBlocksPayload implements ByteArraySerializable {

	public UInt256[] hashStart;

	public UInt256 hashStop;

	public GetBlocksPayload(final ByteBuffer bb) {
		hashStart = ModelUtil.readArray(bb, UInt256.class).toArray(new UInt256[0]);
		hashStop = ModelUtil.getUInt256(bb);
	}

	public GetBlocksPayload(final UInt256 hash_start, final UInt256 hash_stop) {
		hashStart = new UInt256[] { hash_start };
		if (hash_stop == null) {
			hashStop = new UInt256(new byte[UInt256.SIZE]);
		} else {
			hashStop = hash_stop;
		}
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			NetworkUtil.writeVarInt(out, hashStart.length);
			for (int ix = 0; ix < hashStart.length; ix++) {
				out.write(hashStart[ix].toByteArray());
			}
			out.write(hashStop.toByteArray());
			return out.toByteArray();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

}
