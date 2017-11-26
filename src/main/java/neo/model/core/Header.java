package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;

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
	 * @return the Comparator used to sort headers, by index (as a long).
	 */
	public static Comparator<Header> getComparator() {
		final Comparator<Header> c = Comparator.comparing(Header::getIndexAsLong).reversed();
		return c;
	}

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
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			writeBaseToOutputStream(bout);
			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();
		addBaseToJSONObject(json);
		return json;
	}
}
