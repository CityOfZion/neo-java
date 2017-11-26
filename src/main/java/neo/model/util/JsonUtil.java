package neo.model.util;

import org.json.JSONObject;

public class JsonUtil {

	private static final String MILLISECONDS = "milliseconds";

	private static final String SECONDS = "seconds";

	private static final String MINUTES = "minutes";

	public static long getTime(final JSONObject json) {
		if (json.has(MILLISECONDS)) {
			return json.getLong(MILLISECONDS);
		}
		if (json.has(SECONDS)) {
			return json.getLong(SECONDS) * 1000;
		}
		if (json.has(MINUTES)) {
			return json.getLong(MINUTES) * 1000 * 60;
		}

		throw new RuntimeException("no known time field found in json:" + json);
	}

	public static long getTime(final JSONObject json, final String key) {
		if (!json.has(key)) {
			throw new RuntimeException("no key \"" + key + "\" found in " + json.keySet());
		}
		return getTime(json.getJSONObject(key));
	}

}
