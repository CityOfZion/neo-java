package neo.rpc.server;

import org.json.JSONArray;

/**
 * an enumeration of all the commands that can be sent to the neo core rpc
 * service.
 * <p>
 * https://github.com/CityOfZion/neon-wallet-db/blob/042d2d00c4fb1a657e2268280c46fb900b4645ce/README.md
 *
 * @author coranos
 *
 */
public enum CityOfZionCommandEnum {
	/** /address/balance/. */
	BALANCE("/address/balance/"),
	/** /address/history/. */
	HISTORY("/address/history/"),
	/** /address/claims/. */
	CLAIMS("/address/claims/"),
	/** /transaction/. */
	TRANSACTION("/transaction/"),
	/** unknown. */
	UNKNOWN(""),
	/** */
	;
	/** trailing semicolon */
	;

	/**
	 * returns a command with th given name.
	 *
	 * @param name
	 *            the name to look up.
	 *
	 * @return the command with the given name, or null if no command exists.
	 */
	public static CityOfZionCommandEnum getCommandStartingWith(final String name) {
		for (final CityOfZionCommandEnum command : values()) {
			if (command != UNKNOWN) {
				if (name.startsWith(command.uriPrefix)) {
					return command;
				}
			}
		}
		return UNKNOWN;
	}

	/**
	 * return the values, as a JSON array.
	 *
	 * @return the values, as a JSON array.
	 */
	public static JSONArray getValuesJSONArray() {
		final JSONArray expectedArray = new JSONArray();
		for (final CityOfZionCommandEnum command : CityOfZionCommandEnum.values()) {
			if (command != UNKNOWN) {
				expectedArray.put(command.getUriPrefix());
			}
		}
		return expectedArray;
	}

	/**
	 * the command name.
	 */
	private final String uriPrefix;

	/**
	 * the constructor.
	 *
	 * @param uriPrefix
	 *            the uriPrefix to use.
	 */
	CityOfZionCommandEnum(final String uriPrefix) {
		this.uriPrefix = uriPrefix;
	}

	/**
	 * returns the name.
	 *
	 * @return the name.
	 */
	public String getUriPrefix() {
		return uriPrefix;
	}
}
