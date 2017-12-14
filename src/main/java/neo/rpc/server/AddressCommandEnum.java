package neo.rpc.server;

/**
 * an enumeration of all the commands that can be sent to the neo core rpc
 * service.
 * <p>
 * https://github.com/neo-project/neo/wiki/API-Reference
 *
 * @author coranos
 *
 */
public enum AddressCommandEnum {
	/** address/balance. */
	BALANCE("balance"),
	/** address/history. */
	HISTORY("history"),
	/** address/claims. */
	CLAIMS("claims"),
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
	public static final AddressCommandEnum fromName(final String name) {
		for (final AddressCommandEnum command : values()) {
			if (command.name.equals(name)) {
				return command;
			}
		}
		return null;
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
	AddressCommandEnum(final String name) {
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
