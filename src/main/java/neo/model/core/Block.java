package neo.model.core;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.json.JSONObject;

import neo.model.ToJsonObject;
import neo.model.network.Payload;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * a block of transactions.
 *
 * @author coranos
 *
 */
public final class Block extends AbstractBlockBase implements ToJsonObject, Payload {

	private static final long serialVersionUID = 1L;

	/**
	 * the list of transactions.
	 */
	private final List<Transaction> transactionList;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the ByteBuffer to read.
	 */
	public Block(final ByteBuffer bb) {
		super(bb);
		transactionList = ModelUtil.readVariableLengthList(bb, Transaction.class);
	}

	/**
	 * return the transaction list.
	 *
	 * @return the transaction list.
	 */
	public List<Transaction> getTransactionList() {
		return transactionList;
	}

	@Override
	public byte[] toByteArray() {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		writeBaseToOutputStream(bout);
		NetworkUtil.write(bout, transactionList);
		return bout.toByteArray();
	}

	@Override
	public JSONObject toJSONObject() {
		final JSONObject json = new JSONObject();
		final boolean ifNullReturnEmpty = false;
		addBaseToJSONObject(json);
		json.put("tx", ModelUtil.toJSONArray(ifNullReturnEmpty, transactionList));
		return json;
	}

	@Override
	public String toString() {
		return toJSONObject().toString();
	}

}
