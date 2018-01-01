package neo.model.network;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.core.Header;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * the header payload object.
 *
 * @author coranos
 *
 */
public final class HeadersPayload implements Payload, ToJsonObject, ByteArraySerializable {

	/**
	 * the list of headers.
	 */
	private final List<Header> headerList;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the byte buffer to read.
	 */
	public HeadersPayload(final ByteBuffer bb) {
		headerList = ModelUtil.readVariableLengthList(bb, Header.class);
	}

	/**
	 * return the header list.
	 *
	 * @return the header list.
	 */
	public List<Header> getHeaderList() {
		return Collections.unmodifiableList(headerList);
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		NetworkUtil.write(out, headerList);
		return out.toByteArray();

	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		final JSONArray headerListJson = new JSONArray();
		json.put("headerList", headerListJson);

		for (final Header header : headerList) {
			headerListJson.put(header.toJSONObject());
		}

		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}

}
