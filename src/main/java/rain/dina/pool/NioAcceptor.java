package rain.dina.pool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class NioAcceptor {
	
	private volatile Selector selector;
	private volatile boolean selectable;
	// private final NioProcessor processor;
	private static final int DEFAULT_SIZE = Runtime.getRuntime().availableProcessors() + 1;
	private final NioProcessor[] pool = new NioProcessor[DEFAULT_SIZE];
	private final Set<SocketAddress> boundAddresses = new HashSet<>();
	private final Queue<SocketAddress> registerQueue = new ConcurrentLinkedQueue<>();
	private final Queue<SocketAddress> cancelQueue = new ConcurrentLinkedQueue<>();
	private final Executor executor = Executors.newCachedThreadPool();
	private final Semaphore lock = new Semaphore(1);
	private AtomicReference<Acceptor> acceptorRef = new AtomicReference<>();

	public NioAcceptor() throws IOException {
		// init pool
		try {
			selector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
			if (selector != null) {
				selector.close();
			}
		}
		selectable = true;
	}

	public final void bind(SocketAddress localAddress) throws IOException, InterruptedException {
		if (localAddress == null) {
			throw new IllegalArgumentException("localAddress");
		}

		registerQueue.add(localAddress);

		startupAcceptor();
		try {
			lock.acquire();
			selector.wakeup();
		} finally {
			lock.release();
		}
		Set<SocketAddress> addresses = new HashSet<>();
		synchronized (boundAddresses) {
			boundAddresses.addAll(addresses);
		}
	}

	private void startupAcceptor() throws InterruptedException {
		if (!selectable) {
			registerQueue.clear();
			cancelQueue.clear();
		}

		// start the acceptor if not already started
		Acceptor acceptor = acceptorRef.get();

		if (acceptor == null) {
			lock.acquire();
			acceptor = new Acceptor();
			if (acceptorRef.compareAndSet(null, acceptor)) {
				executor.execute(acceptor);
			} else {
				lock.release();
			}
		}

	}

	protected void accept(ServerSocketChannel ssChannel) throws Exception {
		SelectionKey key = null;

		if (ssChannel != null) {
			key = ssChannel.keyFor(selector);
		}

		if ((key == null) || (!key.isValid()) || (!key.isAcceptable())) {
		}
		// accept the connection from the client
		SocketChannel ch = ssChannel.accept();

		if (ch == null) {
		}
	}

	protected ServerSocketChannel open(SocketAddress localAddress) throws Exception {
		// Creates the listening ServerSocket
		ServerSocketChannel channel = ServerSocketChannel.open();
		boolean success = false;
		try {
			channel.configureBlocking(false);
			ServerSocket socket = channel.socket();
			socket.setReuseAddress(false);
			socket.bind(localAddress, 50);
			channel.register(selector, SelectionKey.OP_ACCEPT);
			success = true;
		} finally {
			if (!success) {
				close(channel);
			}
		}
		return channel;
	}

	protected void close(ServerSocketChannel channel) throws Exception {
		SelectionKey key = channel.keyFor(selector);
		if (key != null) {
			key.cancel();
		}
		channel.close();
	}

	protected NioSession createSession(ServerSocketChannel channel) throws Exception {
		SelectionKey key = null;
		if (channel != null) {
			key = channel.keyFor(selector);
		}
		if ((key == null) || (!key.isValid()) || (!key.isAcceptable())) {
			return null;
		}
		// accept the connection from the client
		try {
			SocketChannel ch = channel.accept();
			if (ch == null) {
				return null;
			}
//			return new NioSession(this, pool, ch);
			return new NioSession(pool, ch);
		} catch (Throwable t) {
			try {
				Thread.sleep(50L);
			} catch (InterruptedException ie) {
			}
			return null;
		}
	}

	private class Acceptor implements Runnable {

		@Override
		public void run() {
			assert acceptorRef.get() == this;
			int nHandles = 0;
			// Release the lock
			lock.release();
			while (selectable) {
				try {
					// 如果没有连接， 阻塞当前线程
					int selected = selector.select();
					if (nHandles == 0) {
						acceptorRef.set(null);
						if (registerQueue.isEmpty() && cancelQueue.isEmpty()) {
							break;
						}
						if (!acceptorRef.compareAndSet(null, this)) {
							break;
						}

					}
					if (selected > 0) {
						acceptChannels(selector.selectedKeys().iterator());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		private void acceptChannels(Iterator<SelectionKey> channel) throws Exception {
			while (channel.hasNext()) {
				ServerSocketChannel ch = null;
				SelectionKey key = channel.next();

				if (key.isValid() && key.isAcceptable()) {
					ch = (ServerSocketChannel) key.channel();
				}
				channel.remove();
				// Associates a new created connection to a processor,
				// and get back a session
				NioSession session = createSession(ch);
				if (session == null) {
					continue;
				}
				// add the session to the SocketIoProcessor
				session.getProcessor().addAndStartProcessor(session);
			}
		}

	}

}
