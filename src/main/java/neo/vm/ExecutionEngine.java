package neo.vm;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import neo.model.util.ModelUtil;

/**
 * the execution engine.
 *
 * @author coranos
 *
 */
public class ExecutionEngine {

	/** the table. */
	private final IScriptTable table;

	/** the service. */
	private final InteropService service;

	/**
	 * the crypto.
	 */
	private final ICrypto crypto;

	/**
	 * the script container.
	 */
	private final IScriptContainer scriptContainer;

	/**
	 * the evaluation stack.
	 */
	public final RandomAccessStack<ExecutionContext> invocationStack = new RandomAccessStack<>();

	/**
	 * the evaluation stack.
	 */
	public final RandomAccessStack<AbstractStackItem> evaluationStack = new RandomAccessStack<>();

	/**
	 * the alt stack.
	 */
	public final RandomAccessStack<AbstractStackItem> altStack = new RandomAccessStack<>();

	/**
	 * the state.
	 */
	private final Set<VMState> State = EnumSet.of(VMState.BREAK);

	/**
	 * the constructor.
	 *
	 * @param container
	 *            the container to use.
	 * @param crypto
	 *            the crypto to use.
	 * @param table
	 *            the table to use.
	 * @param service
	 *            the service to use.
	 */
	public ExecutionEngine(final IScriptContainer container, final ICrypto crypto, final IScriptTable table,
			final InteropService service) {
		scriptContainer = container;
		this.crypto = crypto;
		this.table = table;
		this.service = service;
	}

	public void AddBreakPoint(final int position) {
		getCurrentContext().getBreakPoints().add(position);
	}

	public boolean Execute() {
		State.remove(VMState.BREAK);
		while (!State.contains(VMState.HALT) && !State.contains(VMState.FAULT) && !State.contains(VMState.BREAK)) {
			StepInto();
		}
		return true;
	}

