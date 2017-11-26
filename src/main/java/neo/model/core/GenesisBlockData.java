package neo.model.core;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.ArrayUtils;

import neo.model.bytes.UInt256;

public class GenesisBlockData {

	public static final String GENESIS_HASH_HEX_STR = "d42561e3d30e15be6400b6df2f328e02d2bf6354c41dce433bc57687c82144bf";

	public static final UInt256 GENESIS_HASH;

	static {
		try {
			final byte[] bytes = Hex.decodeHex(GENESIS_HASH_HEX_STR.toCharArray());
			ArrayUtils.reverse(bytes);
			GENESIS_HASH = new UInt256(bytes);
		} catch (final DecoderException e) {
			throw new RuntimeException(e);
		}
	}

}
