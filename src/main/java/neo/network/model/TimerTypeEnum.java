package neo.network.model;

/**
 * the type of timer.
 *
 * @author coranos
 *
 */
public enum TimerTypeEnum {
	/** verify. */
	VERIFY("verify"),
	/** refresh. */
	REFRESH("refresh"),
	/** send. */
	SEND("send"),
	/** trailing semicolon */
	;

	/**
	 * returns a timer type from a given name.
	 *
	 * @param name
	 *            the name to use.
	 * @return a timer type from a given name.
	 */
	public static TimerTypeEnum fromName(final String name) {
		for (final TimerTypeEnum timerType : values()) {
			if (timerType.name.equals(name)) {
				return timerType;
			}
		}
		return null;
	}

	/**
	 * the timer name.
	 */
	private final String name;

	/**
	 * the timer type.
	 *
	 * @param name
	 *            the name to use.
	 */
	TimerTypeEnum(final String name) {
		this.name = name;
	}

	/**
	 * return the name.
	 *
	 * @return the name.
	 */
	public String getName() {
		return name;
	}
}
