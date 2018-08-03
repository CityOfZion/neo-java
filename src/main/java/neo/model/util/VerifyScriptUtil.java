package neo.model.util;

import neo.model.ScriptVerificationResultEnum;
import neo.model.bytes.UInt160;
import neo.model.core.Transaction;
import neo.model.db.BlockDb;
import neo.vm.ScriptBuilder;
import neo.vm.contract.ApplicationEngine;
import neo.vm.contract.CachedScriptTable;
import neo.vm.contract.ContractState;
import neo.vm.contract.StateReader;
import neo.vm.contract.TriggerType;

/**
 * the utility for verifying scripts.
 *
 * @author coranos
 *
 */
public final class VerifyScriptUtil {

	/**
	 * verify a script's execution.
	 *
	 * @param blockDb
	 *            the blockdb to use.
	 * @param tx
	 *            the transaction to use.
	 * @return true if the script was executed, and did not halt, and left nothing
	 *         on the stack.
	 */
	public static ScriptVerificationResultEnum verifyScripts(final BlockDb blockDb, final Transaction tx) {
		final UInt160[] hashes = tx.getScriptHashesForVerifying(blockDb);

		if (hashes.length != tx.scripts.size()) {
			return ScriptVerificationResultEnum.FAIL_HASH_SCRIPT_COUNT_DIFFERS;
		}
		for (int i = 0; i < hashes.length; i++) {
			byte[] verification = tx.scripts.get(i).getCopyOfVerificationScript();
			if (verification.length == 0) {
				final ScriptBuilder sb = new ScriptBuilder();
				sb.emitAppCall(hashes[i].toByteArray(), false);
				verification = sb.toByteArray();
			} else {
				final UInt160 txScriptHash = tx.scripts.get(i).getScriptHash();
				if (!hashes[i].equals(txScriptHash)) {
					return ScriptVerificationResultEnum.FAIL_HASH_MISMATCH;
				}
			}
			final StateReader service = new StateReader();
			final CachedScriptTable table = new CachedScriptTable(
					blockDb.getStates(UInt160.class, ContractState.class));
			final ApplicationEngine engine = new ApplicationEngine(TriggerType.Verification, tx, table, service,
					ModelUtil.FIXED8_ZERO, false);
			engine.loadScript(verification, false);
			engine.loadScript(tx.scripts.get(i).getCopyOfInvocationScript(), true);
			if (!engine.Execute()) {
				return ScriptVerificationResultEnum.FAIL_ENGINE_EXECUTE;
			}
			if (engine.evaluationStack.getCount() != 1) {
				return ScriptVerificationResultEnum.FAIL_STACK_CONTAINS_MANY;
			}
			if (!engine.evaluationStack.pop().getBoolean()) {
				return ScriptVerificationResultEnum.FAIL_STACK_CONTAINS_FALSE;
			}
		}
		return ScriptVerificationResultEnum.PASS;
	}

	/**
	 * the constructor.
	 */
	private VerifyScriptUtil() {
	}
}
