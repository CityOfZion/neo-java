package neo.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.network.InvPayload;
import neo.model.network.Message;
import neo.model.network.exception.MessageFormatException;
import neo.model.util.MapUtil;
import neo.model.util.PayloadUtil;
import neo.model.util.threadpool.StopRunnable;
import neo.network.model.LocalNodeData;
import neo.network.model.RemoteNodeData;
import neo.network.model.socket.SocketWrapper;

/**
 * the controller class for remote nodes.
 *
 * @author coranos
 *
 */
public final class RemoteNodeControllerRunnable implements StopRunnable {

	/**
	 * a dash.
	 */
	private static final String DASH = "-";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RemoteNodeControllerRunnable.class);

	/**
	 * the local controller node.
	 */
	private final LocalControllerNode localControllerNode;

	/**
	 * the data for the remote node.
	 */
	private final RemoteNodeData data;

	/**
	 * the constructor.
	 *
	 * @param localControllerNode
	 *            the local controlelr node to use.
	 * @param data
	 *            the remote node data to use.
	 */
	public RemoteNodeControllerRunnable(final LocalControllerNode localControllerNode, final RemoteNodeData data) {
		this.localControllerNode = localControllerNode;
		this.data = data;
	}

	/**
	 * return the remote node data.
	 *
	 * @return the remote node data.
	 */
	public RemoteNodeData getData() {
		return data;
	}

	/**
	 * returns the message, or returns null if there's a SocketTimeoutException.
	 *
	 * @param readTimeOut
	 *            teh read time out.
	 * @param in
	 *            the input stream to read.
	 *
	 * @return the message, or returns null if there's a SocketTimeoutException.
	 * @throws IOException
	 *             if an error occurs.
	 */
	private Message getMessageOrTimeOut(final long readTimeOut, final InputStream in) throws IOException {
		Message messageRecieved;
		try {
			messageRecieved = new Message(readTimeOut, in);
		} catch (final SocketTimeoutException e) {
			LOG.trace("SocketTimeoutException[1] from {}, closing peer", data.getHostAddress());
			LOG.trace("SocketTimeoutException[1]", e);
			messageRecieved = null;
		} catch (final InterruptedIOException e) {
			LOG.trace("InterruptedIOException from {}, closing peer", data.getHostAddress());
			LOG.trace("InterruptedIOException", e);
			data.setGoodPeer(false);
			messageRecieved = null;
		} catch (final IOException e) {
			if (e.getMessage() == null) {
				throw new RuntimeException(e);
			} else if (e.getMessage().equals("Connection reset by peer")) {
				LOG.trace("IOException[1] from {}, \"{}\" closing peer", e.getMessage(), data.getHostAddress());
				LOG.trace("IOException[1]", e);
				data.setGoodPeer(false);
				messageRecieved = null;
			} else if (e.getMessage().equals("Operation timed out")) {
				messageRecieved = null;
			} else {
				throw new RuntimeException(e);
			}
		} catch (final RuntimeException e) {
			LOG.trace("RuntimeException from {}, closing peer", data.getHostAddress());
			data.setGoodPeer(false);
			messageRecieved = null;
			throw e;
		}
		return messageRecieved;
	}

	/**
	 * recieve messages.
	 *
	 * @param readTimeOut
	 *            the read timeout.
	 * @param magic
	 *            the magic number to check for valid messages.
	 * @param in
	 *            the input stream.
	 * @throws IOException
	 *             if an error occurs.
	 */
	private void recieveMessages(final long readTimeOut, final long magic, final InputStream in) throws IOException {
		Message messageRecieved = getMessageOrTimeOut(readTimeOut, in);
		while (messageRecieved != null) {
			if (messageRecieved.magic != magic) {
				LOG.debug(" magic was {} expected {} closing peer.", messageRecieved.magic, magic);
				data.setGoodPeer(false);
			} else {
				MapUtil.increment(LocalNodeData.API_CALL_MAP, RemoteNodeData.IN_BYTES,
						messageRecieved.getPayloadByteArray().length + 24);
				if (messageRecieved.commandEnum != null) {
					final long apiCallCount;
					final String apiCallRoot = "in-" + messageRecieved.commandEnum.name().toLowerCase();
					if (messageRecieved.commandEnum.equals(CommandEnum.TX)) {
						final Transaction tx = messageRecieved.getPayload(Transaction.class);
						final String apiCall = apiCallRoot + DASH + tx.type.name().toLowerCase();
						apiCallCount = MapUtil.increment(LocalNodeData.API_CALL_MAP, apiCall);
					} else if (messageRecieved.commandEnum.equals(CommandEnum.INV)) {
						final InvPayload payload = messageRecieved.getPayload(InvPayload.class);
						final String apiCall = apiCallRoot + DASH + payload.getType().name().toLowerCase();
						final String apiCallHash = apiCallRoot + DASH + payload.getType().name().toLowerCase()
								+ "-hashes";
						apiCallCount = MapUtil.increment(LocalNodeData.API_CALL_MAP, apiCall);
						MapUtil.increment(LocalNodeData.API_CALL_MAP, apiCallHash, payload.getHashes().size());
					} else {
						apiCallCount = MapUtil.increment(LocalNodeData.API_CALL_MAP, apiCallRoot);
					}
					LOG.debug("response from {}:{} {}", data.getHostAddress(), messageRecieved.command, apiCallCount);
				}

				localControllerNode.onMessage(RemoteNodeControllerRunnable.this, messageRecieved);
			}
			if (!data.isGoodPeer()) {
				return;
			}
			messageRecieved = getMessageOrTimeOut(readTimeOut, in);
		}
	}

	/**
	 * the run method.
	 */
	@Override
	public void run() {
		LOG.debug("STARTED RemoteNodeControllerRunnable run {}", data.getHostAddress());
		final long startTimeMs = System.currentTimeMillis();
		final LocalNodeData localNodeData = localControllerNode.getLocalNodeData();
		final long readTimeOut = localNodeData.getRpcServerTimeoutMillis();
		final long magic = localNodeData.getMagic();
		final int localPort = localNodeData.getTcpPort();
		final int nonce = localNodeData.getNonce();
		final Block maxStartHeightBlock = localNodeData.getBlockDb().getHeaderOfBlockWithMaxIndex();
		final long startHeight;
		if (maxStartHeightBlock == null) {
			startHeight = 0;
		} else {
			startHeight = maxStartHeightBlock.getIndexAsLong();
		}

		data.getSendQueue().add(new Message(magic, CommandEnum.VERSION,
				PayloadUtil.getVersionPayload(localPort, nonce, startHeight).toByteArray()));
		data.getSendQueue().add(new Message(magic, CommandEnum.VERACK));
		try {
			try (SocketWrapper s = localNodeData.getSocketFactory().newSocketWrapper()) {
				s.setSoTimeout(2000);
				s.connect(data.getTcpAddressAndPort(), 2000);

				try (OutputStream out = s.getOutputStream(); InputStream in = s.getInputStream();) {
					data.setGoodPeer(true);

					while (data.isGoodPeer()) {
						sendMessages(out);
						out.flush();
						try {
							Thread.sleep(data.getSleepIntervalMs());
						} catch (final InterruptedException e) {
							LOG.debug("InterruptedException[1], stopping. {}", e.getMessage());
							data.setGoodPeer(false);
						}
						if (data.isGoodPeer()) {
							recieveMessages(readTimeOut, magic, in);
						}

						final long currTimeMs = System.currentTimeMillis();
						final long recycleTimeMs = startTimeMs + data.getRecycleIntervalMs();
						if (recycleTimeMs < currTimeMs) {
							LOG.debug("recycling remote node {}", data.getHostAddress());
							data.setGoodPeer(false);
						}

						if (data.isGoodPeer()) {
							try {
								Thread.sleep(data.getSleepIntervalMs());
							} catch (final InterruptedException e) {
								LOG.debug("InterruptedException[2], stopping. {}", e.getMessage());
								data.setGoodPeer(false);
							}
						}
					}
				}
			} catch (final SocketTimeoutException e) {
				LOG.trace("SocketTimeoutException[2] from {}, closing peer", data.getHostAddress());
				LOG.trace("SocketTimeoutException[2]", e);
				data.setGoodPeer(false);
			} catch (final ConnectException e) {
				LOG.trace("ConnectException from {}, closing peer", data.getHostAddress());
				LOG.trace("ConnectException", e);
				data.setGoodPeer(false);
			} catch (final MessageFormatException e) {
				LOG.error("MessageFormatException from {}, closing peer", data.getHostAddress());
				LOG.error("MessageFormatException", e);
				data.setGoodPeer(false);
			} catch (final SocketException e) {
				if (e.getMessage().equals("Broken pipe (Write failed)")) {
					LOG.trace("SocketException from {}, broken pipe, closing peer", data.getHostAddress());
				} else if (e.getMessage().equals("Operation timed out (Read failed)")) {
					LOG.trace("SocketException from {}, timeout, closing peer", data.getHostAddress());
				} else if (e.getMessage().equals("Connection reset")) {
					LOG.trace("SocketException from {}, connection reset, closing peer", data.getHostAddress());
				} else if (e.getMessage().equals("Network is unreachable (connect failed)")) {
					LOG.trace("SocketException from {}, unreachable network, closing peer", data.getHostAddress());
				} else if (e.getMessage().equals("Protocol wrong type for socket (Write failed)")) {
					LOG.trace("SocketException from {}, wrong protocol, closing peer", data.getHostAddress());
				} else {
					LOG.error("SocketException from {}, closing peer", data.getHostAddress());
					LOG.error("SocketException", e);
				}
				data.setGoodPeer(false);
			} catch (final ClosedChannelException e) {
				LOG.trace("ClosedChannelException from {}, closing peer", data.getHostAddress());
				data.setGoodPeer(false);
			}
		} catch (final Exception e) {
			LOG.error("error", e);
			LOG.debug("FAILURE RemoteNodeControllerRunnable run");
			localControllerNode.onSocketClose(RemoteNodeControllerRunnable.this);
			return;
		}
		localControllerNode.onSocketClose(RemoteNodeControllerRunnable.this);
		LOG.debug("SUCCESS RemoteNodeControllerRunnable run");
	}

	/**
	 * send messages.
	 *
	 * @param out
	 *            the output stream to use.
	 * @throws IOException
	 *             if an error occurs.
	 */
	private void sendMessages(final OutputStream out) throws IOException {
		Message messageToSend = data.getSendQueue().poll();
		while (messageToSend != null) {
			if (!data.isGoodPeer()) {
				return;
			}
			final byte[] outBa = messageToSend.toByteArray();
			try {
				out.write(outBa);
			} catch (final SocketTimeoutException e) {
				LOG.trace("SocketTimeoutException[3] from {}, closing peer", data.getHostAddress());
				LOG.trace("SocketTimeoutException[3]", e);
				data.setGoodPeer(false);
				return;
			} catch (final IOException e) {
				if (e.getMessage().equals("Broken pipe")) {
					LOG.trace("IOException[2] from {}, \"{}\" closing peer", e.getMessage(), data.getHostAddress());
					LOG.trace("IOException[2]", e);
					data.setGoodPeer(false);
					return;
				} else {
					throw new RuntimeException(e);
				}
			}
			if (messageToSend.commandEnum != null) {
				final long apiCallCount;
				apiCallCount = MapUtil.increment(LocalNodeData.API_CALL_MAP,
						"out-" + messageToSend.commandEnum.name().toLowerCase());
				MapUtil.increment(LocalNodeData.API_CALL_MAP, RemoteNodeData.OUT_BYTES, outBa.length);
				LOG.debug("request to {}:{} {}", data.getHostAddress(), messageToSend.command, apiCallCount);
			}
			messageToSend = data.getSendQueue().poll();
		}
	}

	/**
	 * stops the run method.
	 */
	@Override
	public void stop() {
		data.setGoodPeer(false);
		data.getSendQueue().clear();
	}

}
