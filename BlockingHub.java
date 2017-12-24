import java.net.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.io.*;
import java.util.Scanner;

public class BlockingHub implements Runnable {
	
	Thread thread;
	Call lcall;
	
	Call ccall;
	
	Call activeCall;
	
	SelfCall scall;
	
	//AbstractCall activeCall;
	
	//definitely replace this with a communication protocol; negotiate
	// other ports after first connection
	int lport;
	int aport;
	int vport;
	int tport;
	
	//ServerSocketChannel ssc;
	
	public boolean text;
	public boolean audio;
	public boolean video;
	boolean options;
	
	String local;
	
	//private Semaphore calling;
	
	BlockingResource res;
	
	//TODO: add functionality for receiving a call while already on a call
	public BlockingHub(BlockingResource r, String localip) {
		activeCall = null;
		text = true;
		video = false;
		//right now, 
		audio = false;
		options = true;
		//calling = new Semaphore(1);
		res = r;
		local = localip;
		lport = 4321;
		tport = 4322;
		aport = 4323;
		vport = 4324;
	}
	
	public void start() {
		thread = new Thread(this);
		thread.start();
		Scanner sc = new Scanner(System.in);
		
		//TODO: seriously consider making this do more work, or somehow sleeping/blocking
		Call held;
		Call ended;
		Call resumed;
		while (options) {
			//System.out.println("checking for new call!");
			//System.out.println("test");
			
			if ((held = res.held()) != null) {
				res.hold(null);
				held.hold();
			}
			
			
			if ((resumed = res.resumed()) != null) {
				res.resume(null);
				resumed.resume();
			}
			
			/*
			if ((ended = res.ended()) != null) {
				//res.ended(null);
				//ended.hangup();
			}
			*/
			
			if (res.calledSelf()) {
				System.out.println("Hub calls itself!");
				callSelf();
				res.callSelf(false);
			}
			
			if (res.closedSelf()) {
				scall.hangup();
				res.closeSelf(false);
			}
			
			if (res.call() && !res.isCalling()) {
				System.out.println("test");
				res.setCalling(true);
				res.setConnected(call(res.getIP(), res.getPort()));
				/*
				if(call(res.getIP(), res.getPort())) {
					res.setConnected(true);
					//res.updateGuiConnected(true);
				} else {
					res.setConnected(false);
					//res.updateGuiConnected(false);
				}
				*/
			}
			
			if (res.isConnected() && res.isDisconnected()) {
				System.out.println("Close Listener:");
				activeCall.hangup();
				//System.out.println("Close Caller:");
				//ccall.hangup();
				
				//TODO: reimplement this somewhere somehow
				//res.finishHangup();
			}
		}
	}
	
	
	@Override
	public void run() {
		//TODO: have the try/catch happen INSIDE the while loop
		try {
			
			int options = 3;
			
			//ServerSocket ss = new ServerSocket(lport);
			
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.socket().bind(new InetSocketAddress(lport));
			ssc.configureBlocking(true);
			SocketChannel receiver = null;
			//TODO: why is this nonblocking? I forget
			while (true) {
				//calling.acquire();
				
				System.out.println("Wait for a call");
				receiver = ssc.accept();
				if (receiver != null && receiver.isConnected()) {
					
					String remote = receiver.getRemoteAddress().toString().substring(1, 13);
					
					//blocks and sees if user answers phone. If not, end call
					//	and restart
					boolean ans = res.receiveCall(15000, remote);
						
					try {
						
						ByteBuffer buf = null;
						
						int written = 0;
						
						String msg;
						
						//TODO IMMEDIATE: ans is now the current index of the call.
						//		however, a call's index can change; this will result
						//		in collisions.
						if (ans) {
							msg = "a";
							buf = ByteBuffer.allocate(8);
							buf.put(msg.getBytes());
							int pos = buf.position();
							buf.position(0);
							while ((written = receiver.write(buf)) < pos) {
								System.out.println("written: " + written);
								System.out.println("position: " + buf.position());
							}
						} else {
							msg = "c";
							buf = ByteBuffer.allocate(8).put(msg.getBytes());
							int pos = buf.position();
							buf.position(0);
							while ((written = receiver.write(buf)) < pos) {
								System.out.println("written: " + written);
								System.out.println("position: " + buf.position());
							}
							System.out.println("ignore call. end");
							receiver.close();
							continue;
						}
						System.out.println("answer call. proceed");
						
						receiver.configureBlocking(false);
						
						//TODO: make them send over what channels can be communicated on (?)
						
						
						ByteBuffer in = ByteBuffer.allocate(64);
						int got = 0;
						boolean gotport = false;
						while (!gotport) {
						
							got = receiver.read(in);
							msg = new String(in.array());
							System.out.println("read " + got + " bytes for msg: " + msg);
							if (got == 4) {
								gotport = true;
							}
						
						}
					
						if (receiver.isConnected()) {
							receiver.close();
						}
					
						in.position(0);
					
						int newport = in.getInt();
					
						System.out.println("Receiver new port: " + newport);
					
						System.out.println("Start listening on new port");
					
						ServerSocketChannel nsc = ServerSocketChannel.open();
						nsc.socket().bind(new InetSocketAddress(newport));
						nsc.configureBlocking(false);
						
						System.out.println("Bind successful!");
						
						while (true) {
							receiver = nsc.accept();
							if (receiver != null && receiver.isConnected()) {
								System.out.println("Receiver is connected!");
								break;
							}
						}
						
						nsc.close();
						System.out.println("!!!!!!!!!!!!!!!!!");
						lcall = new Call(res, receiver, "irrelevant", aport, vport, true, options);
						res.updateGuiConnected(true, lcall);
						activeCall = lcall;
					} catch (Exception e2) {
						//TODO: almost definitely need better cleanup here
						System.out.println("Receiver had an error and closed!");
						res.ansFailed();
						if (receiver != null && receiver.isOpen()) {
							try {
								receiver.close();
							} catch (Exception e3) {
								e3.printStackTrace();
							}
						}
						res.updateGuiConnected(false, null);
						e2.printStackTrace();
					}
				
				}
				/*
				 catch (Exception e2) {
					//TODO: almost definitely need better cleanup here
					System.out.println("Receiver had an error and closed!");
					res.ansFailed();
					if (receiver != null && receiver.isOpen()) {
						try {
							receiver.close();
						} catch (Exception e3) {
							e3.printStackTrace();
						}
					}
					res.updateGuiConnected(false, null);
					e2.printStackTrace();
				}
				*/
				//call = new Call(res, receiver, aport, vport, true, options);
				//aport += 3;
				//vport += 3;
				//calling.release();
			}
		} catch (Exception e) {
			System.out.println("listening error");
			e.printStackTrace();
		}
	}
	
