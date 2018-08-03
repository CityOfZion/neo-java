package neo.vm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;

import neo.model.util.NetworkUtil;

/**
 * the script builder.
 *
 * @author coranos
 *
 */
public class ScriptBuilder {

	/**
	 * the writer.
	 */
	private final ByteArrayOutputStream writer;

	/**
	 * the constructor.
	 */
	public ScriptBuilder() {
		writer = new ByteArrayOutputStream();
	}

	/**
	 * emits a opcode and argument.
	 *
	 * @param op
	 *            the opcode.
	 * @param arg
	 *            th argument.
	 * @return this.
	 */
	public ScriptBuilder emit(final OpCode op, final byte... arg) {
		write(op.getTypeByte());
		if (arg != null) {
			write(arg);
		}
		return this;
	}

	/**
	 * emit an app call.
	 *
	 * @param scriptHash
	 *            the script hash to use.
	 * @param useTailCall
	 *            if true, sue a tail call.
	 * @return this.
	 */
	public ScriptBuilder emitAppCall(final byte[] scriptHash, final boolean useTailCall) {
		if (scriptHash.length != 20) {
			throw new IllegalArgumentException("(scriptHash.length != 20)");
		}
		return emit(useTailCall ? OpCode.TAILCALL : OpCode.APPCALL, scriptHash);
	}

	/**
	 * emit a jump.
	 *
	 * @param op
	 *            the opcode to use.
	 * @param offset
	 *            the offset to use.
	 * @return this.
	 */
	public ScriptBuilder emitJump(final OpCode op, final short offset) {
		if ((op != OpCode.JMP) && (op != OpCode.JMPIF) && (op != OpCode.JMPIFNOT) && (op != OpCode.CALL)) {
			throw new IllegalArgumentException("unknown OpCode for emit:" + op);
		}
		return emit(op, NetworkUtil.getShortByteArray(offset));
	}

	/**
	 * emit a push.
	 *
	 * @param number
	 *            the number to push.
	 * @return this.
	 */
	public ScriptBuilder emitPush(final BigInteger number) {
		if (number.equals(BigInteger.valueOf(-1))) {
			return emit(OpCode.PUSHM1);
		}
		if (number.equals(BigInteger.ZERO)) {
			return emit(OpCode.PUSH0);
		}

		if (number.compareTo(BigInteger.ZERO) >= 0) {
			if (number.compareTo(BigInteger.valueOf(16)) <= 0) {
				final int opCodeByte = (OpCode.PUSH1.getTypeByte() - 1) + number.byteValue();
				return emit(OpCode.valueOfByte((byte) opCodeByte));
			}
		}
		return emitPush(number.toByteArray());
	}

	/**
	 * emits a push.
	 *
	 * @param data
	 *            the data to push.
	 * @return this.
	 */
	public ScriptBuilder emitPush(final boolean data) {
		return emit(data ? OpCode.PUSH1 : OpCode.PUSH0);
	}

