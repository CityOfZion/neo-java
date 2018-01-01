package neo.model.db.mapdb;

import java.nio.ByteBuffer;

import neo.model.core.Witness;
import neo.model.util.ModelUtil;

/**
 * an object mapper for transaction scripts.
 *
 * @author coranos
 *
 */
public final class WitnessFactory extends AbstractByteBufferFactory<Witness> {

	@Override
	public ByteBuffer fromObject(final Witness witness) {
		final byte[] ba = ModelUtil.toByteArray(witness.getCopyOfInvocationScript(),
				witness.getCopyOfVerificationScript());
		return ByteBuffer.wrap(ba);
	}

	@Override
	public Witness toObject(final ByteBuffer bb) {
		bb.getLong();
		final byte[] invocationBa = ModelUtil.getVariableLengthByteArray(bb);
		final byte[] verificationBa = ModelUtil.getVariableLengthByteArray(bb);
		final Witness witness = new Witness(invocationBa, verificationBa);
		return witness;
	}

}