	//triggered by gui. request from user to call some number.
	//creates new Call
	//TODO: needs a semaphore (maybe?)
	public boolean call(String ip, int port) {
		System.out.println("Try calling!");
		SocketChannel caller = null;
		try {
			
			int options = 3;
			
			if (ip.equals(local)) {
				System.out.println("Calling myself! This shouldn't be allowed!");
				options = 2;
				return false;
			}
			int newport;
			if (port == -1) {
				//TODO: make this grab a random open port
				newport = tport;
			} else {
				newport = port;
			}
			
			caller = SocketChannel.open();
			
			caller.configureBlocking(true);
			
			
			//TODO: this might conflict
			caller.connect(new InetSocketAddress(ip, lport));
			
			while (!caller.finishConnect()) {
				System.out.println("...");
			}
			
			ByteBuffer buf = ByteBuffer.allocate(1);
			int got = 0;
			String ans;
			
			got = caller.read(buf);
			ans = new String(buf.array());
			
			System.out.println("received " + ans);
			
			if (!ans.equals("a")) {
				System.out.println("I was ignored!");
				if (caller.isOpen()) {
					caller.close();
				}
				return false;
			}
			
			System.out.println("I was answered!");
			
			System.out.println("Caller new port: " + newport);
			
			buf = ByteBuffer.allocate(4).putInt(newport);
			
			String msg = new String(buf.array());
			
			System.out.println("sending: " + msg);
			
			//int num = buf.getInt();
			
			//System.out.println("number is : " + num);
			
			System.out.println("position: " + buf.position());
			System.out.println("remaining: " + buf.remaining());
			
			buf.position(0);
			
			System.out.println("position: " + buf.position());
			System.out.println("remaining: " + buf.remaining());
			
			int written = 0;
			while ((written = caller.write(buf)) < 4) {
				System.out.println("written: " + written);
				System.out.println("position: " + buf.position());
			}
			
			System.out.println("written: " + written);
			
			System.out.println("sent: " + msg);
			//caller.write(buf);
			
			if (caller.isConnected()) {
				caller.close();
			}
			
			//calling.release();
			
			caller = SocketChannel.open();
			caller.configureBlocking(false);
			boolean failed = false;
			System.out.println("Try to connect");
			
			while (!failed) {
			
				try {
					if (!caller.isConnectionPending()) {
						//if other side is not ready, this will error
						caller.connect(new InetSocketAddress(ip, newport));
					} else {
						//System.out.println("finish connecting!");
						while (!caller.finishConnect()) {
							
						}
						failed = true;
					}
				} catch (Exception e2) {
					try {
						caller.close();
						caller = SocketChannel.open();
						caller.configureBlocking(false);
					} catch (Exception e3) {
						//e3.printStackTrace();
					}
					//e2.printStackTrace();
				}
			
			}
			
			System.out.println("Connected!");
			
			ccall = new Call(res, caller, ip, aport, vport, false, options);
			res.updateGuiConnected(true, ccall);
			activeCall = ccall;
			
		} catch (Exception e) {
			if (caller != null && caller.isOpen()) {
				try {
					caller.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
			System.out.println("Caller error");
			e.printStackTrace();
			res.updateGuiConnected(false, null);
			return false;
		}
		return true;
	}
	
	
	public void callSelf() {
		System.out.println("Try calling self!");
		try {
			
			boolean c = false;
			boolean l = false;
			
			ServerSocketChannel nsc = ServerSocketChannel.open();
			nsc.socket().bind(new InetSocketAddress(tport));
			nsc.configureBlocking(false);
			
			//TODO: probably grab a random port
			int newport = tport;
			
			SocketChannel caller = SocketChannel.open();
			
			caller = SocketChannel.open();
			caller.configureBlocking(false);
			boolean failed = false;
			System.out.println("Try to connect");
			
			SocketChannel receiver = null;
			
			while (!c && !l) {
			
				if (!l) {
					receiver = nsc.accept();
					if (receiver != null && receiver.isConnected()) {
						System.out.println("Receiver is connected!");
						l = true;
					}
				}
				
				if (!c) {
					try {
						if (!caller.isConnectionPending()) {
							//if other side is not ready, this will error
							caller.connect(new InetSocketAddress("localhost", newport));
						} else {
							System.out.println("finish connecting!");
							while (!caller.finishConnect()) {
								
							}
							c = true;
						}
					} catch (Exception e2) {
						try {
							caller.close();
							caller = SocketChannel.open();
							caller.configureBlocking(false);
						} catch (Exception e3) {
							//e3.printStackTrace();
						}
						//e2.printStackTrace();
					}
				}
			}
			
			nsc.close();
			System.out.println("!!!!!!!!!!!!!!!!!");
			
			receiver.configureBlocking(true);
			caller.configureBlocking(true);
			
			scall = new SelfCall(res, receiver, caller, aport, vport);
			
			res.setSelf(scall);
			
			//lcall = new Call(res, receiver, aport, vport, true, 1);
			
			//System.out.println("Connected!");
			
			//ccall = new Call(res, caller, aport, vport, false, 2);
			
		} catch (Exception e) {
			System.out.println("Error!!");
			e.printStackTrace();
		}
	}
	
}
