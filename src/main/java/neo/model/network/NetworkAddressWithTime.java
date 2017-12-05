package neo.model.network;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.bytes.UInt128;
import neo.model.bytes.UInt16;
import neo.model.bytes.UInt32;
import neo.model.bytes.UInt64;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * the network address, with timestamp.
 *
 * @author coranos
 *
 */
public final class NetworkAddressWithTime implements ToJsonObject, ByteArraySerializable {

	/**
	 * the timestamp.
	 */
	public final UInt32 timestamp;

	/**
	 * the services flag.
	 */
	public final UInt64 services;

	/**
	 * the address.
	 */
	public final UInt128 address;

	/**
	 * the port.
	 */
	public final UInt16 port;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the byte buffer to use.
	 */
	public NetworkAddressWithTime(final ByteBuffer bb) {
		timestamp = ModelUtil.getUInt32(bb);
		services = ModelUtil.getUInt64(bb);
		address = ModelUtil.getUInt128(bb);
		port = ModelUtil.getUInt16(bb);
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		NetworkUtil.write(bout, timestamp, true);
		NetworkUtil.write(bout, services, true);
		NetworkUtil.write(bout, address, true);
		NetworkUtil.write(bout, port, true);
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
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
