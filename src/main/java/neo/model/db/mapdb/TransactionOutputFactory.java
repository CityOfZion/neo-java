package neo.model.db.mapdb;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.ArrayUtils;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.TransactionOutput;
import neo.model.util.ModelUtil;

/**
 * an object mapper for transaction outputs.
 *
 * @author coranos
 *
 */
public final class TransactionOutputFactory extends AbstractByteBufferFactory<TransactionOutput> {

	@Override
	public TransactionOutput toObject(final ByteBuffer bb) {
		final byte[] assetIdBa = ModelUtil.getByteArray(bb);
		final byte[] valueBa = ModelUtil.getByteArray(bb);
		ArrayUtils.reverse(valueBa);
		final byte[] scriptHashBa = ModelUtil.getByteArray(bb);

		final UInt256 assetId = new UInt256(ByteBuffer.wrap(assetIdBa));
		final Fixed8 value = new Fixed8(ByteBuffer.wrap(valueBa));
		final UInt160 scriptHash = new UInt160(scriptHashBa);
		final TransactionOutput transactionOutput = new TransactionOutput(assetId, value, scriptHash);
		return transactionOutput;
	}

}