	private void ExecuteOp(final OpCode opcode, final ExecutionContext context) {
		if ((opcode.ordinal() > OpCode.PUSH16.ordinal()) && (opcode != OpCode.RET) && context.pushOnly) {
			State.add(VMState.FAULT);
			return;
		}
		if ((opcode.ordinal() >= OpCode.PUSHBYTES1.ordinal()) && (opcode.ordinal() <= OpCode.PUSHBYTES75.ordinal())) {
			evaluationStack.push(AbstractStackItem.valueOf(context.getOpReader().get(opcode.getTypeByte())));
		} else {
			switch (opcode) {
			// Push value
			case PUSH0:
				evaluationStack.push(AbstractStackItem.valueOf(new byte[0]));
				break;
			case PUSHDATA1:
				evaluationStack.push(AbstractStackItem.valueOf(context.getOpReader().get(context.getOpReader().get())));
				break;
			case PUSHDATA2:
				evaluationStack.push(AbstractStackItem.valueOf(ModelUtil.getUInt16(context.getOpReader()).asInt()));
				break;
			case PUSHDATA4:
				evaluationStack.push(AbstractStackItem.valueOf(ModelUtil.getUInt32(context.getOpReader()).asLong()));
				break;
			case PUSHM1:
			case PUSH1:
			case PUSH2:
			case PUSH3:
			case PUSH4:
			case PUSH5:
			case PUSH6:
			case PUSH7:
			case PUSH8:
			case PUSH9:
			case PUSH10:
			case PUSH11:
			case PUSH12:
			case PUSH13:
			case PUSH14:
			case PUSH15:
			case PUSH16:
				evaluationStack
						.push(AbstractStackItem.valueOf((opcode.getTypeByte() - OpCode.PUSH1.getTypeByte()) + 1));
				break;

			// Control
			case NOP:
				break;
			case JMP:
			case JMPIF:
			case JMPIFNOT: {
				int offset = ModelUtil.getUInt16(context.getOpReader()).asInt();
				offset = (context.getInstructionPointer() + offset) - 3;
				if ((offset < 0) || (offset > context.getScript().length)) {
					State.add(VMState.HALT);
					return;
				}
				boolean fValue = true;
				if (opcode.ordinal() > OpCode.JMP.ordinal()) {
					fValue = evaluationStack.pop().getBoolean();
					if (opcode == OpCode.JMPIFNOT) {
						fValue = !fValue;
					}
				}
				if (fValue) {
					context.setInstructionPointer(offset);
				}
				break;
			}
			case CALL:
				invocationStack.push(context.getClone());
				context.setInstructionPointer(context.getInstructionPointer() + 2);
				ExecuteOp(OpCode.JMP, getCurrentContext());
				break;
			case RET:
				invocationStack.pop();
				if (invocationStack.getCount() == 0) {
					State.add(VMState.HALT);
				}
				break;
			case APPCALL:
			case TAILCALL: {
				if (table == null) {
					State.add(VMState.HALT);
					return;
				}

				byte[] script_hash = new byte[20];
				context.getOpReader().get(script_hash);
				boolean allZero = true;
				for (final byte b : script_hash) {
					if (b != 0) {
						allZero = false;
					}
				}
				if (allZero) {
					script_hash = evaluationStack.pop().getByteArray();
				}

				final byte[] script = table.getScript(script_hash);
				if (script == null) {
					State.add(VMState.HALT);
					return;
				}
				if (opcode == OpCode.TAILCALL) {
					invocationStack.pop();
				}
				loadScript(script, false);
				break;
			}
			case SYSCALL:
				if (!service.invoke(ModelUtil.getVariableLengthString(context.getOpReader()), this)) {
					State.add(VMState.HALT);
				}
				break;

			// Stack ops
			case DUPFROMALTSTACK:
				evaluationStack.push(altStack.peek());
				break;
			case TOALTSTACK:
				altStack.push(evaluationStack.pop());
				break;
			case FROMALTSTACK:
				evaluationStack.push(altStack.pop());
				break;
			case XDROP: {
				final int n = evaluationStack.pop().getBigInteger().intValue();
				if (n < 0) {
					State.add(VMState.HALT);
					return;
				}
				evaluationStack.remove(n);
				break;
			}
			case XSWAP: {
				final int n = evaluationStack.pop().getBigInteger().intValue();
				if (n < 0) {
					State.add(VMState.HALT);
					return;
				}
				if (n == 0) {
					break;
				}
				final AbstractStackItem xn = evaluationStack.peek(n);
				evaluationStack.set(n, evaluationStack.peek());
				evaluationStack.set(0, xn);
				break;
			}
			case XTUCK: {
				final int n = evaluationStack.pop().getBigInteger().intValue();
				if (n <= 0) {
					State.add(VMState.HALT);
					return;
				}
				evaluationStack.insert(n, evaluationStack.peek());
				break;
			}
			case DEPTH:
				evaluationStack.push(AbstractStackItem.valueOf(evaluationStack.getCount()));
				break;
			case DROP:
				evaluationStack.pop();
				break;
			case DUP:
				evaluationStack.push(evaluationStack.peek());
				break;
			case NIP: {
				final AbstractStackItem x2 = evaluationStack.pop();
				evaluationStack.pop();
				evaluationStack.push(x2);
				break;
			}
			case OVER: {
				final AbstractStackItem x2 = evaluationStack.pop();
				final AbstractStackItem x1 = evaluationStack.peek();
				evaluationStack.push(x2);
				evaluationStack.push(x1);
				break;
			}
			case PICK: {
				final int n = evaluationStack.pop().getBigInteger().intValue();
				if (n < 0) {
					State.add(VMState.HALT);
					return;
				}
				evaluationStack.push(evaluationStack.peek(n));
				break;
			}
			case ROLL: {
				final int n = evaluationStack.pop().getBigInteger().intValue();
				if (n < 0) {
					State.add(VMState.HALT);
					return;
				}
				if (n == 0) {
					break;
				}
				evaluationStack.push(evaluationStack.remove(n));
				break;
			}
			case ROT: {
				final AbstractStackItem x3 = evaluationStack.pop();
				final AbstractStackItem x2 = evaluationStack.pop();
				final AbstractStackItem x1 = evaluationStack.pop();
				evaluationStack.push(x2);
				evaluationStack.push(x3);
				evaluationStack.push(x1);
				break;
			}
			case SWAP: {
				final AbstractStackItem x2 = evaluationStack.pop();
				final AbstractStackItem x1 = evaluationStack.pop();
				evaluationStack.push(x2);
				evaluationStack.push(x1);
				break;
			}
			case TUCK: {
				final AbstractStackItem x2 = evaluationStack.pop();
				final AbstractStackItem x1 = evaluationStack.pop();
				evaluationStack.push(x2);
				evaluationStack.push(x1);
				evaluationStack.push(x2);
				break;
			}
			case CAT: {
				final byte[] x2 = evaluationStack.pop().getByteArray();
				final byte[] x1 = evaluationStack.pop().getByteArray();
				final byte[] concat = new byte[x1.length + x2.length];
				System.arraycopy(x1, 0, concat, 0, x1.length);
				System.arraycopy(x2, 0, concat, x1.length, x2.length);
				evaluationStack.push(AbstractStackItem.valueOf(concat));
				break;
			}
			case SUBSTR: {
				final int count = evaluationStack.pop().getBigInteger().intValue();
				if (count < 0) {
					State.add(VMState.HALT);
					return;
				}
				final int index = evaluationStack.pop().getBigInteger().intValue();
				if (index < 0) {
					State.add(VMState.HALT);
					return;
				}
				final byte[] x = evaluationStack.pop().getByteArray();
				final byte[] substr = new byte[count];
				System.arraycopy(x, index, substr, 0, count);
				evaluationStack.push(AbstractStackItem.valueOf(substr));
				break;
			}
			case LEFT: {
				final int count = evaluationStack.pop().getBigInteger().intValue();
				if (count < 0) {
					State.add(VMState.HALT);
					return;
				}
				final byte[] x = evaluationStack.pop().getByteArray();
				final byte[] substr = new byte[count];
				System.arraycopy(x, 0, substr, 0, count);
				evaluationStack.push(AbstractStackItem.valueOf(substr));
				break;
			}
			case RIGHT: {
				final int count = evaluationStack.pop().getBigInteger().intValue();
				if (count < 0) {
					State.add(VMState.HALT);
					return;
				}
				final byte[] x = evaluationStack.pop().getByteArray();
				if (x.length < count) {
					State.add(VMState.HALT);
					return;
				}
				final byte[] substr = new byte[count];
				System.arraycopy(x, x.length - count, substr, 0, count);
				evaluationStack.push(AbstractStackItem.valueOf(substr));
				break;
			}
			case SIZE: {
				final byte[] x = evaluationStack.pop().getByteArray();
				evaluationStack.push(AbstractStackItem.valueOf(x.length));
				break;
			}

			// Bitwise logic
			case INVERT: {
				final BigInteger x = evaluationStack.pop().getBigInteger();
				;
				evaluationStack.push(AbstractStackItem.valueOf(x.not()));
				break;
			}
			case AND: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.and(x2)));
				break;
			}
			case OR: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.or(x2)));
				break;
			}
			case XOR: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.xor(x2)));
				break;
			}
			case EQUAL: {
				final AbstractStackItem x2 = evaluationStack.pop();
				final AbstractStackItem x1 = evaluationStack.pop();
				evaluationStack.push(AbstractStackItem.valueOf(x1.equals(x2)));
				break;
			}

			// Numeric
			case INC: {
				final BigInteger x = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x.add(BigInteger.ONE)));
				break;
			}
			case DEC: {
				final BigInteger x = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x.subtract(BigInteger.ONE)));
				break;
			}
			case SIGN: {
				final BigInteger x = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x.signum()));
				break;
			}
			case NEGATE: {
				final BigInteger x = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x.negate()));
				break;
			}
			case ABS: {
				final BigInteger x = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x.abs()));
				break;
			}
			case NOT: {
				final boolean x = evaluationStack.pop().getBoolean();
				evaluationStack.push(AbstractStackItem.valueOf(!x));
				break;
			}
			case NZ: {
				final BigInteger x = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x != BigInteger.ZERO));
				break;
			}
			case ADD: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.add(x2)));
				break;
			}
			case SUB: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.subtract(x2)));
				break;
			}
			case MUL: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.multiply(x2)));
				break;
			}
			case DIV: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.divide(x2)));
				break;
			}
			case MOD: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.mod(x2)));
				break;
			}
			case SHL: {
				final int n = evaluationStack.pop().getBigInteger().intValue();
				final BigInteger x = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x.shiftLeft(n)));
				break;
			}
			case SHR: {
				final int n = evaluationStack.pop().getBigInteger().intValue();
				final BigInteger x = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x.shiftRight(n)));
				break;
			}
			case BOOLAND: {
				final boolean x2 = evaluationStack.pop().getBoolean();
				final boolean x1 = evaluationStack.pop().getBoolean();
				evaluationStack.push(AbstractStackItem.valueOf(x1 && x2));
				break;
			}
			case BOOLOR: {
				final boolean x2 = evaluationStack.pop().getBoolean();
				final boolean x1 = evaluationStack.pop().getBoolean();
				evaluationStack.push(AbstractStackItem.valueOf(x1 || x2));
				break;
			}
			case NUMEQUAL: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.equals(x2)));
				break;
			}
			case NUMNOTEQUAL: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(!x1.equals(x2)));
				break;
			}
			case LT: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.compareTo(x2) < 0));
				break;
			}
			case GT: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.compareTo(x2) > 0));
				break;
			}
			case LTE: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.compareTo(x2) <= 0));
				break;
			}
			case GTE: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.compareTo(x2) >= 0));
				break;
			}
			case MIN: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();

				evaluationStack.push(AbstractStackItem.valueOf(x1.min(x2)));
				break;
			}
			case MAX: {
				final BigInteger x2 = evaluationStack.pop().getBigInteger();
				final BigInteger x1 = evaluationStack.pop().getBigInteger();
				evaluationStack.push(AbstractStackItem.valueOf(x1.max(x2)));
				break;
			}
			case WITHIN: {
				final BigInteger b = evaluationStack.pop().getBigInteger();
				final BigInteger a = evaluationStack.pop().getBigInteger();
				final BigInteger x = evaluationStack.pop().getBigInteger();
				// evaluationStack.push(a <= x && x < b);
				final boolean within = (a.compareTo(x) <= 0) && (x.compareTo(b) < 0);
				evaluationStack.push(AbstractStackItem.valueOf(within));
				break;
			}

			// Crypto
			// case SHA1:
			// using (SHA1 sha = SHA1.Create())
			// {
			// byte[] x = evaluationStack.pop().getByteArray();
			// evaluationStack.push(sha.ComputeHash(x));
			// }
			// break;
			// case SHA256:
			// using (SHA256 sha = SHA256.Create())
			// {
			// byte[] x = evaluationStack.pop().getByteArray();
			// evaluationStack.push(sha.ComputeHash(x));
			// }
			// break;
			// case HASH160:
			// {
			// byte[] x = evaluationStack.pop().getByteArray();
			// evaluationStack.push(Crypto.Hash160(x));
			// }
			// break;
			// case HASH256:
			// {
			// byte[] x = evaluationStack.pop().getByteArray();
			// evaluationStack.push(Crypto.Hash256(x));
			// }
			// break;
			// case CHECKSIG:
			// {
			// byte[] pubkey = evaluationStack.pop().getByteArray();
			// byte[] signature = evaluationStack.pop().getByteArray();
			// try
			// {
			// evaluationStack.push(Crypto.VerifySignature(ScriptContainer.GetMessage(),
			// signature, pubkey));
			// }
			// catch (ArgumentException)
			// {
			// evaluationStack.push(false);
			// }
			// }
			// break;
			// case CHECKMULTISIG:
			// {
			// int n;
			// byte[][] pubkeys;
			// AbstractStackItem item = evaluationStack.pop();
			// if (item.IsArray)
			// {
			// pubkeys = item.GetArray().Select(p => p.getByteArray()).ToArray();
			// n = pubkeys.Length;
			// if (n == 0)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// }
			// else
			// {
			// n = (int)item.getBigInteger();
			// if (n < 1 || n > evaluationStack.Count)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// pubkeys = new byte[n][];
			// for (int i = 0; i < n; i++)
			// pubkeys[i] = evaluationStack.pop().getByteArray();
			// }
			// int m;
			// byte[][] signatures;
			// item = evaluationStack.pop();
			// if (item.IsArray)
			// {
			// signatures = item.GetArray().Select(p => p.getByteArray()).ToArray();
			// m = signatures.Length;
			// if (m == 0 || m > n)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// }
			// else
			// {
			// m = (int)item.getBigInteger();
			// if (m < 1 || m > n || m > evaluationStack.Count)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// signatures = new byte[m][];
			// for (int i = 0; i < m; i++)
			// signatures[i] = evaluationStack.pop().getByteArray();
			// }
			// byte[] message = ScriptContainer.GetMessage();
			// bool fSuccess = true;
			// try
			// {
			// for (int i = 0, j = 0; fSuccess && i < m && j < n;)
			// {
			// if (Crypto.VerifySignature(message, signatures[i], pubkeys[j]))
			// i++;
			// j++;
			// if (m - i > n - j)
			// fSuccess = false;
			// }
			// }
			// catch (ArgumentException)
			// {
			// fSuccess = false;
			// }
			// evaluationStack.push(fSuccess);
			// }
			// break;

			// Array
			// case ARRAYSIZE:
			// {
			// AbstractStackItem item = evaluationStack.pop();
			// if (!item.IsArray)
			// evaluationStack.push(item.getByteArray().Length);
			// else
			// evaluationStack.push(item.GetArray().Count);
			// }
			// break;
			// case PACK:
			// {
			// int size = (int)evaluationStack.pop().getBigInteger();
			// if (size < 0 || size > evaluationStack.Count)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// List<AbstractStackItem> items = new List<AbstractStackItem>(size);
			// for (int i = 0; i < size; i++)
			// items.Add(evaluationStack.pop());
			// evaluationStack.push(items);
			// }
			// break;
			// case UNPACK:
			// {
			// AbstractStackItem item = evaluationStack.pop();
			// if (!item.IsArray)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// IList<AbstractStackItem> items = item.GetArray();
			// for (int i = items.Count - 1; i >= 0; i--)
			// evaluationStack.push(items[i]);
			// evaluationStack.push(items.Count);
			// }
			// break;
			// case PICKITEM:
			// {
			// int index = (int)evaluationStack.pop().getBigInteger();
			// if (index < 0)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// AbstractStackItem item = evaluationStack.pop();
			// if (!item.IsArray)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// IList<AbstractStackItem> items = item.GetArray();
			// if (index >= items.Count)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// evaluationStack.push(items[index]);
			// }
			// break;
			// case SETITEM:
			// {
			// AbstractStackItem newItem = evaluationStack.pop();
			// if (newItem is Types.Struct s)
			// {
			// newItem = s.Clone();
			// }
			// int index = (int)evaluationStack.pop().getBigInteger();
			// AbstractStackItem arrItem = evaluationStack.pop();
			// if (!arrItem.IsArray)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// IList<AbstractStackItem> items = arrItem.GetArray();
			// if (index < 0 || index >= items.Count)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// items[index] = newItem;
			// }
			// break;
			// case NEWARRAY:
			// {
			// int count = (int)evaluationStack.pop().getBigInteger();
			// List<AbstractStackItem> items = new List<AbstractStackItem>(count);
			// for (var i = 0; i < count; i++)
			// {
			// items.Add(false);
			// }
			// evaluationStack.push(new Types.Array(items));
			// }
			// break;
			// case NEWSTRUCT:
			// {
			// int count = (int)evaluationStack.pop().getBigInteger();
			// List<AbstractStackItem> items = new List<AbstractStackItem>(count);
			// for (var i = 0; i < count; i++)
			// {
			// items.Add(false);
			// }
			// evaluationStack.push(new VM.Types.Struct(items));
			// }
			// break;
			// case APPEND:
			// {
			// AbstractStackItem newItem = evaluationStack.pop();
			// if (newItem is Types.Struct s)
			// {
			// newItem = s.Clone();
			// }
			// AbstractStackItem arrItem = evaluationStack.pop();
			// if (!arrItem.IsArray)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// IList<AbstractStackItem> items = arrItem.GetArray();
			// items.Add(newItem);
			// }
			// break;
			//
			// case REVERSE:
			// {
			// AbstractStackItem arrItem = evaluationStack.pop();
			// if (!arrItem.IsArray)
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// ((Types.Array)arrItem).Reverse();
			// }
			// break;
			//
			// // Exceptions
			// case THROW:
			// State.add(VMState.HALT);
			// return;
			// case THROWIFNOT:
			// if (!evaluationStack.pop().GetBoolean())
			// {
			// State.add(VMState.HALT);
			// return;
			// }
			// break;
			//
			// default:
			// State.add(VMState.HALT);
			// return;
			// }
			// if (!State.HasFlag(VMState.FAULT) && invocationStack.Count > 0)
			// {
			// if
			// (CurrentContext.BreakPoints.Contains((uint)CurrentContext.InstructionPointer))
			// State |= VMState.BREAK;
			// }
			}
		}

		// public void LoadScript(byte[] script, bool push_only = false)
		// {
		// InvocationStack.Push(new ExecutionContext(this, script, push_only));
		// }
		//
		// public bool RemoveBreakPoint(uint position)
		// {
		// if (InvocationStack.Count == 0) return false;
		// return CurrentContext.BreakPoints.Remove(position);
		// }
	}

	/**
	 * return the calling context.
	 *
	 * @return the calling context.
	 */
	public ExecutionContext getCallingContext() {
		throw new NotImplementedException("getCallingContext");
	}

	/**
	 * return the executing script hash.
	 *
	 * @return the executing script hash.
	 */
	public IScriptContainer getCallingScriptHash() {
		throw new NotImplementedException("getCallingScriptHash");
	}

	/**
	 * return the crypto package.
	 *
	 * @return the crypto package.
	 */
	public ICrypto getCrypto() {
		return crypto;
	}

	/**
	 * return the current context.
	 *
	 * @return the current context.
	 */
	public ExecutionContext getCurrentContext() {
		throw new NotImplementedException("getCurrentContext");
	}

	/**
	 * return the entry context.
	 *
	 * @return the entry context.
	 */
	public ExecutionContext getEntryContext() {
		throw new NotImplementedException("getEntryContext");
	}

	/**
	 * return the entry script hash.
	 *
	 * @return the entry script hash.
	 */
	public IScriptContainer getEntryScriptHash() {
		throw new NotImplementedException("getEntryScriptHash");
	}

	/**
	 * return the executing script hash.
	 *
	 * @return the executing script hash.
	 */
	public IScriptContainer getExecutingScriptHash() {
		throw new NotImplementedException("getExecutingScriptHash");
	}

	/**
	 * return the script container.
	 *
	 * @return the script container.
	 */
	public IScriptContainer getScriptContainer() {
		return scriptContainer;
	}

	/**
	 * return the service.
	 *
	 * @return the service.
	 */
	public InteropService getService() {
		return service;
	}

	/**
	 * return the state.
	 *
	 * @return the state.
	 */
	public Set<VMState> getState() {
		return State;
	}

	/**
	 * return the script table.
	 *
	 * @return the script table.
	 */
	public IScriptTable getTable() {
		return table;
	}

	/**
	 * loads the script.
	 *
	 * @param script
	 *            the script to load.
	 * @param push_only
	 *            push only.
	 */
	public void loadScript(final byte[] script, final boolean push_only) {
		invocationStack.push(new ExecutionContext(this, script, push_only, null));
	}

	public void StepInto() {
		if (invocationStack.getCount() == 0) {
			State.add(VMState.HALT);
		}
		if (State.contains(VMState.HALT) || State.contains(VMState.FAULT)) {
			return;
		}
		final OpCode opcode = getCurrentContext().getInstructionPointer() >= getCurrentContext().getScript().length
				? OpCode.RET
				: OpCode.valueOfByte(getCurrentContext().getOpReader().get());
		try {
			ExecuteOp(opcode, getCurrentContext());
		} catch (final Exception e) {
			State.add(VMState.HALT);
		}
	}

	public void StepOut() {
		State.remove(VMState.BREAK);
		final int c = invocationStack.getCount();
		while (!State.contains(VMState.HALT) && !State.contains(VMState.FAULT) && !State.contains(VMState.BREAK)
				&& (invocationStack.getCount() >= c)) {
			StepInto();
		}
	}

	public void StepOver() {
		if (State.contains(VMState.HALT) || State.contains(VMState.FAULT)) {
			return;
		}
		State.remove(VMState.BREAK);
		final int c = invocationStack.getCount();
		do {
			StepInto();
		} while (!State.contains(VMState.HALT) && !State.contains(VMState.FAULT) && !State.contains(VMState.BREAK)
				&& (invocationStack.getCount() > c));
	}

}

