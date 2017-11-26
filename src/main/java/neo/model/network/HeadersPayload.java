package neo.model.network;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.core.Header;
import neo.model.util.ModelUtil;

public class HeadersPayload implements Payload, ToJsonObject {

	private final List<Header> headerList;

	public HeadersPayload(final ByteBuffer bb) {
		headerList = ModelUtil.readArray(bb, Header.class);
	}

	public List<Header> getHeaderList() {
		return Collections.unmodifiableList(headerList);
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
