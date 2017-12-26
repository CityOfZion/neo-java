package neo.network.model;

/**
 * the enumerated connection phases.
 *
 * @author coranos
 *
 */
public enum NodeConnectionPhaseEnum {

	/** node has never been connected. */
	UNKNOWN,
	/** node should try to start a TCP connection. */
	TRY_START,
	/** node has an active TCP connection. */
	ACTIVE,
	/** node has an acknowledged TCP connection. */
	ACKNOWLEDGED,
	/** node refused to create a TCP connection. */
	REFUSED,
	/** node previously had an active TCP connection. */
	INACTIVE,
	/** trailing semicolon */
	;

}