// using System;
// using System.Collections.Generic;
// using System.Linq;
// using System.Numerics;
// using System.Security.Cryptography;
// using System.Text;
//
// namespace Neo.VM
// {
// public class ExecutionEngine : IDisposable
// {
// private readonly IScriptTable table;
// private readonly InteropService service;
//
// public IScriptContainer ScriptContainer { get; }
// public ICrypto Crypto { get; }
// public RandomAccessStack<ExecutionContext> InvocationStack { get; } = new
// RandomAccessStack<ExecutionContext>();
// public RandomAccessStack<StackItem> EvaluationStack { get; } = new
// RandomAccessStack<StackItem>();
// public RandomAccessStack<StackItem> AltStack { get; } = new
// RandomAccessStack<StackItem>();
// public ExecutionContext CurrentContext => InvocationStack.Peek();
// public ExecutionContext CallingContext => InvocationStack.Count > 1 ?
// InvocationStack.Peek(1) : null;
// public ExecutionContext EntryContext =>
// InvocationStack.Peek(InvocationStack.Count - 1);
// public VMState State { get; private set; } = VMState.BREAK;
//
// public ExecutionEngine(IScriptContainer container, ICrypto crypto,
// IScriptTable table = null, InteropService service = null)
// {
// this.ScriptContainer = container;
// this.Crypto = crypto;
// this.table = table;
// this.service = service ?? new InteropService();
// }
//
// public void AddBreakPoint(uint position)
// {
// CurrentContext.BreakPoints.Add(position);
// }
//
// public void Dispose()
// {
// while (InvocationStack.Count > 0)
// InvocationStack.Pop().Dispose();
// }
//
// public void Execute()
// {
// State &= ~VMState.BREAK;
// while (!State.HasFlag(VMState.HALT) && !State.HasFlag(VMState.FAULT) &&
// !State.HasFlag(VMState.BREAK))
// StepInto();
// }
//
// private void ExecuteOp(OpCode opcode, ExecutionContext context)
// {
// if (opcode > OpCode.PUSH16 && opcode != OpCode.RET && context.PushOnly)
// {
// State |= VMState.FAULT;
// return;
// }
// if (opcode >= OpCode.PUSHBYTES1 && opcode <= OpCode.PUSHBYTES75)
// EvaluationStack.Push(context.OpReader.ReadBytes((byte)opcode));
// else
// switch (opcode)
// {
// // Push value
// case OpCode.PUSH0:
// EvaluationStack.Push(new byte[0]);
// break;
// case OpCode.PUSHDATA1:
// EvaluationStack.Push(context.OpReader.ReadBytes(context.OpReader.ReadByte()));
// break;
// case OpCode.PUSHDATA2:
// EvaluationStack.Push(context.OpReader.ReadBytes(context.OpReader.ReadUInt16()));
// break;
// case OpCode.PUSHDATA4:
// EvaluationStack.Push(context.OpReader.ReadBytes(context.OpReader.ReadInt32()));
// break;
// case OpCode.PUSHM1:
// case OpCode.PUSH1:
// case OpCode.PUSH2:
// case OpCode.PUSH3:
// case OpCode.PUSH4:
// case OpCode.PUSH5:
// case OpCode.PUSH6:
// case OpCode.PUSH7:
// case OpCode.PUSH8:
// case OpCode.PUSH9:
// case OpCode.PUSH10:
// case OpCode.PUSH11:
// case OpCode.PUSH12:
// case OpCode.PUSH13:
// case OpCode.PUSH14:
// case OpCode.PUSH15:
// case OpCode.PUSH16:
// EvaluationStack.Push((int)opcode - (int)OpCode.PUSH1 + 1);
// break;
//
// // Control
// case OpCode.NOP:
// break;
// case OpCode.JMP:
// case OpCode.JMPIF:
// case OpCode.JMPIFNOT:
// {
// int offset = context.OpReader.ReadInt16();
// offset = context.InstructionPointer + offset - 3;
// if (offset < 0 || offset > context.Script.Length)
// {
// State |= VMState.FAULT;
// return;
// }
// bool fValue = true;
// if (opcode > OpCode.JMP)
// {
// fValue = EvaluationStack.Pop().GetBoolean();
// if (opcode == OpCode.JMPIFNOT)
// fValue = !fValue;
// }
// if (fValue)
// context.InstructionPointer = offset;
// }
// break;
// case OpCode.CALL:
// InvocationStack.Push(context.Clone());
// context.InstructionPointer += 2;
// ExecuteOp(OpCode.JMP, CurrentContext);
// break;
// case OpCode.RET:
// InvocationStack.Pop().Dispose();
// if (InvocationStack.Count == 0)
// State |= VMState.HALT;
// break;
// case OpCode.APPCALL:
// case OpCode.TAILCALL:
// {
// if (table == null)
// {
// State |= VMState.FAULT;
// return;
// }
//
// byte[] script_hash = context.OpReader.ReadBytes(20);
// if (script_hash.All(p => p == 0))
// {
// script_hash = EvaluationStack.Pop().GetByteArray();
// }
//
// byte[] script = table.GetScript(script_hash);
// if (script == null)
// {
// State |= VMState.FAULT;
// return;
// }
// if (opcode == OpCode.TAILCALL)
// InvocationStack.Pop().Dispose();
// LoadScript(script);
// }
// break;
// case OpCode.SYSCALL:
// if
// (!service.Invoke(Encoding.ASCII.GetString(context.OpReader.ReadVarBytes(252)),
// this))
// State |= VMState.FAULT;
// break;
//
// // Stack ops
// case OpCode.DUPFROMALTSTACK:
// EvaluationStack.Push(AltStack.Peek());
// break;
// case OpCode.TOALTSTACK:
// AltStack.Push(EvaluationStack.Pop());
// break;
// case OpCode.FROMALTSTACK:
// EvaluationStack.Push(AltStack.Pop());
// break;
// case OpCode.XDROP:
// {
// int n = (int)EvaluationStack.Pop().GetBigInteger();
// if (n < 0)
// {
// State |= VMState.FAULT;
// return;
// }
// EvaluationStack.Remove(n);
// }
// break;
// case OpCode.XSWAP:
// {
// int n = (int)EvaluationStack.Pop().GetBigInteger();
// if (n < 0)
// {
// State |= VMState.FAULT;
// return;
// }
// if (n == 0) break;
// StackItem xn = EvaluationStack.Peek(n);
// EvaluationStack.Set(n, EvaluationStack.Peek());
// EvaluationStack.Set(0, xn);
// }
// break;
// case OpCode.XTUCK:
// {
// int n = (int)EvaluationStack.Pop().GetBigInteger();
// if (n <= 0)
// {
// State |= VMState.FAULT;
// return;
// }
// EvaluationStack.Insert(n, EvaluationStack.Peek());
// }
// break;
// case OpCode.DEPTH:
// EvaluationStack.Push(EvaluationStack.Count);
// break;
// case OpCode.DROP:
// EvaluationStack.Pop();
// break;
// case OpCode.DUP:
// EvaluationStack.Push(EvaluationStack.Peek());
// break;
// case OpCode.NIP:
// {
// StackItem x2 = EvaluationStack.Pop();
// EvaluationStack.Pop();
// EvaluationStack.Push(x2);
// }
// break;
// case OpCode.OVER:
// {
// StackItem x2 = EvaluationStack.Pop();
// StackItem x1 = EvaluationStack.Peek();
// EvaluationStack.Push(x2);
// EvaluationStack.Push(x1);
// }
// break;
// case OpCode.PICK:
// {
// int n = (int)EvaluationStack.Pop().GetBigInteger();
// if (n < 0)
// {
// State |= VMState.FAULT;
// return;
// }
// EvaluationStack.Push(EvaluationStack.Peek(n));
// }
// break;
// case OpCode.ROLL:
// {
// int n = (int)EvaluationStack.Pop().GetBigInteger();
// if (n < 0)
// {
// State |= VMState.FAULT;
// return;
// }
// if (n == 0) break;
// EvaluationStack.Push(EvaluationStack.Remove(n));
// }
// break;
// case OpCode.ROT:
// {
// StackItem x3 = EvaluationStack.Pop();
// StackItem x2 = EvaluationStack.Pop();
// StackItem x1 = EvaluationStack.Pop();
// EvaluationStack.Push(x2);
// EvaluationStack.Push(x3);
// EvaluationStack.Push(x1);
// }
// break;
// case OpCode.SWAP:
// {
// StackItem x2 = EvaluationStack.Pop();
// StackItem x1 = EvaluationStack.Pop();
// EvaluationStack.Push(x2);
// EvaluationStack.Push(x1);
// }
// break;
// case OpCode.TUCK:
// {
// StackItem x2 = EvaluationStack.Pop();
// StackItem x1 = EvaluationStack.Pop();
// EvaluationStack.Push(x2);
// EvaluationStack.Push(x1);
// EvaluationStack.Push(x2);
// }
// break;
// case OpCode.CAT:
// {
// byte[] x2 = EvaluationStack.Pop().GetByteArray();
// byte[] x1 = EvaluationStack.Pop().GetByteArray();
// EvaluationStack.Push(x1.Concat(x2).ToArray());
// }
// break;
// case OpCode.SUBSTR:
// {
// int count = (int)EvaluationStack.Pop().GetBigInteger();
// if (count < 0)
// {
// State |= VMState.FAULT;
// return;
// }
// int index = (int)EvaluationStack.Pop().GetBigInteger();
// if (index < 0)
// {
// State |= VMState.FAULT;
// return;
// }
// byte[] x = EvaluationStack.Pop().GetByteArray();
// EvaluationStack.Push(x.Skip(index).Take(count).ToArray());
// }
// break;
// case OpCode.LEFT:
// {
// int count = (int)EvaluationStack.Pop().GetBigInteger();
// if (count < 0)
// {
// State |= VMState.FAULT;
// return;
// }
// byte[] x = EvaluationStack.Pop().GetByteArray();
// EvaluationStack.Push(x.Take(count).ToArray());
// }
// break;
// case OpCode.RIGHT:
// {
// int count = (int)EvaluationStack.Pop().GetBigInteger();
// if (count < 0)
// {
// State |= VMState.FAULT;
// return;
// }
// byte[] x = EvaluationStack.Pop().GetByteArray();
// if (x.Length < count)
// {
// State |= VMState.FAULT;
// return;
// }
// EvaluationStack.Push(x.Skip(x.Length - count).ToArray());
// }
// break;
// case OpCode.SIZE:
// {
// byte[] x = EvaluationStack.Pop().GetByteArray();
// EvaluationStack.Push(x.Length);
// }
// break;
//
// // Bitwise logic
// case OpCode.INVERT:
// {
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(~x);
// }
// break;
// case OpCode.AND:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 & x2);
// }
// break;
// case OpCode.OR:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 | x2);
// }
// break;
// case OpCode.XOR:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 ^ x2);
// }
// break;
// case OpCode.EQUAL:
// {
// StackItem x2 = EvaluationStack.Pop();
// StackItem x1 = EvaluationStack.Pop();
// EvaluationStack.Push(x1.Equals(x2));
// }
// break;
//
// // Numeric
// case OpCode.INC:
// {
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x + 1);
// }
// break;
// case OpCode.DEC:
// {
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x - 1);
// }
// break;
// case OpCode.SIGN:
// {
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x.Sign);
// }
// break;
// case OpCode.NEGATE:
// {
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(-x);
// }
// break;
// case OpCode.ABS:
// {
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(BigInteger.Abs(x));
// }
// break;
// case OpCode.NOT:
// {
// bool x = EvaluationStack.Pop().GetBoolean();
// EvaluationStack.Push(!x);
// }
// break;
// case OpCode.NZ:
// {
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x != BigInteger.Zero);
// }
// break;
// case OpCode.ADD:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 + x2);
// }
// break;
// case OpCode.SUB:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 - x2);
// }
// break;
// case OpCode.MUL:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 * x2);
// }
// break;
// case OpCode.DIV:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 / x2);
// }
// break;
// case OpCode.MOD:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 % x2);
// }
// break;
// case OpCode.SHL:
// {
// int n = (int)EvaluationStack.Pop().GetBigInteger();
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x << n);
// }
// break;
// case OpCode.SHR:
// {
// int n = (int)EvaluationStack.Pop().GetBigInteger();
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x >> n);
// }
// break;
// case OpCode.BOOLAND:
// {
// bool x2 = EvaluationStack.Pop().GetBoolean();
// bool x1 = EvaluationStack.Pop().GetBoolean();
// EvaluationStack.Push(x1 && x2);
// }
// break;
// case OpCode.BOOLOR:
// {
// bool x2 = EvaluationStack.Pop().GetBoolean();
// bool x1 = EvaluationStack.Pop().GetBoolean();
// EvaluationStack.Push(x1 || x2);
// }
// break;
// case OpCode.NUMEQUAL:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 == x2);
// }
// break;
// case OpCode.NUMNOTEQUAL:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 != x2);
// }
// break;
// case OpCode.LT:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 < x2);
// }
// break;
// case OpCode.GT:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 > x2);
// }
// break;
// case OpCode.LTE:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 <= x2);
// }
// break;
// case OpCode.GTE:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(x1 >= x2);
// }
// break;
// case OpCode.MIN:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(BigInteger.Min(x1, x2));
// }
// break;
// case OpCode.MAX:
// {
// BigInteger x2 = EvaluationStack.Pop().GetBigInteger();
// BigInteger x1 = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(BigInteger.Max(x1, x2));
// }
// break;
// case OpCode.WITHIN:
// {
// BigInteger b = EvaluationStack.Pop().GetBigInteger();
// BigInteger a = EvaluationStack.Pop().GetBigInteger();
// BigInteger x = EvaluationStack.Pop().GetBigInteger();
// EvaluationStack.Push(a <= x && x < b);
// }
// break;
//
// // Crypto
// case OpCode.SHA1:
// using (SHA1 sha = SHA1.Create())
// {
// byte[] x = EvaluationStack.Pop().GetByteArray();
// EvaluationStack.Push(sha.ComputeHash(x));
// }
// break;
// case OpCode.SHA256:
// using (SHA256 sha = SHA256.Create())
// {
// byte[] x = EvaluationStack.Pop().GetByteArray();
// EvaluationStack.Push(sha.ComputeHash(x));
// }
// break;
// case OpCode.HASH160:
// {
// byte[] x = EvaluationStack.Pop().GetByteArray();
// EvaluationStack.Push(Crypto.Hash160(x));
// }
// break;
// case OpCode.HASH256:
// {
// byte[] x = EvaluationStack.Pop().GetByteArray();
// EvaluationStack.Push(Crypto.Hash256(x));
// }
// break;
// case OpCode.CHECKSIG:
// {
// byte[] pubkey = EvaluationStack.Pop().GetByteArray();
// byte[] signature = EvaluationStack.Pop().GetByteArray();
// try
// {
// EvaluationStack.Push(Crypto.VerifySignature(ScriptContainer.GetMessage(),
// signature, pubkey));
// }
// catch (ArgumentException)
// {
// EvaluationStack.Push(false);
// }
// }
// break;
// case OpCode.CHECKMULTISIG:
// {
// int n;
// byte[][] pubkeys;
// StackItem item = EvaluationStack.Pop();
// if (item.IsArray)
// {
// pubkeys = item.GetArray().Select(p => p.GetByteArray()).ToArray();
// n = pubkeys.Length;
// if (n == 0)
// {
// State |= VMState.FAULT;
// return;
// }
// }
// else
// {
// n = (int)item.GetBigInteger();
// if (n < 1 || n > EvaluationStack.Count)
// {
// State |= VMState.FAULT;
// return;
// }
// pubkeys = new byte[n][];
// for (int i = 0; i < n; i++)
// pubkeys[i] = EvaluationStack.Pop().GetByteArray();
// }
// int m;
// byte[][] signatures;
// item = EvaluationStack.Pop();
// if (item.IsArray)
// {
// signatures = item.GetArray().Select(p => p.GetByteArray()).ToArray();
// m = signatures.Length;
// if (m == 0 || m > n)
// {
// State |= VMState.FAULT;
// return;
// }
// }
// else
// {
// m = (int)item.GetBigInteger();
// if (m < 1 || m > n || m > EvaluationStack.Count)
// {
// State |= VMState.FAULT;
// return;
// }
// signatures = new byte[m][];
// for (int i = 0; i < m; i++)
// signatures[i] = EvaluationStack.Pop().GetByteArray();
// }
// byte[] message = ScriptContainer.GetMessage();
// bool fSuccess = true;
// try
// {
// for (int i = 0, j = 0; fSuccess && i < m && j < n;)
// {
// if (Crypto.VerifySignature(message, signatures[i], pubkeys[j]))
// i++;
// j++;
// if (m - i > n - j)
// fSuccess = false;
// }
// }
// catch (ArgumentException)
// {
// fSuccess = false;
// }
// EvaluationStack.Push(fSuccess);
// }
// break;
//
// // Array
// case OpCode.ARRAYSIZE:
// {
// StackItem item = EvaluationStack.Pop();
// if (!item.IsArray)
// EvaluationStack.Push(item.GetByteArray().Length);
// else
// EvaluationStack.Push(item.GetArray().Count);
// }
// break;
// case OpCode.PACK:
// {
// int size = (int)EvaluationStack.Pop().GetBigInteger();
// if (size < 0 || size > EvaluationStack.Count)
// {
// State |= VMState.FAULT;
// return;
// }
// List<StackItem> items = new List<StackItem>(size);
// for (int i = 0; i < size; i++)
// items.Add(EvaluationStack.Pop());
// EvaluationStack.Push(items);
// }
// break;
// case OpCode.UNPACK:
// {
// StackItem item = EvaluationStack.Pop();
// if (!item.IsArray)
// {
// State |= VMState.FAULT;
// return;
// }
// IList<StackItem> items = item.GetArray();
// for (int i = items.Count - 1; i >= 0; i--)
// EvaluationStack.Push(items[i]);
// EvaluationStack.Push(items.Count);
// }
// break;
// case OpCode.PICKITEM:
// {
// int index = (int)EvaluationStack.Pop().GetBigInteger();
// if (index < 0)
// {
// State |= VMState.FAULT;
// return;
// }
// StackItem item = EvaluationStack.Pop();
// if (!item.IsArray)
// {
// State |= VMState.FAULT;
// return;
// }
// IList<StackItem> items = item.GetArray();
// if (index >= items.Count)
// {
// State |= VMState.FAULT;
// return;
// }
// EvaluationStack.Push(items[index]);
// }
// break;
// case OpCode.SETITEM:
// {
// StackItem newItem = EvaluationStack.Pop();
// if (newItem is Types.Struct s)
// {
// newItem = s.Clone();
// }
// int index = (int)EvaluationStack.Pop().GetBigInteger();
// StackItem arrItem = EvaluationStack.Pop();
// if (!arrItem.IsArray)
// {
// State |= VMState.FAULT;
// return;
// }
// IList<StackItem> items = arrItem.GetArray();
// if (index < 0 || index >= items.Count)
// {
// State |= VMState.FAULT;
// return;
// }
// items[index] = newItem;
// }
// break;
// case OpCode.NEWARRAY:
// {
// int count = (int)EvaluationStack.Pop().GetBigInteger();
// List<StackItem> items = new List<StackItem>(count);
// for (var i = 0; i < count; i++)
// {
// items.Add(false);
// }
// EvaluationStack.Push(new Types.Array(items));
// }
// break;
// case OpCode.NEWSTRUCT:
// {
// int count = (int)EvaluationStack.Pop().GetBigInteger();
// List<StackItem> items = new List<StackItem>(count);
// for (var i = 0; i < count; i++)
// {
// items.Add(false);
// }
// EvaluationStack.Push(new VM.Types.Struct(items));
// }
// break;
// case OpCode.APPEND:
// {
// StackItem newItem = EvaluationStack.Pop();
// if (newItem is Types.Struct s)
// {
// newItem = s.Clone();
// }
// StackItem arrItem = EvaluationStack.Pop();
// if (!arrItem.IsArray)
// {
// State |= VMState.FAULT;
// return;
// }
// IList<StackItem> items = arrItem.GetArray();
// items.Add(newItem);
// }
// break;
//
// case OpCode.REVERSE:
// {
// StackItem arrItem = EvaluationStack.Pop();
// if (!arrItem.IsArray)
// {
// State |= VMState.FAULT;
// return;
// }
// ((Types.Array)arrItem).Reverse();
// }
// break;
//
// // Exceptions
// case OpCode.THROW:
// State |= VMState.FAULT;
// return;
// case OpCode.THROWIFNOT:
// if (!EvaluationStack.Pop().GetBoolean())
// {
// State |= VMState.FAULT;
// return;
// }
// break;
//
// default:
// State |= VMState.FAULT;
// return;
// }
// if (!State.HasFlag(VMState.FAULT) && InvocationStack.Count > 0)
// {
// if
// (CurrentContext.BreakPoints.Contains((uint)CurrentContext.InstructionPointer))
// State |= VMState.BREAK;
// }
// }
//
// public void LoadScript(byte[] script, bool push_only = false)
// {
// InvocationStack.Push(new ExecutionContext(this, script, push_only));
// }
//
// public bool RemoveBreakPoint(uint position)
// {
// if (InvocationStack.Count == 0) return false;
// return CurrentContext.BreakPoints.Remove(position);
// }
//
// public void StepInto()
// {
// if (InvocationStack.Count == 0) State |= VMState.HALT;
// if (State.HasFlag(VMState.HALT) || State.HasFlag(VMState.FAULT)) return;
// OpCode opcode = CurrentContext.InstructionPointer >=
// CurrentContext.Script.Length ? OpCode.RET :
// (OpCode)CurrentContext.OpReader.ReadByte();
// try
// {
// ExecuteOp(opcode, CurrentContext);
// }
// catch
// {
// State |= VMState.FAULT;
// }
// }
//
// public void StepOut()
// {
// State &= ~VMState.BREAK;
// int c = InvocationStack.Count;
// while (!State.HasFlag(VMState.HALT) && !State.HasFlag(VMState.FAULT) &&
// !State.HasFlag(VMState.BREAK) && InvocationStack.Count >= c)
// StepInto();
// }
//
// public void StepOver()
// {
// if (State.HasFlag(VMState.HALT) || State.HasFlag(VMState.FAULT)) return;
// State &= ~VMState.BREAK;
// int c = InvocationStack.Count;
// do
// {
// StepInto();
// } while (!State.HasFlag(VMState.HALT) && !State.HasFlag(VMState.FAULT) &&
// !State.HasFlag(VMState.BREAK) && InvocationStack.Count > c);
// }
// }
// }
