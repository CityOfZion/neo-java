package neo.model.db.h2;

import java.util.Map;

import neo.model.core.Witness;

/**
 * an object mapper for transaction scripts.
 *
 * @author coranos
 *
 */
public final class WitnessMapToObject extends AbstractMapToObject<Witness> {

	@Override
	public Witness toObject(final Map<String, Object> map) {
		final byte[] invocationBa = getBytes(map, "invocation_script");
		final byte[] verificationBa = getBytes(map, "verification_script");

		final Witness witness = new Witness(invocationBa, verificationBa);
		return witness;
	}

}