	/**
	 * emits a push.
	 *
	 * @param data
	 *            the data to push.
	 * @return this.
	 */
	public ScriptBuilder emitPush(final byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("(data == null)");
		}
		if (data.length <= OpCode.PUSHBYTES75.getTypeByte()) {
			write((byte) data.length);
			write(data);
		} else if (data.length < 0x100) {
			emit(OpCode.PUSHDATA1);
			write((byte) data.length);
			write(data);
		} else if (data.length < 0x10000) {
			emit(OpCode.PUSHDATA2);
			write(NetworkUtil.getShortByteArray((short) data.length));
			write(data);
		} else {
			emit(OpCode.PUSHDATA4);
			write(NetworkUtil.getIntByteArray(data.length));
			write(data);
		}
		return this;
	}

	/**
	 * emit a push.
	 *
	 * @param data
	 *            the data to push.
	 * @return this.
	 */
	public ScriptBuilder emitPush(final String data) {
		return emitPush(data.getBytes(Charset.forName("UTF8")));
	}

	/**
	 * emits a system call.
	 *
	 * @param api
	 *            the api to call.
	 * @return this.
	 */
	public ScriptBuilder emitSysCall(final String api) {
		if (api == null) {
			throw new IllegalArgumentException("(api == null)");
		}
		final byte[] apiBytes = api.getBytes(Charset.forName("ASCII"));
		if ((apiBytes.length == 0) || (apiBytes.length > 252)) {
			throw new IllegalArgumentException("api_bytes.length not between 0 and 252:" + apiBytes.length);
		}
		final byte[] arg = new byte[apiBytes.length + 1];
		arg[0] = (byte) apiBytes.length;
		System.arraycopy(apiBytes, 0, arg, 1, apiBytes.length);
		return emit(OpCode.SYSCALL, arg);
	}

	/**
	 * return the offset.
	 *
	 * @return the offset.
	 */
	public int getOffset() {
		return writer.size();
	}

	/**
	 * returns an array.
	 *
	 * @return the array.
	 */
	public byte[] toByteArray() {
		return writer.toByteArray();
	}

	/**
	 * writes the bytes to the writer.
	 *
	 * @param arg
	 *            the bytes to write.
	 */
	private void write(final byte... arg) {
		try {
			writer.write(arg);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}

// using System;
// using System.IO;
// using System.Numerics;
// using System.Text;
//
// namespace Neo.VM
// {
// public class ScriptBuilder : IDisposable
// {
// private readonly MemoryStream ms = new MemoryStream();
// private readonly BinaryWriter writer;
//
// public int Offset => (int)ms.Position;
//
// public ScriptBuilder()
// {
// this.writer = new BinaryWriter(ms);
// }
//
// public void Dispose()
// {
// writer.Dispose();
// ms.Dispose();
// }
//
// public ScriptBuilder Emit(OpCode op, byte[] arg = null)
// {
// writer.Write((byte)op);
// if (arg != null)
// writer.Write(arg);
// return this;
// }
//
// public ScriptBuilder EmitAppCall(byte[] scriptHash, bool useTailCall = false)
// {
// if (scriptHash.Length != 20)
// throw new ArgumentException();
// return Emit(useTailCall ? OpCode.TAILCALL : OpCode.APPCALL, scriptHash);
// }
//
// public ScriptBuilder EmitJump(OpCode op, short offset)
// {
// if (op != OpCode.JMP && op != OpCode.JMPIF && op != OpCode.JMPIFNOT && op !=
// OpCode.CALL)
// throw new ArgumentException();
// return Emit(op, BitConverter.GetBytes(offset));
// }
//
// public ScriptBuilder EmitPush(BigInteger number)
// {
// if (number == -1) return Emit(OpCode.PUSHM1);
// if (number == 0) return Emit(OpCode.PUSH0);
// if (number > 0 && number <= 16) return Emit(OpCode.PUSH1 - 1 + (byte)number);
// return EmitPush(number.ToByteArray());
// }
//
// public ScriptBuilder EmitPush(bool data)
// {
// return Emit(data ? OpCode.PUSHT : OpCode.PUSHF);
// }
//
// public ScriptBuilder EmitPush(byte[] data)
// {
// if (data == null)
// throw new ArgumentNullException();
// if (data.Length <= (int)OpCode.PUSHBYTES75)
// {
// writer.Write((byte)data.Length);
// writer.Write(data);
// }
// else if (data.Length < 0x100)
// {
// Emit(OpCode.PUSHDATA1);
// writer.Write((byte)data.Length);
// writer.Write(data);
// }
// else if (data.Length < 0x10000)
// {
// Emit(OpCode.PUSHDATA2);
// writer.Write((ushort)data.Length);
// writer.Write(data);
// }
// else// if (data.Length < 0x100000000L)
// {
// Emit(OpCode.PUSHDATA4);
// writer.Write(data.Length);
// writer.Write(data);
// }
// return this;
// }
//
// public ScriptBuilder EmitPush(string data)
// {
// return EmitPush(Encoding.UTF8.GetBytes(data));
// }
//
// public ScriptBuilder EmitSysCall(string api)
// {
// if (api == null)
// throw new ArgumentNullException();
// byte[] api_bytes = Encoding.ASCII.GetBytes(api);
// if (api_bytes.Length == 0 || api_bytes.Length > 252)
// throw new ArgumentException();
// byte[] arg = new byte[api_bytes.Length + 1];
// arg[0] = (byte)api_bytes.Length;
// Buffer.BlockCopy(api_bytes, 0, arg, 1, api_bytes.Length);
// return Emit(OpCode.SYSCALL, arg);
// }
//
// public byte[] ToArray()
// {
// writer.Flush();
// return ms.ToArray();
// }
// }
// }
