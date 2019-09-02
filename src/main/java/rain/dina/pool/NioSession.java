package rain.dina.pool;

import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class NioSession {
	private long sessionId;
	private static AtomicLong idGenerator = new AtomicLong(0);
	/** The NioSession processor */
	protected final NioProcessor processor;

	/** The communication channel */
	protected final Channel channel;

	/** The SelectionKey used for this session */
	private SelectionKey key;
    private boolean readSuspended = false;
    private boolean writeSuspended = false;
	
	private final ConcurrentHashMap<Object, Object> attributes = new ConcurrentHashMap<>(4);
	 /** A queue to store incoming write requests */
    private final Queue<WriteRequest> writeQueue = new ConcurrentLinkedQueue<>();

	public NioSession(NioProcessor processor, Channel channel) {
		this.processor = processor;
		this.channel = channel;
	}

	public NioSession(NioProcessor[] pool, Channel channel) {
		this.processor = pool[Math.abs((int) sessionId) % pool.length];
		this.channel = channel;
		sessionId = idGenerator.incrementAndGet();
	}

	public final boolean isActive() {
		return key.isValid();
	}

	public Queue<WriteRequest> getWriteQueue() {
		return writeQueue;
	}

	public NioProcessor getProcessor() {
		return processor;
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public SelectionKey getKey() {
		return key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

	public Channel getChannel() {
		return channel;
	}

	public ConcurrentHashMap<Object, Object> getAttributes() {
		return attributes;
	}

	public boolean isReadSuspended() {
		return readSuspended;
	}

	public void setReadSuspended(boolean readSuspended) {
		this.readSuspended = readSuspended;
	}

	public boolean isWriteSuspended() {
		return writeSuspended;
	}

	public void setWriteSuspended(boolean writeSuspended) {
		this.writeSuspended = writeSuspended;
	}
	
	

}
