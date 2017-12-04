package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.util.ModelUtil;

/**
 * the header of a block, withount any transactions.
 *
 * @author coranos
 *
 */
public final class Header extends AbstractBlockBase {

	private static final long serialVersionUID = 1L;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public Header(final ByteBuffer bb) {
		super(bb);
		final byte headerLastByte = ModelUtil.getByte(bb);
		if (headerLastByte != 0) {
			throw new RuntimeException(
					"headerLastByte should be 0, was " + headerLastByte + " at position " + bb.position());
		}
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		writeBaseToOutputStream(bout);
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();
		addBaseToJSONObject(json);
		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}
}
