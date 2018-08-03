package neo.vm;

/**
 * the script table.
 *
 * @author coranos
 *
 */
public interface IScriptTable {

	/**
	 * return the script with the given hash.
	 *
	 * @param scripHash
	 *            the script hash to use.
	 * @return the script with the given hash.
	 */
	byte[] getScript(byte[] scripHash);
}

// namespace Neo.VM
// {
// public interface IScriptTable
// {
// byte[] GetScript(byte[] script_hash);
// }
// }
