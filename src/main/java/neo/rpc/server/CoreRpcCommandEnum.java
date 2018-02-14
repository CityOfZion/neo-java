package neo.rpc.server;

import org.json.JSONArray;

/**
 * an enumeration of all the commands that can be sent to the neo core rpc
 * service.
 * <p>
 * https://github.com/neo-project/neo/wiki/API-Reference
 *
 * @author coranos
 *
 */
public enum CoreRpcCommandEnum {
	/** getbestblockhash. */
	GETBESTBLOCKHASH("getbestblockhash"),
	/** getblock. */
	GETBLOCK("getblock"),
	/** getblockcount. */
	GETBLOCKCOUNT("getblockcount"),
	/** getblockhash. */
	GETBLOCKHASH("getblockhash"),
	/** getconnectioncount. */
	GETCONNECTIONCOUNT("getconnectioncount"),
	/** getrawmempool. */
	GETRAWMEMPOOL("getrawmempool"),
	/** getrawtransaction. */
	GETRAWTRANSACTION("getrawtransaction"),
	/** gettxout. */
	GETTXOUT("gettxout"),
	/** sendrawtransaction. */
	SENDRAWTRANSACTION("sendrawtransaction"),
	/** submitblock. */
	SUBMITBLOCK("submitblock"),
	/** getaccountlist. */
	GETACCOUNTLIST("getaccountlist"),
	/** default, unknown. */
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
	public static CoreRpcCommandEnum fromName(final String name) {
		for (final CoreRpcCommandEnum command : values()) {
			if (command != UNKNOWN) {
				if (command.name.equals(name)) {
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
		for (final CoreRpcCommandEnum command : CoreRpcCommandEnum.values()) {
			if (command != UNKNOWN) {
				expectedArray.put(command.getName());
			}
		}
		return expectedArray;
	}

	/**
	 * the command name.
	 */
	private final String name;

	/**
	 * the constructor.
	 *
	 * @param name
	 *            the name to use.
	 */
	CoreRpcCommandEnum(final String name) {
		this.name = name;
	}

	/**
	 * returns the name.
	 *
	 * @return the name.
	 */
	public String getName() {
		return name;
	}
}
