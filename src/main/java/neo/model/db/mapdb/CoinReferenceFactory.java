package neo.model.db.mapdb;

import java.nio.ByteBuffer;

import neo.model.bytes.UInt16;
import neo.model.bytes.UInt256;
import neo.model.core.CoinReference;
import neo.model.util.ModelUtil;

/**
 * an object mapper for transaction inputs.
 *
 * @author coranos
 *
 */
public final class CoinReferenceFactory extends AbstractByteBufferFactory<CoinReference> {

	@Override
	public ByteBuffer fromObject(final CoinReference coinReference) {
		final byte[] ba = ModelUtil.toByteArray(coinReference.prevHash.toByteArray(),
				coinReference.prevIndex.toByteArray());
		return ByteBuffer.wrap(ba);
	}

	@Override
	public CoinReference toObject(final ByteBuffer bb) {
		bb.getLong();
		final byte[] prevHashBa = ModelUtil.getVariableLengthByteArray(bb);
		final byte[] prevIndexBa = ModelUtil.getVariableLengthByteArray(bb);
		final UInt256 prevHash = new UInt256(ByteBuffer.wrap(prevHashBa));
		final UInt16 prevIndex = new UInt16(prevIndexBa);
		final CoinReference coinReference = new CoinReference(prevHash, prevIndex);
		return coinReference;
	}

}
