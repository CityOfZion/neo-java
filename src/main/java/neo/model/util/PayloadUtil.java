package neo.model.util;

import neo.model.network.VersionPayload;

public class PayloadUtil {

	public static VersionPayload getVersionPayload(final int port, final int nonce, final long startHeight) {
		return new VersionPayload(System.currentTimeMillis() / 1000, port, nonce, "/NEO:2.3.4/", startHeight);
	}

}
