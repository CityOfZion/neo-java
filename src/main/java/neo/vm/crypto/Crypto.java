package neo.vm.crypto;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import neo.model.crypto.ecc.ECCurve;
import neo.model.crypto.ecc.ECPoint;
import neo.model.util.RIPEMD160HashUtil;
import neo.model.util.SHA256HashUtil;
import neo.vm.ICrypto;

/**
 * the crypto class.
 *
 * @author coranos
 *
 */
public final class Crypto implements ICrypto {

	/**
	 * the string "BC".
	 */
	private static final String BC = "BC";

	/** the default crypto. */
	public static final Crypto Default = new Crypto();

	@Override
	public byte[] hash160(final byte[] message) {
		return RIPEMD160HashUtil.getRIPEMD160Hash(message);
	}

	@Override
	public byte[] hash256(final byte[] message) {
		return SHA256HashUtil.getDoubleSHA256Hash(message);
	}

	@Override
	public boolean verifySignature(final byte[] message, final byte[] sign, final byte[] pubkey) {

		if ((pubkey.length == 33) && ((pubkey[0] == 0x02) || (pubkey[0] == 0x03))) {
			try {
				final byte[] point = ECPoint.DecodePoint(pubkey, ECCurve.Secp256r1).EncodePoint(false);
				System.arraycopy(point, 1, pubkey, 0, pubkey.length);
			} catch (final Exception e) {
				return false;
			}
		} else if ((pubkey.length == 65) && (pubkey[0] == 0x04)) {
			System.arraycopy(pubkey, 1, pubkey, 0, pubkey.length);
		} else if (pubkey.length != 64) {
			throw new RuntimeException("unsupported pubkey length:" + pubkey.length);
		}

		// from https://github.com/rakeb/ECSingVerify/blob/master/src/ECPSingVerify.java
		try {
			final KeyFactory factory = KeyFactory.getInstance("ECDSA", BC);
			final PublicKey ecPublicKey = factory.generatePublic(new X509EncodedKeySpec(pubkey));

			final Signature signature = Signature.getInstance("SHA256withECDSA", BC);
			signature.initVerify(ecPublicKey);
			signature.update(message);

			return signature.verify(sign);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}

// using Neo.VM;
// using System;
// using System.Linq;
// using System.Security.Cryptography;
//
// namespace Neo.Cryptography
// {
// public class Crypto : ICrypto
// {
// public static readonly Crypto Default = new Crypto();
//
// public byte[] Hash160(byte[] message)
// {
// return message.Sha256().RIPEMD160();
// }
//
// public byte[] Hash256(byte[] message)
// {
// return message.Sha256().Sha256();
// }
//
// public byte[] Sign(byte[] message, byte[] prikey, byte[] pubkey)
// {
// using (var ecdsa = ECDsa.Create(new ECParameters
// {
// Curve = ECCurve.NamedCurves.nistP256,
// D = prikey,
// Q = new ECPoint
// {
// X = pubkey.Take(32).ToArray(),
// Y = pubkey.Skip(32).ToArray()
// }
// }))
// {
// return ecdsa.SignData(message, HashAlgorithmName.SHA256);
// }
// }
//
// public bool VerifySignature(byte[] message, byte[] signature, byte[] pubkey)
// {
// if (pubkey.Length == 33 && (pubkey[0] == 0x02 || pubkey[0] == 0x03))
// {
// try
// {
// pubkey = Cryptography.ECC.ECPoint.DecodePoint(pubkey,
// Cryptography.ECC.ECCurve.Secp256r1).EncodePoint(false).Skip(1).ToArray();
// }
// catch
// {
// return false;
// }
// }
// else if (pubkey.Length == 65 && pubkey[0] == 0x04)
// {
// pubkey = pubkey.Skip(1).ToArray();
// }
// else if (pubkey.Length != 64)
// {
// throw new ArgumentException();
// }
// using (var ecdsa = ECDsa.Create(new ECParameters
// {
// Curve = ECCurve.NamedCurves.nistP256,
// Q = new ECPoint
// {
// X = pubkey.Take(32).ToArray(),
// Y = pubkey.Skip(32).ToArray()
// }
// }))
// {
// return ecdsa.VerifyData(message, signature, HashAlgorithmName.SHA256);
// }
// }
// }
// }
