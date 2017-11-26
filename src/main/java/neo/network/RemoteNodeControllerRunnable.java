package neo.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.model.core.Block;
import neo.model.network.InvPayload;
import neo.model.network.Message;
import neo.model.util.MapUtil;
import neo.model.util.PayloadUtil;
import neo.network.model.RemoteNodeData;

public class RemoteNodeControllerRunnable implements Runnable {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RemoteNodeControllerRunnable.class);

	private final ConcurrentLinkedQueue<Message> sendQueue = new ConcurrentLinkedQueue<>();

	private final LocalControllerNode localControllerNode;

	private final RemoteNodeData data;

	private boolean isGoodPeer = false;

	private boolean isAcknowledgedPeer = false;

	private Thread sendThread;

	private Thread receiveThread;

	private long inBytes = 0;

	private long outBytes = 0;

	private final Map<String, Long> apiCallMap = Collections.synchronizedMap(new TreeMap<>());

	public RemoteNodeControllerRunnable(final LocalControllerNode localControllerNode, final RemoteNodeData data) {
		this.localControllerNode = localControllerNode;
		this.data = data;
	}

	public Map<String, Long> getApiCallMap() {
		return apiCallMap;
	}

	public RemoteNodeData getData() {
		return data;
	}

	public String getHostAddress() {
		final InetSocketAddress peer = data.getTcpAddressAndPort();
		return peer.getAddress().getHostAddress();
	}

	public long getInBytes() {
		return inBytes;
	}

	public Message getMessageOrTimeOut(final CountingInputStream in) throws IOException {
		Message messageRecieved;
		try {
			messageRecieved = new Message(in);
		} catch (final SocketTimeoutException e) {
			messageRecieved = null;
		}
		return messageRecieved;
	}

	public long getOutBytes() {
		return outBytes;
	}

	public int getQueueDepth() {
		return sendQueue.size();
	}

	public boolean isAcknowledgedPeer() {
		return isAcknowledgedPeer;
	}

	public boolean isGoodPeer() {
		return isGoodPeer;
	}

	@Override
	public void run() {
		LOG.debug("STARTED RemoteNodeControllerRunnable run {}", getHostAddress());

		final long startTimeMs = System.currentTimeMillis();

		final long magic = localControllerNode.getLocalNodeData().getMagic();
		final int localPort = localControllerNode.getPort();
		final int nonce = localControllerNode.getNonce();
		final Block maxStartHeightBlock = localControllerNode.getLocalNodeData().getBlockDb().getBlockWithMaxIndex();
		final long startHeight;
		if (maxStartHeightBlock == null) {
			startHeight = 0;
		} else {
			startHeight = maxStartHeightBlock.getIndexAsLong();
		}

		sendQueue.add(new Message(magic, "version",
				PayloadUtil.getVersionPayload(localPort, nonce, startHeight).toByteArray()));
		sendQueue.add(new Message(magic, "verack"));
		try {
			try (Socket s = new Socket();) {
				s.setSoTimeout(2000);
				s.connect(data.getTcpAddressAndPort(), 2000);

				try (OutputStream sOut = s.getOutputStream();
						InputStream sIn = s.getInputStream();
						CountingOutputStream out = new CountingOutputStream(sOut);
						CountingInputStream in = new CountingInputStream(sIn);) {
					isGoodPeer = true;

					while (isGoodPeer) {
						outBytes = out.getByteCount();
						inBytes = in.getByteCount();
						Message messageToSend = sendQueue.poll();
						while (messageToSend != null) {
							final byte[] outBa = messageToSend.toByteArray();
							out.write(outBa);
							if (messageToSend.commandEnum != null) {
								final long apiCallCount;
								apiCallCount = MapUtil.increment(getApiCallMap(),
										"out-" + messageToSend.commandEnum.name().toLowerCase());
								MapUtil.increment(getApiCallMap(), "out-bytes", outBa.length);
								LOG.debug("request to {}:{} {}", getHostAddress(), messageToSend.command, apiCallCount);
							}
							messageToSend = sendQueue.poll();
						}
						out.flush();
						Thread.sleep(data.getSleepIntervalMs());

						Message messageRecieved = getMessageOrTimeOut(in);
						while (messageRecieved != null) {
							if (messageRecieved.magic != magic) {
								LOG.debug(" magic was {} expected {} closing peer.", messageRecieved.magic, magic);
								isGoodPeer = false;
							} else {
								MapUtil.increment(getApiCallMap(), "in-bytes",
										messageRecieved.getPayloadByteArray().length + 24);
								if (messageRecieved.commandEnum != null) {
									final long apiCallCount;
									final String apiCallRoot = "in-" + messageRecieved.commandEnum.name().toLowerCase();
									if (messageRecieved.commandEnum.equals(CommandEnum.INV)) {
										final InvPayload payload = messageRecieved.getPayload(InvPayload.class);
										final String apiCall = apiCallRoot + "-"
												+ payload.getType().name().toLowerCase();
										apiCallCount = MapUtil.increment(getApiCallMap(), apiCall);
									} else {
										apiCallCount = MapUtil.increment(getApiCallMap(), apiCallRoot);
									}
									LOG.debug("response from {}:{} {}", getHostAddress(), messageRecieved.command,
											apiCallCount);
								}

								localControllerNode.onMessage(RemoteNodeControllerRunnable.this, messageRecieved);
							}
							messageRecieved = getMessageOrTimeOut(in);
						}

						final long currTimeMs = System.currentTimeMillis();
						final long recycleTimeMs = startTimeMs + data.getRecycleIntervalMs();
						if (recycleTimeMs < currTimeMs) {
							LOG.debug("recycling remote node {}", getHostAddress());
							isGoodPeer = false;
						}

						if (isGoodPeer) {
							Thread.sleep(data.getSleepIntervalMs());
						}
					}
				}
			} catch (final SocketTimeoutException e) {
				LOG.trace("SocketTimeoutException from {}, closing peer", getHostAddress());
				LOG.trace("SocketTimeoutException", e);
				isGoodPeer = false;
			} catch (final ConnectException e) {
				LOG.trace("ConnectException from {}, closing peer", getHostAddress());
				LOG.trace("ConnectException", e);
				isGoodPeer = false;
			} catch (final SocketException e) {
				if (e.getMessage().equals("Broken pipe (Write failed)")) {
					LOG.trace("SocketException from {}, closing peer", getHostAddress());
				} else if (e.getMessage().equals("Operation timed out (Read failed)")) {
					LOG.trace("SocketException from {}, closing peer", getHostAddress());
				} else if (e.getMessage().equals("Connection reset")) {
					LOG.trace("SocketException from {}, closing peer", getHostAddress());
				} else if (e.getMessage().equals("Network is unreachable (connect failed)")) {
					LOG.trace("SocketException from {}, closing peer", getHostAddress());
				} else if (e.getMessage().equals("Protocol wrong type for socket (Write failed)")) {
					LOG.trace("SocketException from {}, closing peer", getHostAddress());
				} else {
					LOG.error("SocketException from {}, closing peer", getHostAddress());
					LOG.error("SocketException", e);
				}
				isGoodPeer = false;
			}
		} catch (

		final Exception e) {
			LOG.error("error", e);
			LOG.debug("FAILURE RemoteNodeControllerRunnable run");
			localControllerNode.OnSocketClose(RemoteNodeControllerRunnable.this);
			return;
		}
		localControllerNode.OnSocketClose(RemoteNodeControllerRunnable.this);
		LOG.debug("SUCCESS RemoteNodeControllerRunnable run");
	}

	public void send(final Message message) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("send to {}:{}", getHostAddress(), message.command);
		}
		sendQueue.add(message);
	}

	public void setAcknowledgedPeer(final boolean isAcknowledgedPeer) {
		this.isAcknowledgedPeer = isAcknowledgedPeer;
	}

	public void stop() throws InterruptedException {
		isGoodPeer = false;
		if (sendThread != null) {
			sendThread.join();
		}
		if (receiveThread != null) {
			receiveThread.join();
		}
	}

}
