package neo.vm.contract;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.json.JSONObject;

import neo.model.bytes.UInt160;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;
import neo.vm.ContractPropertyState;
import neo.vm.IInteropInterface;
import neo.vm.StateBase;

/**
 * contract state.
 *
 * @author coranos
 *
 */
public final class ContractState extends StateBase {

	public byte[] Script;

	public ContractParameterType[] ParameterList;

	public ContractParameterType ReturnType;

	public ContractPropertyState ContractProperties;

	public String Name;

	public String CodeVersion;

	public String Author;

	public String Email;

	public String Description;

	@Override
	public int compareTo(final IInteropInterface object) {
		final ContractState that = (ContractState) object;

		final ByteBuffer thisScript = ByteBuffer.wrap(Script);
		final ByteBuffer thatScript = ByteBuffer.wrap(Script);
		final int scriptC = thisScript.compareTo(thatScript);
		if (scriptC != 0) {
			return scriptC;
		}

		final int parameterListC = ModelUtil.compareTo(ParameterList, that.ParameterList);
		if (parameterListC != 0) {
			return parameterListC;
		}

		final int returnTypeC = ReturnType.compareTo(that.ReturnType);
		if (returnTypeC != 0) {
			return returnTypeC;
		}

		final int contractPropertiesC = ContractProperties.compareTo(that.ContractProperties);
		if (contractPropertiesC != 0) {
			return contractPropertiesC;
		}

		final int nameC = Name.compareTo(that.Name);
		if (nameC != 0) {
			return nameC;
		}

		final int codeVersionC = CodeVersion.compareTo(that.CodeVersion);
		if (codeVersionC != 0) {
			return codeVersionC;
		}

		final int authorC = Author.compareTo(that.Author);
		if (authorC != 0) {
			return authorC;
		}

		final int emailC = Email.compareTo(that.Email);
		if (emailC != 0) {
			return emailC;
		}

		final int descriptionC = Description.compareTo(that.Description);
		if (descriptionC != 0) {
			return descriptionC;
		}
		return 0;
	}

	public UInt160 getScriptHash() {
		return ModelUtil.toScriptHash(Script);
	}

	@Override
	public int getSize() {
		return super.getSize() + ModelUtil.getVarSize(Script) + ModelUtil.getVarSize(ParameterList)
				+ ContractParameterType.SIZE + 1 + ModelUtil.getVarSize(Name) + ModelUtil.getVarSize(CodeVersion)
				+ ModelUtil.getVarSize(Author) + ModelUtil.getVarSize(Email) + ModelUtil.getVarSize(Description);
	}

	public boolean hasDynamicInvoke() {
		return ContractProperties.getTypeByte() == ContractPropertyState.HasDynamicInvoke.getTypeByte();
	}

	public boolean hasStorage() {
		return ContractProperties.getTypeByte() == ContractPropertyState.HasStorage.getTypeByte();
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		NetworkUtil.write(bout, Script);
		NetworkUtil.writeArray(bout, ParameterList);
		NetworkUtil.write(bout, ReturnType.toByteArray());
		NetworkUtil.write(bout, ContractProperties.toByteArray());
		NetworkUtil.writeString(bout, Name);
		NetworkUtil.writeString(bout, CodeVersion);
		NetworkUtil.writeString(bout, Author);
		NetworkUtil.writeString(bout, Email);
		NetworkUtil.writeString(bout, Description);
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject object = super.toJSONObject();
		object.put("hash", getScriptHash().toHexString());
		object.put("script", ModelUtil.toHexString(Script));
		object.put("parameters", ModelUtil.toJSONArray(true, Arrays.asList(ParameterList)));
		object.put("returntype", ReturnType.toJSONObject());
		object.put("name", Name);
		object.put("code_version", CodeVersion);
		object.put("author", Author);
		object.put("email", Email);
		object.put("description", Description);

		final JSONObject properties = new JSONObject();
		object.put("properties", properties);

		properties.put("storage", hasStorage());
		properties.put("dynamic_invoke", hasDynamicInvoke());

		return object;
	}
}

