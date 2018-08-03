package neo.vm.contract;

import java.util.Map;

import neo.model.bytes.UInt160;
import neo.vm.IScriptTable;

public final class CachedScriptTable implements IScriptTable {

	private final Map<UInt160, ContractState> contracts;

	public CachedScriptTable(final Map<UInt160, ContractState> contracts) {
		this.contracts = contracts;
	}

	public ContractState getContractState(final byte[] script_hash) {
		return contracts.get(new UInt160(script_hash));
	}

	@Override
	public byte[] getScript(final byte[] script_hash) {
		return getContractState(script_hash).Script;
	}
}

// using Neo.Core;
// using Neo.IO.Caching;
// using Neo.VM;
//
// namespace Neo.SmartContract
// {
// internal class CachedScriptTable : IScriptTable
// {
// private DataCache<UInt160, ContractState> contracts;
//
// public CachedScriptTable(DataCache<UInt160, ContractState> contracts)
// {
// this.contracts = contracts;
// }
//
// byte[] IScriptTable.GetScript(byte[] script_hash)
// {
// return contracts[new UInt160(script_hash)].Script;
// }
//
// public ContractState GetContractState(byte[] script_hash)
// {
// return contracts[new UInt160(script_hash)];
// }
// }
// }
