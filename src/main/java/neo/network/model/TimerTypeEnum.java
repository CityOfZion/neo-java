package neo.network.model;

public enum TimerTypeEnum {
	/** verify */
	VERIFY("verify"),
	/** refresh */
	REFRESH("refresh"),
	/** send */
	SEND("send"),
	/** trailing semicolon */
	;

	public static final TimerTypeEnum fromName(final String name) {
		for (final TimerTypeEnum timerType : values()) {
			if (timerType.name.equals(name)) {
				return timerType;
			}
		}
		return null;
	}

	private final String name;

	private TimerTypeEnum(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
