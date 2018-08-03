package neo.vm.contract;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.bytes.UInt32;
import neo.model.core.Block;
import neo.model.db.BlockDb;
import neo.model.util.ModelUtil;
import neo.vm.AbstractStackItem;
import neo.vm.ContractPropertyState;
import neo.vm.ExecutionEngine;
import neo.vm.IScriptContainer;
import neo.vm.IScriptTable;
import neo.vm.InteropService;
import neo.vm.OpCode;
import neo.vm.VMState;
import neo.vm.crypto.Crypto;
import neo.vm.types.ArrayStackItem;

public final class ApplicationEngine extends ExecutionEngine {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationEngine.class);

	private static final int MaxSizeForBigInteger = 32;

	/** the max Stack Size. */
	private static final int MaxStackSize = 2 * 1024;

	/** Max Item Size. */
	private static final int MaxItemSize = 1024 * 1024;

	/** Max Invocation Stack Size. */
	private static final int MaxInvocationStackSize = 1024;

	/** Max Array Size. */
	private static final int MaxArraySize = 1024;

	private static final long ratio = 100000;

	private static final long gas_free = 10 * 100000000;

	public static ApplicationEngine Run(final BlockDb blockDb, final byte[] script, final IScriptContainer container,
			final Block persisting_block) {
		final Map<UInt160, AccountState> accounts = blockDb.getStates(UInt160.class, AccountState.class);
		final Map<UInt256, AssetState> assets = blockDb.getStates(UInt256.class, AssetState.class);
		final Map<UInt160, ContractState> contracts = blockDb.getStates(UInt160.class, ContractState.class);
		final Map<StorageKey, StorageItem> storages = blockDb.getStates(StorageKey.class, StorageItem.class);
		final CachedScriptTable script_table = new CachedScriptTable(contracts);

		final StateMachine service = new StateMachine(persisting_block, accounts, assets, contracts, storages);
		final ApplicationEngine engine = new ApplicationEngine(TriggerType.Application, container, script_table,
				service, ModelUtil.FIXED8_ZERO, true);
		engine.loadScript(script, false);
		engine.Execute();
		return engine;
	}

	private final long gas_amount;

	private long gas_consumed = 0;

	private final boolean testMode;

	private final CachedScriptTable script_table;

	private final TriggerType trigger;

	public ApplicationEngine(final TriggerType trigger, final IScriptContainer container, final IScriptTable table,
			final InteropService service, final Fixed8 gas, final boolean testMode) {
		super(container, Crypto.Default, table, service);
		gas_amount = gas_free + gas.value;
		this.testMode = testMode;
		this.trigger = trigger;
		if (table instanceof CachedScriptTable) {
			script_table = (CachedScriptTable) table;
		} else {
			script_table = null;
		}
	}

	private boolean CheckArraySize(final OpCode nextInstruction) {
		int size;
		switch (nextInstruction) {
		case PACK:
		case NEWARRAY:
		case NEWSTRUCT: {
			if (evaluationStack.getCount() == 0) {
				return false;
			}
			size = evaluationStack.peek().getBigInteger().intValue();
			break;
		}
		case SETITEM: {
			if (evaluationStack.getCount() < 3) {
				return false;
			}
			if (!(evaluationStack.peek(2) instanceof Map)) {
				return true;
			} else {
				// TODO: debug setitem.
				final Map<AbstractStackItem, Object> map = (Map<AbstractStackItem, Object>) evaluationStack.peek(2);
				final AbstractStackItem key = evaluationStack.peek(1);
				if (key instanceof Collection) {
					return false;
				}
				if (map.containsKey(key)) {
					return true;
				}
				size = map.size() + 1;
			}
		}

		case APPEND: {
			if (evaluationStack.getCount() < 2) {
				return false;
			}
			if (!(evaluationStack.peek(1) instanceof ArrayStackItem)) {
				return false;
			} else {
				final ArrayStackItem array = (ArrayStackItem) evaluationStack.peek(1);
				size = array.getArray().size() + 1;
			}
			break;
		}
		default:
			return true;
		}
		return size <= MaxArraySize;
	}

	/// <summary>
	/// Check if the BigInteger is allowed for numeric operations
	/// </summary>
	/// <param name="value">Value</param>
	/// <returns>Return True if are allowed, otherwise False</returns>
	private boolean CheckBigInteger(final BigInteger value) {
		return value == null ? false : value.toByteArray().length <= MaxSizeForBigInteger;
	}

	/// <summary>
	/// Check if the BigInteger is allowed for numeric operations
	/// </summary>
	private boolean CheckBigIntegers(final OpCode nextInstruction) {
		switch (nextInstruction) {
		case INC: {
			final BigInteger x = evaluationStack.peek().getBigInteger();

			if (!CheckBigInteger(x) || !CheckBigInteger(x.add(BigInteger.ONE))) {
				return false;
			}

			break;
		}
		case DEC: {
			final BigInteger x = evaluationStack.peek().getBigInteger();

			if (!CheckBigInteger(x) || ((x.signum() <= 0) && !CheckBigInteger(x.subtract(BigInteger.ONE)))) {
				return false;
			}

			break;
		}
		case ADD: {
			final BigInteger x2 = evaluationStack.peek().getBigInteger();
			final BigInteger x1 = evaluationStack.peek(1).getBigInteger();

			if (!CheckBigInteger(x2) || !CheckBigInteger(x1) || !CheckBigInteger(x1.add(x2))) {
				return false;
			}

			break;
		}
		case SUB: {
			final BigInteger x2 = evaluationStack.peek().getBigInteger();
			final BigInteger x1 = evaluationStack.peek(1).getBigInteger();

			if (!CheckBigInteger(x2) || !CheckBigInteger(x1) || !CheckBigInteger(x1.subtract(x2))) {
				return false;
			}

			break;
		}
		case MUL: {
			final BigInteger x2 = evaluationStack.peek().getBigInteger();
			final BigInteger x1 = evaluationStack.peek(1).getBigInteger();

			final int lx1 = x1 == null ? 0 : x1.toByteArray().length;

			if (lx1 > MaxSizeForBigInteger) {
				return false;
			}

			final int lx2 = x2 == null ? 0 : x2.toByteArray().length;

			if ((lx1 + lx2) > MaxSizeForBigInteger) {
				return false;
			}

			break;
		}
		case DIV: {
			final BigInteger x2 = evaluationStack.peek().getBigInteger();
			final BigInteger x1 = evaluationStack.peek(1).getBigInteger();

			if (!CheckBigInteger(x2) || !CheckBigInteger(x1)) {
				return false;
			}

			break;
		}
		case MOD: {
			final BigInteger x2 = evaluationStack.peek().getBigInteger();
			final BigInteger x1 = evaluationStack.peek(1).getBigInteger();

			if (!CheckBigInteger(x2) || !CheckBigInteger(x1)) {
				return false;
			}

			break;
		}
		default:
		}

		return true;
	}

	private boolean CheckDynamicInvoke(final OpCode nextInstruction) {
		if ((nextInstruction == OpCode.APPCALL) || (nextInstruction == OpCode.TAILCALL)) {
			for (int i = getCurrentContext().getInstructionPointer()
					+ 1; i < (getCurrentContext().getInstructionPointer() + 21); i++) {
				if (getCurrentContext().getScript()[i] != 0) {
					return true;
				}
			}
			// if we get this far it is a dynamic call
			// now look at the current executing script
			// to determine if it can do dynamic calls
			final ContractState contract = script_table.getContractState(getCurrentContext().getScriptHash());
			return contract.hasDynamicInvoke();
		}
		return true;
	}

	private boolean CheckInvocationStack(final OpCode nextInstruction) {
		switch (nextInstruction) {
		case CALL:
		case APPCALL:
			if (invocationStack.getCount() >= MaxInvocationStackSize) {
				return false;
			}
			return true;
		default:
			return true;
		}
	}

	private boolean CheckItemSize(final OpCode nextInstruction) {
		switch (nextInstruction) {
		case PUSHDATA4: {
			if ((getCurrentContext().getInstructionPointer() + 4) >= getCurrentContext().getScript().length) {
				return false;
			}
			final byte[] ba = new byte[UInt32.SIZE];
			System.arraycopy(getCurrentContext().getScript(), getCurrentContext().getInstructionPointer() + 1, ba, 0,
					ba.length);
			final long length = ModelUtil.getUInt32(ByteBuffer.wrap(ba)).asLong();
			if (length > MaxItemSize) {
				return false;
			}
			return true;
		}
		case CAT: {
			if (evaluationStack.getCount() < 2) {
				return false;
			}
			final int length = evaluationStack.peek(0).getByteArray().length
					+ evaluationStack.peek(1).getByteArray().length;
			if (length > MaxItemSize) {
				return false;
			}
			return true;
		}
		default:
			return true;
		}
	}

	private boolean CheckStackSize(final OpCode nextInstruction) {
		int size = 0;
		if (nextInstruction.ordinal() <= OpCode.PUSH16.ordinal()) {
			size = 1;
		} else {
			switch (nextInstruction) {
			case DEPTH:
			case DUP:
			case OVER:
			case TUCK:
			case NEWMAP:
				size = 1;
				break;
			case UNPACK:
				final AbstractStackItem item = evaluationStack.peek();
				if (item instanceof ArrayStackItem) {
					final ArrayStackItem array = (ArrayStackItem) item;
					size = array.getCount();
				} else {
					return false;
				}
			}
		}
		if (size == 0) {
			return true;
		}
		size += evaluationStack.getCount() + altStack.getCount();
		if (size > MaxStackSize) {
			return false;
		}
		return true;
	}

	@Override
	public boolean Execute() {
		try {
			while (!getState().contains(VMState.HALT) && !getState().contains(VMState.FAULT)) {
				if (getCurrentContext().getInstructionPointer() < getCurrentContext().getScript().length) {
					final OpCode nextOpcode = getCurrentContext().getNextInstruction();

					final BigInteger priceBi = BigInteger.valueOf(GetPrice(nextOpcode));
					final BigInteger totlPriceBi = priceBi.multiply(BigInteger.valueOf(ratio));
					final BigInteger gas_consumed_bi = BigInteger.valueOf(gas_consumed).add(totlPriceBi);
					gas_consumed = gas_consumed_bi.longValueExact();
					if (!testMode && (gas_consumed > gas_amount)) {
						getState().add(VMState.FAULT);
						return false;
					}

					if (!CheckItemSize(nextOpcode) || !CheckStackSize(nextOpcode) || !CheckArraySize(nextOpcode)
							|| !CheckInvocationStack(nextOpcode) || !CheckBigIntegers(nextOpcode)
							|| !CheckDynamicInvoke(nextOpcode)) {
						getState().add(VMState.FAULT);
						return false;
					}
				}
				StepInto();
			}
		} catch (final Exception e) {
			LOG.error("exception halted execution", e);
			getState().add(VMState.FAULT);
			return false;
		}
		return !getState().contains(VMState.FAULT);
	}

	public Fixed8 getGasConsumed() {
		return ModelUtil.getFixed8(BigInteger.valueOf(gas_consumed));
	}

	private long GetPrice(final OpCode nextInstruction) {
		if (nextInstruction.ordinal() <= OpCode.PUSH16.ordinal()) {
			return 0;
		}
		switch (nextInstruction) {
		case NOP:
			return 0;
		case APPCALL:
		case TAILCALL:
			return 10;
		case SYSCALL:
			return GetPriceForSysCall();
		case SHA1:
		case SHA256:
			return 10;
		case HASH160:
		case HASH256:
			return 20;
		case CHECKSIG:
			return 100;
		case CHECKMULTISIG: {
			if (evaluationStack.getCount() == 0) {
				return 1;
			}
			final int n = evaluationStack.peek().getBigInteger().intValue();
			if (n < 1) {
				return 1;
			}
			return 100 * n;
		}
		default:
			return 1;
		}
	}

	private long GetPriceForSysCall() {
		if (getCurrentContext().getInstructionPointer() >= (getCurrentContext().getScript().length - 3)) {
			return 1;
		}
		final byte length = getCurrentContext().getScript()[getCurrentContext().getInstructionPointer() + 1];
		if (getCurrentContext().getInstructionPointer() > (getCurrentContext().getScript().length - length - 2)) {
			return 1;
		}
		final String api_name = new String(getCurrentContext().getScript(),
				getCurrentContext().getInstructionPointer() + 2, length, Charset.forName("ASCII"));
		switch (api_name) {
		case "Neo.Runtime.CheckWitness":
		case "AntShares.Runtime.CheckWitness":
			return 200;
		case "Neo.Blockchain.GetHeader":
		case "AntShares.Blockchain.GetHeader":
			return 100;
		case "Neo.Blockchain.GetBlock":
		case "AntShares.Blockchain.GetBlock":
			return 200;
		case "Neo.Blockchain.GetTransaction":
		case "AntShares.Blockchain.GetTransaction":
			return 100;
		case "Neo.Blockchain.GetAccount":
		case "AntShares.Blockchain.GetAccount":
			return 100;
		case "Neo.Blockchain.GetValidators":
		case "AntShares.Blockchain.GetValidators":
			return 200;
		case "Neo.Blockchain.GetAsset":
		case "AntShares.Blockchain.GetAsset":
			return 100;
		case "Neo.Blockchain.GetContract":
		case "AntShares.Blockchain.GetContract":
			return 100;
		case "Neo.Transaction.GetReferences":
		case "AntShares.Transaction.GetReferences":
		case "Neo.Transaction.GetUnspentCoins":
			return 200;
		case "Neo.Account.SetVotes":
		case "AntShares.Account.SetVotes":
			return 1000;
		case "Neo.Validator.Register":
		case "AntShares.Validator.Register":
			return (1000L * 100000000L) / ratio;
		case "Neo.Asset.Create":
		case "AntShares.Asset.Create":
			return (5000L * 100000000L) / ratio;
		case "Neo.Asset.Renew":
		case "AntShares.Asset.Renew":
			return ((byte) evaluationStack.peek(1).getBigInteger().longValue() * 5000L * 100000000L) / ratio;
		case "Neo.Contract.Create":
		case "Neo.Contract.Migrate":
		case "AntShares.Contract.Create":
		case "AntShares.Contract.Migrate":
			long fee = 100L;
			final ContractPropertyState contract_properties = ContractPropertyState
					.valueOfByte(evaluationStack.peek(3).getBigInteger().byteValue());
			if (contract_properties.HasFlag(ContractPropertyState.HasStorage)) {
				fee += 400L;
			}
			if (contract_properties.HasFlag(ContractPropertyState.HasDynamicInvoke)) {
				fee += 500L;
			}
			return (fee * 100000000L) / ratio;
		case "Neo.Storage.Get":
		case "AntShares.Storage.Get":
			return 100;
		case "Neo.Storage.Put":
		case "AntShares.Storage.Put":
			return ((((evaluationStack.peek(1).getByteArray().length + evaluationStack.peek(2).getByteArray().length)
					- 1) / 1024) + 1) * 1000;
		case "Neo.Storage.Delete":
		case "AntShares.Storage.Delete":
			return 100;
		default:
			return 1;
		}
	}

	public TriggerType getTrigger() {
		return trigger;
	}
}
// TODO: finish this.
// using Neo.Core;
// using Neo.IO.Caching;
// using Neo.VM;
// using Neo.VM.Types;
// using System.Collections;
// using System.Numerics;
// using System.Text;
//
// namespace Neo.SmartContract
// {
// public class ApplicationEngine : ExecutionEngine
// {
// #region Limits
// /// <summary>
// /// Set the max size allowed size for BigInteger
// /// </summary>
// private const int MaxSizeForBigInteger = 32;
// /// <summary>
// /// Set the max Stack Size
// /// </summary>
// private const uint MaxStackSize = 2 * 1024;
// /// <summary>
// /// Set Max Item Size
// /// </summary>
// private const uint MaxItemSize = 1024 * 1024;
// /// <summary>
// /// Set Max Invocation Stack Size
// /// </summary>
// private const uint MaxInvocationStackSize = 1024;
// /// <summary>
// /// Set Max Array Size
// /// </summary>
// private const uint MaxArraySize = 1024;
// #endregion
//
// private const long ratio = 100000;
// private const long gas_free = 10 * 100000000;
// private readonly long gas_amount;
// private long gas_consumed = 0;
// private readonly bool testMode;
//
// private readonly CachedScriptTable script_table;
//
// public TriggerType Trigger { get; }
// public Fixed8 GasConsumed => new Fixed8(gas_consumed);
//
// public ApplicationEngine(TriggerType trigger, IScriptContainer container,
// IScriptTable table, InteropService service, Fixed8 gas, bool testMode =
// false)
// : base(container, Cryptography.Crypto.Default, table, service)
// {
// this.gas_amount = gas_free + gas.GetData();
// this.testMode = testMode;
// this.Trigger = trigger;
// if (table is CachedScriptTable)
// {
// this.script_table = (CachedScriptTable)table;
// }
// }
//
// private bool CheckArraySize(OpCode nextInstruction)
// {
// int size;
// switch (nextInstruction)
// {
// case OpCode.PACK:
// case OpCode.NEWARRAY:
// case OpCode.NEWSTRUCT:
// {
// if (EvaluationStack.Count == 0) return false;
// size = (int)EvaluationStack.Peek().GetBigInteger();
// }
// break;
// case OpCode.SETITEM:
// {
// if (EvaluationStack.Count < 3) return false;
// if (!(EvaluationStack.Peek(2) is Map map)) return true;
// StackItem key = EvaluationStack.Peek(1);
// if (key is ICollection) return false;
// if (map.ContainsKey(key)) return true;
// size = map.Count + 1;
// }
// break;
// case OpCode.APPEND:
// {
// if (EvaluationStack.Count < 2) return false;
// if (!(EvaluationStack.Peek(1) is Array array)) return false;
// size = array.Count + 1;
// }
// break;
// default:
// return true;
// }
// return size <= MaxArraySize;
// }
//
// private bool CheckInvocationStack(OpCode nextInstruction)
// {
// switch (nextInstruction)
// {
// case OpCode.CALL:
// case OpCode.APPCALL:
// if (InvocationStack.Count >= MaxInvocationStackSize) return false;
// return true;
// default:
// return true;
// }
// }
//
// private bool CheckItemSize(OpCode nextInstruction)
// {
// switch (nextInstruction)
// {
// case OpCode.PUSHDATA4:
// {
// if (CurrentContext.InstructionPointer + 4 >= CurrentContext.Script.Length)
// return false;
// uint length =
// CurrentContext.Script.ToUInt32(CurrentContext.InstructionPointer + 1);
// if (length > MaxItemSize) return false;
// return true;
// }
// case OpCode.CAT:
// {
// if (EvaluationStack.Count < 2) return false;
// int length = EvaluationStack.Peek(0).GetByteArray().Length +
// EvaluationStack.Peek(1).GetByteArray().Length;
// if (length > MaxItemSize) return false;
// return true;
// }
// default:
// return true;
// }
// }
//
// /// <summary>
// /// Check if the BigInteger is allowed for numeric operations
// /// </summary>
// /// <param name="value">Value</param>
// /// <returns>Return True if are allowed, otherwise False</returns>
// private bool CheckBigInteger(BigInteger value)
// {
// return value == null ? false :
// value.ToByteArray().Length <= MaxSizeForBigInteger;
// }
//
// /// <summary>
// /// Check if the BigInteger is allowed for numeric operations
// /// </summary>
// private bool CheckBigIntegers(OpCode nextInstruction)
// {
// switch (nextInstruction)
// {
// case OpCode.INC:
// {
// BigInteger x = EvaluationStack.Peek().GetBigInteger();
//
// if (!CheckBigInteger(x) || !CheckBigInteger(x + 1))
// return false;
//
// break;
// }
// case OpCode.DEC:
// {
// BigInteger x = EvaluationStack.Peek().GetBigInteger();
//
// if (!CheckBigInteger(x) || (x.Sign <= 0 && !CheckBigInteger(x - 1)))
// return false;
//
// break;
// }
// case OpCode.ADD:
// {
// BigInteger x2 = EvaluationStack.Peek().GetBigInteger();
// BigInteger x1 = EvaluationStack.Peek(1).GetBigInteger();
//
// if (!CheckBigInteger(x2) || !CheckBigInteger(x1) || !CheckBigInteger(x1 +
// x2))
// return false;
//
// break;
// }
// case OpCode.SUB:
// {
// BigInteger x2 = EvaluationStack.Peek().GetBigInteger();
// BigInteger x1 = EvaluationStack.Peek(1).GetBigInteger();
//
// if (!CheckBigInteger(x2) || !CheckBigInteger(x1) || !CheckBigInteger(x1 -
// x2))
// return false;
//
// break;
// }
// case OpCode.MUL:
// {
// BigInteger x2 = EvaluationStack.Peek().GetBigInteger();
// BigInteger x1 = EvaluationStack.Peek(1).GetBigInteger();
//
// int lx1 = x1 == null ? 0 : x1.ToByteArray().Length;
//
// if (lx1 > MaxSizeForBigInteger)
// return false;
//
// int lx2 = x2 == null ? 0 : x2.ToByteArray().Length;
//
// if ((lx1 + lx2) > MaxSizeForBigInteger)
// return false;
//
// break;
// }
// case OpCode.DIV:
// {
// BigInteger x2 = EvaluationStack.Peek().GetBigInteger();
// BigInteger x1 = EvaluationStack.Peek(1).GetBigInteger();
//
// if (!CheckBigInteger(x2) || !CheckBigInteger(x1))
// return false;
//
// break;
// }
// case OpCode.MOD:
// {
// BigInteger x2 = EvaluationStack.Peek().GetBigInteger();
// BigInteger x1 = EvaluationStack.Peek(1).GetBigInteger();
//
// if (!CheckBigInteger(x2) || !CheckBigInteger(x1))
// return false;
//
// break;
// }
// }
//
// return true;
// }
//
// private bool CheckStackSize(OpCode nextInstruction)
// {
// int size = 0;
// if (nextInstruction <= OpCode.PUSH16)
// size = 1;
// else
// switch (nextInstruction)
// {
// case OpCode.DEPTH:
// case OpCode.DUP:
// case OpCode.OVER:
// case OpCode.TUCK:
// case OpCode.NEWMAP:
// size = 1;
// break;
// case OpCode.UNPACK:
// StackItem item = EvaluationStack.Peek();
// if (item is Array array)
// size = array.Count;
// else
// return false;
// break;
// }
// if (size == 0) return true;
// size += EvaluationStack.Count + AltStack.Count;
// if (size > MaxStackSize) return false;
// return true;
// }
//
// private bool CheckDynamicInvoke(OpCode nextInstruction)
// {
// if (nextInstruction == OpCode.APPCALL || nextInstruction == OpCode.TAILCALL)
// {
// for (int i = CurrentContext.InstructionPointer + 1; i <
// CurrentContext.InstructionPointer + 21; i++)
// {
// if (CurrentContext.Script[i] != 0) return true;
// }
// // if we get this far it is a dynamic call
// // now look at the current executing script
// // to determine if it can do dynamic calls
// ContractState contract =
// script_table.GetContractState(CurrentContext.ScriptHash);
// return contract.HasDynamicInvoke;
// }
// return true;
// }
//
// public new bool Execute()
// {
// try
// {
// while (!State.HasFlag(VMState.HALT) && !State.HasFlag(VMState.FAULT))
// {
// if (CurrentContext.InstructionPointer < CurrentContext.Script.Length)
// {
// OpCode nextOpcode = CurrentContext.NextInstruction;
//
// gas_consumed = checked(gas_consumed + GetPrice(nextOpcode) * ratio);
// if (!testMode && gas_consumed > gas_amount)
// {
// State |= VMState.FAULT;
// return false;
// }
//
// if (!CheckItemSize(nextOpcode) ||
// !CheckStackSize(nextOpcode) ||
// !CheckArraySize(nextOpcode) ||
// !CheckInvocationStack(nextOpcode) ||
// !CheckBigIntegers(nextOpcode) ||
// !CheckDynamicInvoke(nextOpcode))
// {
// State |= VMState.FAULT;
// return false;
// }
// }
// StepInto();
// }
// }
// catch
// {
// State |= VMState.FAULT;
// return false;
// }
// return !State.HasFlag(VMState.FAULT);
// }
//
// protected virtual long GetPrice(OpCode nextInstruction)
// {
// if (nextInstruction <= OpCode.PUSH16) return 0;
// switch (nextInstruction)
// {
// case OpCode.NOP:
// return 0;
// case OpCode.APPCALL:
// case OpCode.TAILCALL:
// return 10;
// case OpCode.SYSCALL:
// return GetPriceForSysCall();
// case OpCode.SHA1:
// case OpCode.SHA256:
// return 10;
// case OpCode.HASH160:
// case OpCode.HASH256:
// return 20;
// case OpCode.CHECKSIG:
// return 100;
// case OpCode.CHECKMULTISIG:
// {
// if (EvaluationStack.Count == 0) return 1;
// int n = (int)EvaluationStack.Peek().GetBigInteger();
// if (n < 1) return 1;
// return 100 * n;
// }
// default: return 1;
// }
// }
//
// protected virtual long GetPriceForSysCall()
// {
// if (CurrentContext.InstructionPointer >= CurrentContext.Script.Length - 3)
// return 1;
// byte length = CurrentContext.Script[CurrentContext.InstructionPointer + 1];
// if (CurrentContext.InstructionPointer > CurrentContext.Script.Length - length
// - 2)
// return 1;
// string api_name = Encoding.ASCII.GetString(CurrentContext.Script,
// CurrentContext.InstructionPointer + 2, length);
// switch (api_name)
// {
// case "Neo.Runtime.CheckWitness":
// case "AntShares.Runtime.CheckWitness":
// return 200;
// case "Neo.Blockchain.GetHeader":
// case "AntShares.Blockchain.GetHeader":
// return 100;
// case "Neo.Blockchain.GetBlock":
// case "AntShares.Blockchain.GetBlock":
// return 200;
// case "Neo.Blockchain.GetTransaction":
// case "AntShares.Blockchain.GetTransaction":
// return 100;
// case "Neo.Blockchain.GetAccount":
// case "AntShares.Blockchain.GetAccount":
// return 100;
// case "Neo.Blockchain.GetValidators":
// case "AntShares.Blockchain.GetValidators":
// return 200;
// case "Neo.Blockchain.GetAsset":
// case "AntShares.Blockchain.GetAsset":
// return 100;
// case "Neo.Blockchain.GetContract":
// case "AntShares.Blockchain.GetContract":
// return 100;
// case "Neo.Transaction.GetReferences":
// case "AntShares.Transaction.GetReferences":
// case "Neo.Transaction.GetUnspentCoins":
// return 200;
// case "Neo.Account.SetVotes":
// case "AntShares.Account.SetVotes":
// return 1000;
// case "Neo.Validator.Register":
// case "AntShares.Validator.Register":
// return 1000L * 100000000L / ratio;
// case "Neo.Asset.Create":
// case "AntShares.Asset.Create":
// return 5000L * 100000000L / ratio;
// case "Neo.Asset.Renew":
// case "AntShares.Asset.Renew":
// return (byte)EvaluationStack.Peek(1).GetBigInteger() * 5000L * 100000000L /
// ratio;
// case "Neo.Contract.Create":
// case "Neo.Contract.Migrate":
// case "AntShares.Contract.Create":
// case "AntShares.Contract.Migrate":
// long fee = 100L;
//
// ContractPropertyState contract_properties =
// (ContractPropertyState)(byte)EvaluationStack.Peek(3).GetBigInteger();
//
// if (contract_properties.HasFlag(ContractPropertyState.HasStorage))
// {
// fee += 400L;
// }
// if (contract_properties.HasFlag(ContractPropertyState.HasDynamicInvoke))
// {
// fee += 500L;
// }
// return fee * 100000000L / ratio;
// case "Neo.Storage.Get":
// case "AntShares.Storage.Get":
// return 100;
// case "Neo.Storage.Put":
// case "AntShares.Storage.Put":
// return ((EvaluationStack.Peek(1).GetByteArray().Length +
// EvaluationStack.Peek(2).GetByteArray().Length - 1) / 1024 + 1) * 1000;
// case "Neo.Storage.Delete":
// case "AntShares.Storage.Delete":
// return 100;
// default:
// return 1;
// }
// }
//
// public static ApplicationEngine Run(byte[] script, IScriptContainer container
// = null, Block persisting_block = null)
// {
// if (persisting_block == null)
// persisting_block = new Block
// {
// Version = 0,
// PrevHash = Blockchain.Default.CurrentBlockHash,
// MerkleRoot = new UInt256(),
// Timestamp = Blockchain.Default.GetHeader(Blockchain.Default.Height).Timestamp
// + Blockchain.SecondsPerBlock,
// Index = Blockchain.Default.Height + 1,
// ConsensusData = 0,
// NextConsensus =
// Blockchain.Default.GetHeader(Blockchain.Default.Height).NextConsensus,
// Script = new Witness
// {
// InvocationScript = new byte[0],
// VerificationScript = new byte[0]
// },
// Transactions = new Transaction[0]
// };
// DataCache<UInt160, AccountState> accounts =
// Blockchain.Default.GetStates<UInt160, AccountState>();
// DataCache<UInt256, AssetState> assets = Blockchain.Default.GetStates<UInt256,
// AssetState>();
// DataCache<UInt160, ContractState> contracts =
// Blockchain.Default.GetStates<UInt160, ContractState>();
// DataCache<StorageKey, StorageItem> storages =
// Blockchain.Default.GetStates<StorageKey, StorageItem>();
// CachedScriptTable script_table = new CachedScriptTable(contracts);
// using (StateMachine service = new StateMachine(persisting_block, accounts,
// assets, contracts, storages))
// {
// ApplicationEngine engine = new ApplicationEngine(TriggerType.Application,
// container, script_table, service, Fixed8.Zero, true);
// engine.LoadScript(script, false);
// engine.Execute();
// return engine;
// }
// }
// }
// }
