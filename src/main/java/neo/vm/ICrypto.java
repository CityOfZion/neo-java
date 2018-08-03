package neo.vm;

/**
 * the cryptographic function interface.
 *
 * @author coranos
 *
 */

public interface ICrypto {

	/**
	 * do a Hash160.
	 *
	 * @param message
	 *            the message to hash.
	 * @return the hash.
	 */
	byte[] hash160(byte[] message);

	/**
	 * do a Hash256.
	 *
	 * @param message
	 *            the message to hash.
	 * @return the hash.
	 */
	byte[] hash256(byte[] message);

	/**
	 * verify the signature.
	 *
	 * @param message
	 *            the message.
	 * @param signature
	 *            the signature.
	 * @param pubkey
	 *            the public key.
	 * @return true if the signature is valid.
	 */
	boolean verifySignature(byte[] message, byte[] signature, byte[] pubkey);
}

// namespace Neo.VM
// {
// public interface ICrypto
// {
// byte[] Hash160(byte[] message);
//
// byte[] Hash256(byte[] message);
//
// bool VerifySignature(byte[] message, byte[] signature, byte[] pubkey);
// }
// }
