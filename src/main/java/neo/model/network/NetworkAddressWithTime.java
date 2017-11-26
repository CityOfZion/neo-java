package neo.model.network;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.bytes.UInt128;
import neo.model.bytes.UInt16;
import neo.model.bytes.UInt32;
import neo.model.bytes.UInt64;
import neo.model.keystore.ByteArraySerializable;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

public class NetworkAddressWithTime implements ByteArraySerializable {

	public final UInt32 timestamp;

	public final UInt64 services;

	public final UInt128 address;

	public final UInt16 port;

	public NetworkAddressWithTime(final ByteBuffer bb) {
		timestamp = ModelUtil.getUInt32(bb);
		services = ModelUtil.getUInt64(bb);
		address = ModelUtil.getUInt128(bb);
		port = ModelUtil.getUInt16(bb);
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			NetworkUtil.write(bout, timestamp, true);
			NetworkUtil.write(bout, services, true);
			NetworkUtil.write(bout, address, true);
			NetworkUtil.write(bout, port, true);
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public JSONObject toJson() {
		final JSONObject json = new JSONObject();

		json.put("timestamp", timestamp.toPositiveBigInteger());
		json.put("services", services.toPositiveBigInteger());
		json.put("address", address.toPositiveBigInteger());
		json.put("port", port.toPositiveBigInteger());

		return json;
	}

	@Override
	public String toString() {
		return address.toHexString() + ":" + port.toPositiveBigInteger();
	}

}
