package neo.model.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.model.bytes.UInt32;
import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.util.InputStreamUtil;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;
import neo.model.util.SHA256HashUtil;

/**
 * the message object.
 *
 * @author coranos
 *
 */
public final class Message {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Message.class);

	/**
	 * the magic.
	 */
	public final long magic;

	/**
	 * the command.
	 */
	public final String command;

	/**
	 * the payload, as a byte array.
	 */
	private final byte[] payloadBa;

	/**
	 * the payload.
	 */
	public final Payload payload;

	/**
	 * the command, as an enum.
	 */
	public final CommandEnum commandEnum;

	/**
	 * the constructor.
	 *
	 * @param bb
	 *            the byte buffer to read.
	 */
	public Message(final ByteBuffer bb) {
		final UInt32 magicObj = ModelUtil.getUInt32(bb);
		magic = magicObj.toPositiveBigInteger().intValue();
		LOG.trace("interim[1] inSocket magicObj:{} magic:{}", magicObj, magic);
		command = ModelUtil.getFixedLengthString(bb, 12).trim();
		LOG.trace("interim[1] inSocket command:{}", command);
		final UInt32 lengthObj = ModelUtil.getUInt32(bb);
		final int length = lengthObj.toPositiveBigInteger().intValue();
		LOG.trace("interim inSocket lengthObj:{} length:{}", lengthObj, length);
		final UInt32 checksum = ModelUtil.getUInt32(bb);
		LOG.trace("interim[1] inSocket checksum:{}", checksum);
		payloadBa = ModelUtil.getFixedLengthByteArray(bb, length, false);
		payload = createPayload();
		commandEnum = CommandEnum.fromName(command);
	}

	/**
	 * the constructor.
	 *
	 * @param magic
	 *            the magic to use.
	 * @param command
	 *            the command to use.
	 * @param payloadBa
	 *            the payload byte array.
	 */
	public Message(final long magic, final CommandEnum command, final byte... payloadBa) {
		this.magic = magic;
		this.command = command.getName();
		this.payloadBa = payloadBa;
		payload = createPayload();
		commandEnum = CommandEnum.fromName(this.command);
	}

	/**
	 * the constructor.
	 *
	 * @param readTimeOut
	 *            the amount of time to wait for a read timeout, in milliseconds.
	 * @param in
	 *            the input stream to read.
	 * @throws IOException
	 *             if an error occurs.
	 */
	public Message(final long readTimeOut, final InputStream in) throws IOException {
		final byte[] headerBa = new byte[24];
		InputStreamUtil.readUntilFull(readTimeOut, in, headerBa);
		final ByteBuffer headerBb = ByteBuffer.wrap(headerBa);
		final UInt32 magicObj = ModelUtil.getUInt32(headerBb);
		magic = magicObj.toPositiveBigInteger().intValue();
		LOG.trace("interim[2] inSocket magicObj:{} magic:{}", magicObj, magic);
		command = ModelUtil.getFixedLengthString(headerBb, 12).trim();
		commandEnum = CommandEnum.fromName(command);
		LOG.trace("interim[2] inSocket command:{}", command);
		final UInt32 lengthObj = ModelUtil.getUInt32(headerBb);
		final int lengthRaw = lengthObj.toPositiveBigInteger().intValue();
		final int length;
		if (lengthRaw < 0) {
			LOG.debug("command:{};lengthRaw < 0:{};", command, lengthRaw);
			length = 0;
		} else {
			length = lengthRaw;
		}
		final UInt32 checksum = ModelUtil.getUInt32(headerBb);
		LOG.trace("interim[2] inSocket checksum:{}", checksum);
		final byte[] payloadBa;
		if (commandEnum == null) {
			throw new SocketTimeoutException();
		} else {
			try {
				payloadBa = new byte[length];
			} catch (final OutOfMemoryError e) {
				LOG.error("OutOfMemoryError getting command \"{}\" payload of size:{}", command, length);
				throw e;
			}
			InputStreamUtil.readUntilFull(readTimeOut, in, payloadBa);
		}
		final ByteBuffer payloadBb = ByteBuffer.wrap(payloadBa);
		this.payloadBa = ModelUtil.getFixedLengthByteArray(payloadBb, length, false);
		payload = createPayload();
	}

	/**
	 * return the payload.
	 *
	 *
	 * @param <T>
	 *            the payload type.
	 * @return the payload.
	 */
	private <T extends Payload> Payload createPayload() {
		return createPayload(Payload.class);
	}

	/**
	 * creates a payload.
	 *
	 * @param cl
	 *            the payload class.
	 * @param <T>
	 *            the payload type.
	 * @return the payload.
	 */
	@SuppressWarnings("unchecked")
	private <T extends Payload> T createPayload(final Class<T> cl) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("initPayload payloadBa {}", Hex.encodeHexString(payloadBa));
		}
		final Payload payload;
		// TODO: make a case statement.
		if ("version".equals(command)) {
			payload = new VersionPayload(ByteBuffer.wrap(payloadBa));
		} else if ("inv".equals(command)) {
			payload = new InvPayload(ByteBuffer.wrap(payloadBa));
		} else if ("addr".equals(command)) {
			payload = new AddrPayload(ByteBuffer.wrap(payloadBa));
		} else if ("headers".equals(command)) {
			payload = new HeadersPayload(ByteBuffer.wrap(payloadBa));
		} else if ("verack".equals(command)) {
			payload = null;
		} else if ("getaddr".equals(command)) {
			payload = null;
		} else if ("getdata".equals(command)) {
			payload = null;
		} else if ("getblocks".equals(command)) {
			payload = null;
		} else if ("mempool".equals(command)) {
			payload = null;
		} else if ("".equals(command)) {
			payload = null;
		} else if ("getheaders".equals(command)) {
			payload = null;
		} else if ("block".equals(command)) {
			payload = new Block(ByteBuffer.wrap(payloadBa));
		} else if ("tx".equals(command)) {
			payload = new Transaction(ByteBuffer.wrap(payloadBa));
		} else if (!command.matches("[a-z]+")) {
			LOG.debug("unknown payload type for non alphabetic command \"{}\"", command);
			payload = null;
		} else {
			LOG.error("unknown payload type for command \"{}\"", command);
			payload = null;
		}
		return (T) payload;
	}

	/**
	 * return the payload.
	 *
	 * @param cl
	 *            the payload class.
	 * @param <T>
	 *            the payload type.
	 * @return the payload.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Payload> T getPayload(final Class<T> cl) {
		return (T) payload;
	}

	/**
	 * return the payload byte array.
	 *
	 * @return the payload byte array.
	 */
	public byte[] getPayloadByteArray() {
		return payloadBa;
	}

	/**
	 * return the message as a byte array.
	 *
	 * @return the message as a byte array.
	 * @throws IOException
	 *             if an error occurs.
	 * @throws UnsupportedEncodingException
	 *             if an error occurs.
	 */
	public byte[] toByteArray() throws IOException, UnsupportedEncodingException {
		final byte[] magicBa = NetworkUtil.getIntByteArray(magic);
		ArrayUtils.reverse(magicBa);
		final UInt32 magicObj = new UInt32(magicBa);
		final byte[] checksumFull = SHA256HashUtil.getDoubleSHA256Hash(payloadBa);
		final byte[] checksum = new byte[4];
		System.arraycopy(checksumFull, 0, checksum, 0, 4);
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		bout.write(magicObj.getBytesCopy());
		NetworkUtil.writeString(bout, 12, command);

		if (LOG.isTraceEnabled()) {
			LOG.trace("createMessage magic+command {}", Hex.encodeHexString(bout.toByteArray()));
		}
		final byte[] lengthBa = NetworkUtil.getIntByteArray(payloadBa.length);
		ArrayUtils.reverse(lengthBa);
		if (LOG.isTraceEnabled()) {
			LOG.trace("createMessage lengthBa {}", Hex.encodeHexString(lengthBa));
		}
		bout.write(lengthBa);
		if (LOG.isTraceEnabled()) {
			LOG.trace("createMessage magic+command+length {}", Hex.encodeHexString(bout.toByteArray()));
			LOG.trace("createMessage checksum {}", Hex.encodeHexString(checksum));
		}
		bout.write(checksum);
		if (LOG.isTraceEnabled()) {
			LOG.trace("createMessage magic+command+length+checksum {}", Hex.encodeHexString(bout.toByteArray()));
		}
		bout.write(payloadBa);
		if (LOG.isTraceEnabled()) {
			LOG.trace("createMessage payloadBa {}", Hex.encodeHexString(payloadBa));
			LOG.trace("createMessage magic+command+length+checksum+payload {}",
					Hex.encodeHexString(bout.toByteArray()));
		}
		return bout.toByteArray();
	}

	@Override
	public String toString() {
		final JSONObject json = new JSONObject();
		json.put("magic", magic);
		json.put("command", command);

		if (payload == null) {
			json.put("payloadHex", Hex.encodeHexString(payloadBa));
		} else {
			json.put("payload", payload);
		}
		return json.toString();
	}
}
