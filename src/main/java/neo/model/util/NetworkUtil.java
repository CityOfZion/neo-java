package neo.model.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import neo.model.keystore.ByteArraySerializable;

public class NetworkUtil {

	public static byte[] getIntByteArray(final long x) {
		final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt((int) x);
		final byte[] ba = buffer.array();
		return ba;
	}

	public static byte[] getLongByteArray(final long x) {
		final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		final byte[] ba = buffer.array();
		return ba;
	}

	public static byte[] getShortByteArray(final long x) {
		final ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
		buffer.putShort((short) x);
		final byte[] ba = buffer.array();
		return ba;
	}

	public static void write(final ByteArrayOutputStream bout, final byte[] ba0, final boolean reversed)
			throws IOException {
		if (reversed) {
			final byte[] ba1 = new byte[ba0.length];
			System.arraycopy(ba0, 0, ba1, 0, ba0.length);
			bout.write(ba1);
		} else {
			bout.write(ba0);
		}
	}

	public static <T extends ByteArraySerializable> void write(final OutputStream out, final List<T> list)
			throws IOException {
		writeVarInt(out, list.size());
		for (final T t : list) {
			out.write(t.toByteArray());
		}
	}

	public static <T extends ByteArraySerializable> void write(final OutputStream out, final T t,
			final boolean reversed) throws IOException {
		final byte[] ba = t.toByteArray();
		if (reversed) {
			ArrayUtils.reverse(ba);
		}
		out.write(ba);
	}

	private static void writeByte(final OutputStream out, final byte value) throws IOException {
		out.write(new byte[] { value });
	}

	public static <T extends ByteArraySerializable> void writeByteArray(final OutputStream out, final byte[] byteArray)
			throws IOException {
		writeVarInt(out, byteArray.length);
		out.write(byteArray);
	}

	public static void writeInt(final OutputStream out, final long x) throws IOException {
		final byte[] ba = getIntByteArray(x);
		out.write(ba);
	}

	public static void writeLong(final OutputStream out, final long x) throws IOException {
		final byte[] ba = getLongByteArray(x);
		out.write(ba);
	}

	public static void writeShort(final OutputStream out, final long x) throws IOException {
		final byte[] ba = getShortByteArray(x);
		ArrayUtils.reverse(ba);
		out.write(ba);
	}

	public static void writeString(final OutputStream out, final int length, final String str)
			throws UnsupportedEncodingException, IOException {
		final byte[] ba = str.getBytes("UTF-8");
		final ByteBuffer bb = ByteBuffer.allocate(length);
		bb.put(ba);
		out.write(bb.array());
	}

	public static void writeString(final OutputStream out, final String str)
			throws UnsupportedEncodingException, IOException {
		final byte[] ba = str.getBytes("UTF-8");
		writeVarInt(out, ba.length);
		out.write(ba);
	}

	public static void writeVarInt(final OutputStream out, final long value) throws IOException {
		if (value < 0) {
			throw new RuntimeException("value out of range:" + value);
		}
		if (value < 0xFD) {
			writeByte(out, (byte) value);
		} else if (value <= 0xFFFF) {
			writeByte(out, (byte) 0xFD);
			writeShort(out, value);
		} else if (value <= 0xFFFFFFFF) {
			writeByte(out, (byte) 0xFE);
			writeInt(out, value);
		} else {
			writeByte(out, (byte) 0xFF);
			writeLong(out, value);
		}
	}

}
