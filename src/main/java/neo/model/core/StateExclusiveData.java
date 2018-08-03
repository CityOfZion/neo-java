package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * exclusive data for Claim transactions.
 *
 * @author coranos
 *
 */
public final class StateExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the list of CoinReferences that represent coin claims.
	 */
	public final List<StateDescriptor> stateDescriptors;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public StateExclusiveData(final ByteBuffer bb) {
		stateDescriptors = ModelUtil.readVariableLengthList(bb, StateDescriptor.class);
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		NetworkUtil.write(bout, stateDescriptors);
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		final boolean ifNullReturnEmpty = false;
		json.put("stateDescriptors", ModelUtil.toJSONArray(ifNullReturnEmpty, stateDescriptors));

		return json;
	}

}
