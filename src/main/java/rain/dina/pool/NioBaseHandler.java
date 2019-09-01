package rain.dina.pool;

public class NioBaseHandler {

	public void sessionCreated(NioSession session) throws Exception {
		// Empty handler
	}

	public void sessionOpened(NioSession session) throws Exception {
		// Empty handler
	}

	public void sessionClosed(NioSession session) throws Exception {
		// Empty handler
	}

//	public void sessionIdle(NioSession session, IdleStatus status) throws Exception {
//		// Empty handler
//	}

	public void exceptionCaught(NioSession session, Throwable cause) throws Exception {

	}

	public void messageReceived(NioSession session, Object message) throws Exception {
		// Empty handler
	}

	public void messageSent(NioSession session, Object message) throws Exception {
		// Empty handler
	}

	public void inputClosed(NioSession session) throws Exception {
		// session.closeNow();
	}

}
