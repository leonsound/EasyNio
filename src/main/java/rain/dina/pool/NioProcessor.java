package rain.dina.pool;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NioProcessor {
	private static final int DEFAULT_SIZE = Runtime.getRuntime().availableProcessors() + 1;
	private Selector selector;

	private final Executor executor = Executors.newCachedThreadPool();
	private final AtomicReference<Processor> processorRef = new AtomicReference<>();

	private final Queue<NioSession> newSessions = new ConcurrentLinkedQueue<>();
	private ReadWriteLock selectorLock = new ReentrantReadWriteLock();
	protected AtomicBoolean wakeupCalled = new AtomicBoolean(false);

	private void startupProcessor() {
		Processor processor = processorRef.get();
		if (processor == null) {
			processor = new Processor();
			if (processorRef.compareAndSet(null, processor)) {
				executor.execute(processor);
			}
		}
		wakeup();
	}

	protected void wakeup() {
		wakeupCalled.getAndSet(true);
		selectorLock.readLock().lock();
		try {
			selector.wakeup();
		} finally {
			selectorLock.readLock().unlock();
		}
	}

	public void addAndStartProcessor(NioSession session) {
		newSessions.add(session);
		Processor processor = processorRef.get();
		if (processor == null) {
			processor = new Processor();
			if (processorRef.compareAndSet(null, processor)) {
				executor.execute(processor);
			}
		}

		// Just stop the select() and start it again, so that the processor
		// can be activated immediately.
		wakeup();
	}

	private class Processor implements Runnable {

		@Override
		public void run() {
			assert processorRef.get() == this;
			for (;;) {
				int selected = 0;
				selectorLock.readLock().lock();
				try {
					selected = selector.select(1000L);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					selectorLock.readLock().unlock();
				}

				int addedSessions = 0;
				for (NioSession session = newSessions.poll(); session != null; session = newSessions.poll()) {
					SelectableChannel ch = (SelectableChannel) session.getChannel();
					ch.configureBlocking(false);
					selectorLock.readLock().lock();
					try {
						session.setKey(ch.register(selector, SelectionKey.OP_READ, session));
					} catch (ClosedChannelException e) {
					} finally {
						selectorLock.readLock().unlock();
					}
					addedSessions++;
				}

				if (selected > 0) {
					process();
				}

			}
		}

	}
}
