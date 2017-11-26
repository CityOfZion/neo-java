package neo.model.network;

import java.nio.ByteBuffer;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import neo.model.util.ModelUtil;

public class AddrPayload implements Payload {

	public final List<NetworkAddressWithTime> addressList;

	public AddrPayload(final ByteBuffer bb) {
		addressList = ModelUtil.readArray(bb, NetworkAddressWithTime.class);
	}

	@Override
	public String toString() {
		final JSONObject json = new JSONObject();

		final JSONArray addressListJson = new JSONArray();
		json.put("addressList", addressListJson);

		for (final NetworkAddressWithTime nawt : addressList) {
			addressListJson.put(nawt.toJson());
		}

		return json.toString();
	}

}
