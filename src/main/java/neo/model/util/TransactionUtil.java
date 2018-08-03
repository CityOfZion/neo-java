package neo.model.util;

import java.nio.ByteBuffer;

import neo.model.core.ClaimExclusiveData;
import neo.model.core.EnrollmentExclusiveData;
import neo.model.core.ExclusiveData;
import neo.model.core.InvocationExclusiveData;
import neo.model.core.MinerExclusiveData;
import neo.model.core.NoExclusiveData;
import neo.model.core.PublishExclusiveData;
import neo.model.core.RegisterExclusiveData;
import neo.model.core.StateExclusiveData;
import neo.model.core.TransactionType;

/**
 * the utility for transaction helper methods.
 *
 * @author coranos
 *
 */
public final class TransactionUtil {

	/**
	 * deserialize the exclusive data based on the transaction type.
	 *
	 * @param type
	 *            the type of transaction to use.
	 * @param version
	 *            the version to use.
	 * @param bb
	 *            the ByteBuffer to read.
	 *
	 * @return the exclusive data, or throw an exception if the transaction type is
	 *         not recognized.
	 */
	public static ExclusiveData deserializeExclusiveData(final TransactionType type, final byte version,
			final ByteBuffer bb) {
		switch (type) {
		case MINER_TRANSACTION:
			return new MinerExclusiveData(bb);
		case CLAIM_TRANSACTION:
			return new ClaimExclusiveData(bb);
		case CONTRACT_TRANSACTION:
			return new NoExclusiveData(bb);
		case ENROLLMENT_TRANSACTION:
			return new EnrollmentExclusiveData(bb);
		case INVOCATION_TRANSACTION:
			return new InvocationExclusiveData(version, bb);
		case ISSUE_TRANSACTION:
			return new NoExclusiveData(bb);
		case PUBLISH_TRANSACTION:
			return new PublishExclusiveData(version, bb);
		case REGISTER_TRANSACTION:
			return new RegisterExclusiveData(bb);
		case STATE_TRANSACTION:
			return new StateExclusiveData(bb);
		default:
			throw new RuntimeException("unknown type:" + type);
		}
	}

	/**
	 * the constructor.
	 */
	private TransactionUtil() {

	}

}
