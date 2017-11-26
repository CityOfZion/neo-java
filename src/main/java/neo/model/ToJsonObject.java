package neo.model;

import org.json.JSONObject;

/**
 * interface indicating the object can be converted to a JSONObject.
 *
 * @author coranos
 *
 */
public interface ToJsonObject {
	/**
	 * @return a JSONObject representing this object.
	 */
	JSONObject toJSONObject();
}
