package rain.dina.pool;

import java.net.SocketAddress;

public class WriteRequest {
	/** An empty message */
	public static final byte[] EMPTY_MESSAGE = new byte[] {};
	/**
	 * The original message as it was written by the IoHandler. It will be sent back
	 * in the messageSent event
	 */
	private final Object originalMessage;

	/** The message that will ultimately be written to the remote peer */
	private Object message;

	private final SocketAddress destination;

	public WriteRequest(Object originalMessage, Object message, SocketAddress destination) {
		super();
		this.originalMessage = originalMessage;
		this.message = message;
		this.destination = destination;
	}

}
