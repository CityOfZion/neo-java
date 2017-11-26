package neo.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.model.core.Block;
import neo.model.network.InvPayload;
import neo.model.network.Message;
import neo.model.util.MapUtil;
import neo.model.util.PayloadUtil;
import neo.network.model.LocalNodeData;
import neo.network.model.RemoteNodeData;

public class RemoteNodeControllerRunnable implements Runnable {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RemoteNodeControllerRunnable.class);

	private final LocalControllerNode localControllerNode;

	private final RemoteNodeData data;

	public RemoteNodeControllerRunnable(final LocalControllerNode localControllerNode, final RemoteNodeData data) {
		this.localControllerNode = localControllerNode;
		this.data = data;
	}

	public RemoteNodeData getData() {
		return data;
	}

	public Message getMessageOrTimeOut(final InputStream in) throws IOException {
		Message messageRecieved;
		try {
			messageRecieved = new Message(in);
		} catch (final SocketTimeoutException e) {
			messageRecieved = null;
		}
		return messageRecieved;
	}

	@Override
	public void run() {
		LOG.debug("STARTED RemoteNodeControllerRunnable run {}", data.getHostAddress());

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

		data.getSendQueue().add(new Message(magic, "version",
				PayloadUtil.getVersionPayload(localPort, nonce, startHeight).toByteArray()));
		data.getSendQueue().add(new Message(magic, "verack"));
		try {
			try (Socket s = new Socket();) {
				s.setSoTimeout(2000);
				s.connect(data.getTcpAddressAndPort(), 2000);

				try (OutputStream out = s.getOutputStream(); InputStream in = s.getInputStream();) {
					data.setGoodPeer(true);

					while (data.isGoodPeer()) {
						Message messageToSend = data.getSendQueue().poll();
						while (messageToSend != null) {
							final byte[] outBa = messageToSend.toByteArray();
							out.write(outBa);
							if (messageToSend.commandEnum != null) {
								final long apiCallCount;
								apiCallCount = MapUtil.increment(LocalNodeData.API_CALL_MAP,
										"out-" + messageToSend.commandEnum.name().toLowerCase());
								MapUtil.increment(LocalNodeData.API_CALL_MAP, RemoteNodeData.OUT_BYTES, outBa.length);
								LOG.debug("request to {}:{} {}", data.getHostAddress(), messageToSend.command,
										apiCallCount);
							}
							messageToSend = data.getSendQueue().poll();
						}
						out.flush();
						Thread.sleep(data.getSleepIntervalMs());

						Message messageRecieved = getMessageOrTimeOut(in);
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
									if (messageRecieved.commandEnum.equals(CommandEnum.INV)) {
										final InvPayload payload = messageRecieved.getPayload(InvPayload.class);
										final String apiCall = apiCallRoot + "-"
												+ payload.getType().name().toLowerCase();
										apiCallCount = MapUtil.increment(LocalNodeData.API_CALL_MAP, apiCall);
									} else {
										apiCallCount = MapUtil.increment(LocalNodeData.API_CALL_MAP, apiCallRoot);
									}
									LOG.debug("response from {}:{} {}", data.getHostAddress(), messageRecieved.command,
											apiCallCount);
								}

								localControllerNode.onMessage(RemoteNodeControllerRunnable.this, messageRecieved);
							}
							messageRecieved = getMessageOrTimeOut(in);
						}

						final long currTimeMs = System.currentTimeMillis();
						final long recycleTimeMs = startTimeMs + data.getRecycleIntervalMs();
						if (recycleTimeMs < currTimeMs) {
							LOG.debug("recycling remote node {}", data.getHostAddress());
							data.setGoodPeer(false);
						}

						if (data.isGoodPeer()) {
							Thread.sleep(data.getSleepIntervalMs());
						}
					}
				}
			} catch (final SocketTimeoutException e) {
				LOG.trace("SocketTimeoutException from {}, closing peer", data.getHostAddress());
				LOG.trace("SocketTimeoutException", e);
				data.setGoodPeer(false);
			} catch (final ConnectException e) {
				LOG.trace("ConnectException from {}, closing peer", data.getHostAddress());
				LOG.trace("ConnectException", e);
				data.setGoodPeer(false);
			} catch (final SocketException e) {
				if (e.getMessage().equals("Broken pipe (Write failed)")) {
					LOG.trace("SocketException from {}, closing peer", data.getHostAddress());
				} else if (e.getMessage().equals("Operation timed out (Read failed)")) {
					LOG.trace("SocketException from {}, closing peer", data.getHostAddress());
				} else if (e.getMessage().equals("Connection reset")) {
					LOG.trace("SocketException from {}, closing peer", data.getHostAddress());
				} else if (e.getMessage().equals("Network is unreachable (connect failed)")) {
					LOG.trace("SocketException from {}, closing peer", data.getHostAddress());
				} else if (e.getMessage().equals("Protocol wrong type for socket (Write failed)")) {
					LOG.trace("SocketException from {}, closing peer", data.getHostAddress());
				} else {
					LOG.error("SocketException from {}, closing peer", data.getHostAddress());
					LOG.error("SocketException", e);
				}
				data.setGoodPeer(false);
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

	public void stop() throws InterruptedException {
		data.setGoodPeer(false);
		data.getSendQueue().clear();
	}

}
