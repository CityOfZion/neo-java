package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import neo.model.ByteArraySerializable;
import neo.model.ToJsonObject;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * the transaction attributes.
 *
 * @author coranos
 *
 */
public final class TransactionAttribute implements ToJsonObject, ByteArraySerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * the usage of the transaction attribute.
	 */
	public final TransactionAttributeUsage usage;

	/**
	 * the data for the transaction attribute.
	 */
	private final byte[] data;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public TransactionAttribute(final ByteBuffer bb) {
		usage = TransactionAttributeUsage.valueOf(ModelUtil.getByte(bb));

		switch (usage) {
		case CONTRACT_HASH:
		case VOTE:
		case HASH_01:
		case HASH_02:
		case HASH_03:
		case HASH_04:
		case HASH_05:
		case HASH_06:
		case HASH_07:
		case HASH_08:
		case HASH_09:
		case HASH_10:
		case HASH_11:
		case HASH_12:
		case HASH_13:
		case HASH_14:
		case HASH_15:
			data = ModelUtil.getByteArray(bb, 32, false);
			break;
		case ECDH02:
		case ECDH03: {
			final byte[] readData = ModelUtil.getByteArray(bb, 32, false);
			data = new byte[readData.length + 1];
			System.arraycopy(readData, 0, data, 1, readData.length);
			data[0] = usage.getTypeByte();
			break;
		}
		case SCRIPT:
			data = ModelUtil.getByteArray(bb, 20, false);
			break;
		case DESCRIPTION_URL: {
			final int length = ModelUtil.getByte(bb) & 0xff;
			data = ModelUtil.getByteArray(bb, length, false);
			break;
		}
		case DESCRIPTION:
		case REMARK_00:
		case REMARK_01:
		case REMARK_02:
		case REMARK_03:
		case REMARK_04:
		case REMARK_05:
		case REMARK_06:
		case REMARK_07:
		case REMARK_08:
		case REMARK_09:
		case REMARK_10:
		case REMARK_11:
		case REMARK_12:
		case REMARK_13:
		case REMARK_14:
		case REMARK_15: {
			data = ModelUtil.getByteArray(bb);
			break;
		}
		default:
			throw new RuntimeException("unknown usage:" + usage);
		}
	}

	@Override
	public byte[] toByteArray() {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			bout.write(new byte[] { usage.getTypeByte() });

			switch (usage) {
			case CONTRACT_HASH:
			case VOTE:
			case HASH_01:
			case HASH_02:
			case HASH_03:
			case HASH_04:
			case HASH_05:
			case HASH_06:
			case HASH_07:
			case HASH_08:
			case HASH_09:
			case HASH_10:
			case HASH_11:
			case HASH_12:
			case HASH_13:
			case HASH_14:
			case HASH_15:
				bout.write(data);
				break;
			case ECDH02:
			case ECDH03: {
				bout.write(data, 1, data.length - 1);
				break;
			}
			case SCRIPT:
				bout.write(data);
				break;
			case DESCRIPTION_URL: {
				final byte b = (byte) data.length;
				bout.write(b);
				bout.write(data);
				break;
			}
			case DESCRIPTION:
			case REMARK_00:
			case REMARK_01:
			case REMARK_02:
			case REMARK_03:
			case REMARK_04:
			case REMARK_05:
			case REMARK_06:
			case REMARK_07:
			case REMARK_08:
			case REMARK_09:
			case REMARK_10:
			case REMARK_11:
			case REMARK_12:
			case REMARK_13:
			case REMARK_14:
			case REMARK_15: {
				NetworkUtil.writeByteArray(bout, data);
				break;
			}
			default:
				throw new RuntimeException("unknown usage:" + usage);
			}

			return bout.toByteArray();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();

		json.put("usage", usage);
		json.put("data", ModelUtil.toHexString(data));

		return json;
	}

	@Override
	public String toString() {
		final JSONObject json = new JSONObject();
		json.put("class", getClass().getSimpleName());
		json.put("fields", toJSONObject());
		return json.toString();
	}

}
