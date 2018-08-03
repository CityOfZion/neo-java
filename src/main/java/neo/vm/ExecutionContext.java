package neo.vm;

import java.nio.ByteBuffer;
import java.util.HashSet;

/**
 * the execution context.
 *
 * @author coranos
 *
 */
public class ExecutionContext {

	/**
	 * the engine.
	 */
	private final ExecutionEngine engine;

	/**
	 * if true, push only.
	 */
	public final boolean pushOnly;

	/**
	 * the opcode reader.
	 */
	private final ByteBuffer opReader;

	/**
	 * breakpoints.
	 */
	private final HashSet<Integer> breakPoints;

	/**
	 * the script.
	 */
	private final byte[] script;

	/**
	 * the constructor.
	 *
	 * @param engine
	 *            the engine to use.
	 * @param script
	 *            the script to use.
	 * @param pushOnly
	 *            if true, push only.
	 * @param breakPoints
	 *            breakpoints, if any.
	 */
	public ExecutionContext(final ExecutionEngine engine, final byte[] script, final boolean pushOnly,
			final HashSet<Integer> breakPoints) {
		this.engine = engine;
		this.script = script;
		this.pushOnly = pushOnly;
		opReader = ByteBuffer.wrap(script);
		this.breakPoints = breakPoints;
	}

	/**
	 * return the list of breakpoints.
	 *
	 * @return the list of breakpoints.
	 */
	public HashSet<Integer> getBreakPoints() {
		return breakPoints;
	}

	/**
	 * return a clone of the context.
	 *
	 * @return a clone of the context.
	 */
	public ExecutionContext getClone() {
		final ExecutionContext ec = new ExecutionContext(engine, script, pushOnly, breakPoints);
		ec.setInstructionPointer(getInstructionPointer());
		return ec;
	}

	/**
	 * return the instruction pointer.
	 *
	 * @return the instruction pointer.
	 */
	public int getInstructionPointer() {
		return opReader.position();
	}

	/**
	 * return the next instruction.
	 *
	 * @return the next instruction.
	 */
	public OpCode getNextInstruction() {
		return OpCode.valueOfByte(script[opReader.position()]);
	}

	public ByteBuffer getOpReader() {
		return opReader;
	}

	/**
	 * return the scrypt.
	 *
	 * @return the scrypt.
	 */
	public byte[] getScript() {
		return script;
	}

	/**
	 * return the script hash.
	 *
	 * @return the script hash.
	 */
	public byte[] getScriptHash() {
		return engine.getCrypto().hash160(script);
	}

	/**
	 * set the instruction pointer to the given position.
	 *
	 * @param position
	 *            the position to use.
	 */
	public void setInstructionPointer(final int position) {
		opReader.position(position);
	}
}

// using System;
// using System.Collections.Generic;
// using System.IO;
//
// namespace Neo.VM
// {
// public class ExecutionContext : IDisposable
// {
// private ExecutionEngine engine;
// public readonly byte[] Script;
// public readonly bool PushOnly;
// internal readonly BinaryReader OpReader;
// internal readonly HashSet<uint> BreakPoints;
//
// public int InstructionPointer
// {
// get
// {
// return (int)OpReader.BaseStream.Position;
// }
// set
// {
// OpReader.BaseStream.Seek(value, SeekOrigin.Begin);
// }
// }
//
// public OpCode NextInstruction =>
// (OpCode)Script[OpReader.BaseStream.Position];
//
// private byte[] _script_hash = null;
// public byte[] ScriptHash
// {
// get
// {
// if (_script_hash == null)
// _script_hash = engine.Crypto.Hash160(Script);
// return _script_hash;
// }
// }
//
// internal ExecutionContext(ExecutionEngine engine, byte[] script, bool
// push_only, HashSet<uint> break_points = null)
// {
// this.engine = engine;
// this.Script = script;
// this.PushOnly = push_only;
// this.OpReader = new BinaryReader(new MemoryStream(script, false));
// this.BreakPoints = break_points ?? new HashSet<uint>();
// }
//
// public ExecutionContext Clone()
// {
// return new ExecutionContext(engine, Script, PushOnly, BreakPoints)
// {
// InstructionPointer = InstructionPointer
// };
// }
//
// public void Dispose()
// {
// OpReader.Dispose();
// }
// }
// }
