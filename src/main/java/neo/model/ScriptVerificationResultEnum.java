package neo.model;

/**
 * the results of script verification.
 *
 * @author coranos
 *
 */
public enum ScriptVerificationResultEnum {

	/** pass. */
	PASS,
	/** fail, hash count and script count differ. */
	FAIL_HASH_SCRIPT_COUNT_DIFFERS,
	/** fail, engine execution. */
	FAIL_ENGINE_EXECUTE,
	/** fail, stack count is over 1. */
	FAIL_STACK_CONTAINS_MANY,
	/** fail, stack result is not true. */
	FAIL_STACK_CONTAINS_FALSE,
	/** fail, hash does not match script hash. */
	FAIL_HASH_MISMATCH,
	/** ending semicolon */
	;
}