// using Neo.IO;
// using Neo.IO.Json;
// using Neo.SmartContract;
// using System.IO;
// using System.Linq;
//
// namespace Neo.Core
// {
// public class ContractState : StateBase, ICloneable<ContractState>
// {
// public byte[] Script;
// public ContractParameterType[] ParameterList;
// public ContractParameterType ReturnType;
// public ContractPropertyState ContractProperties;
// public string Name;
// public string CodeVersion;
// public string Author;
// public string Email;
// public string Description;
//
//
// public bool HasStorage =>
// ContractProperties.HasFlag(ContractPropertyState.HasStorage);
//
// public bool HasDynamicInvoke =>
// ContractProperties.HasFlag(ContractPropertyState.HasDynamicInvoke);
//
// private UInt160 _scriptHash;
// public UInt160 ScriptHash
// {
// get
// {
// if (_scriptHash == null)
// {
// _scriptHash = Script.ToScriptHash();
// }
// return _scriptHash;
// }
// }
//
// public override int Size => base.Size + Script.GetVarSize() +
// ParameterList.GetVarSize() + sizeof(ContractParameterType) + sizeof(bool) +
// Name.GetVarSize() + CodeVersion.GetVarSize() + Author.GetVarSize() +
// Email.GetVarSize() + Description.GetVarSize();
//
// ContractState ICloneable<ContractState>.Clone()
// {
// return new ContractState
// {
// Script = Script,
// ParameterList = ParameterList,
// ReturnType = ReturnType,
// ContractProperties = ContractProperties,
// Name = Name,
// CodeVersion = CodeVersion,
// Author = Author,
// Email = Email,
// Description = Description
// };
// }
//
// public override void Deserialize(BinaryReader reader)
// {
// base.Deserialize(reader);
// Script = reader.ReadVarBytes();
// ParameterList = reader.ReadVarBytes().Select(p =>
// (ContractParameterType)p).ToArray();
// ReturnType = (ContractParameterType)reader.ReadByte();
// ContractProperties = (ContractPropertyState)reader.ReadByte();
// Name = reader.ReadVarString();
// CodeVersion = reader.ReadVarString();
// Author = reader.ReadVarString();
// Email = reader.ReadVarString();
// Description = reader.ReadVarString();
// }
//
// void ICloneable<ContractState>.FromReplica(ContractState replica)
// {
// Script = replica.Script;
// ParameterList = replica.ParameterList;
// ReturnType = replica.ReturnType;
// ContractProperties = replica.ContractProperties;
// Name = replica.Name;
// CodeVersion = replica.CodeVersion;
// Author = replica.Author;
// Email = replica.Email;
// Description = replica.Description;
// }
//
// public override void Serialize(BinaryWriter writer)
// {
// base.Serialize(writer);
// writer.WriteVarBytes(Script);
// writer.WriteVarBytes(ParameterList.Cast<byte>().ToArray());
// writer.Write((byte)ReturnType);
// writer.Write((byte)ContractProperties);
// writer.WriteVarString(Name);
// writer.WriteVarString(CodeVersion);
// writer.WriteVarString(Author);
// writer.WriteVarString(Email);
// writer.WriteVarString(Description);
// }
//
// public override JObject ToJson()
// {
// JObject json = base.ToJson();
// json["hash"] = ScriptHash.ToString();
// json["script"] = Script.ToHexString();
// json["parameters"] = new JArray(ParameterList.Select(p => (JObject)p));
// json["returntype"] = ReturnType;
// json["name"] = Name;
// json["code_version"] = CodeVersion;
// json["author"] = Author;
// json["email"] = Email;
// json["description"] = Description;
// json["properties"] = new JObject();
// json["properties"]["storage"] = HasStorage;
// json["properties"]["dynamic_invoke"] = HasDynamicInvoke;
// return json;
// }
// }
// }
