package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONArray;
import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.keystore.ByteArraySerializable;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

public class PublishExclusiveData implements ExclusiveData, ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	private final byte version;

	public byte[] script;

	public ContractParameterType[] parameterList;

	public ContractParameterType returnType;

	public boolean needStorage;

	public String name;

	public String codeVersion;

	public String author;

	public String email;

	public String description;

	public PublishExclusiveData(final byte version, final ByteBuffer bb) {
		this.version = version;
		script = ModelUtil.getByteArray(bb);
		parameterList = ContractParameterType.valuesOf(ModelUtil.getByteArray(bb));
		returnType = ContractParameterType.valueOf(ModelUtil.getByte(bb));
		if (version >= 1) {
			needStorage = ModelUtil.getBoolean(bb);
		} else {
			needStorage = false;
		}
		name = ModelUtil.getString(bb);
		codeVersion = ModelUtil.getString(bb);
		author = ModelUtil.getString(bb);
		email = ModelUtil.getString(bb);
		description = ModelUtil.getString(bb);
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			NetworkUtil.writeByteArray(bout, script);
			final byte[] parameterListBa = new byte[parameterList.length];
			for (int ix = 0; ix < parameterList.length; ix++) {
				parameterListBa[ix] = parameterList[ix].getTypeByte();
			}
			NetworkUtil.writeByteArray(bout, parameterListBa);
			bout.write(new byte[] { returnType.getTypeByte() });

			if (version > 1) {
				if (needStorage) {
					bout.write(new byte[] { 1 });
				} else {
					bout.write(new byte[] { 0 });
				}
			}

			NetworkUtil.writeString(bout, name);
			NetworkUtil.writeString(bout, codeVersion);
			NetworkUtil.writeString(bout, author);
			NetworkUtil.writeString(bout, email);
			NetworkUtil.writeString(bout, description);

			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		json.put("script", ModelUtil.toHexString(script));

		final JSONArray parameterListJson = new JSONArray();
		for (final ContractParameterType type : parameterList) {
			parameterListJson.put(type.name());
		}
		json.put("parameterList", parameterListJson);

		json.put("returnType", returnType.name());
		json.put("needStorage", needStorage);
		json.put("name", name);
		json.put("codeVersion", codeVersion);
		json.put("author", author);
		json.put("email", email);
		json.put("description", description);

		return json;
	}

}
