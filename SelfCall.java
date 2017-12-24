import java.nio.channels.*;
import java.io.*;
import java.net.*;

public class SelfCall {

	SocketChannel lt;
	SocketChannel la;
	SocketChannel lv;
	
	SocketChannel ct;
	SocketChannel ca;
	SocketChannel cv;
	
	AudioRW laudio;
	AudioRW caudio;
	
	int aport, vport;
	
	boolean connected;
	
	BlockingResource res;
	
	InputStream tin;
	OutputStream tout;
	
	InputStream ain;
	OutputStream aout;
	
	InputStream vin;
	OutputStream vout;

	public SelfCall(BlockingResource r, SocketChannel l, SocketChannel c, int aport, int vport) {
		lt = l;
		ct = c;
		this.aport = aport;
		this.vport = vport;
		connected = true;
		res = r;
		
		try {
			lt.configureBlocking(true);
			ct.configureBlocking(true);
			tin = lt.socket().getInputStream();
			tout = ct.socket().getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		openStreams();
		
		//Start text connection
		readText();
		writeText();
		
		
		//assume that both audio and video are enabled:
		laudio = new AudioRW(r, la, ain, aout, 1);
		
		caudio = new AudioRW(r, ca, ain, aout, 2);
		
	}
	
	public void openStreams() {
	
		boolean laflg = false;
		boolean lvflg = false;
		
		boolean caflg = false;
		boolean cvflg = false;
		
		try {
			
			System.out.println("Start opening streams");
			
			ServerSocketChannel a_ss = ServerSocketChannel.open();
			a_ss.socket().bind(new InetSocketAddress(aport));
			a_ss.configureBlocking(false);
			
			ServerSocketChannel v_ss = ServerSocketChannel.open();
			v_ss.socket().bind(new InetSocketAddress(vport));
			v_ss.configureBlocking(false);
			
			
			
			ca = SocketChannel.open();
			cv = SocketChannel.open();
			
			ca.configureBlocking(false);
			cv.configureBlocking(false);
			
			
			
			while (!laflg && !lvflg && !caflg && !cvflg) {
			
				//System.out.println("try accepting with audio");
				la = a_ss.accept();
				if (!laflg && la != null && la.isConnected()) {
					System.out.println("Listener received audio!");
					la.configureBlocking(false);
					laflg = true;
				}
				
				//System.out.println("try accepting with video");
				lv = v_ss.accept();
				if (!lvflg && lv != null && lv.isConnected()) {
					System.out.println("Listener received video!");
					lv.configureBlocking(false);
					lvflg = true;
				}
				
				
				//try to connect audio
				if (!caflg) {
					try {
						if (!ca.isConnectionPending()) {
							//if other side is not ready, this will error
							ca.connect(new InetSocketAddress("localhost", aport));
						} else {
							if(ca.finishConnect()) {
								System.out.println("Caller was answered for audio");
								caflg = true;
							}
						}
					} catch (Exception ea) {
						try {
							ca.close();
							ca = SocketChannel.open();
							ca.configureBlocking(false);
						} catch (Exception ea2) {
							ea2.printStackTrace();
						}
						ea.printStackTrace();
					}
				}
				
				//try to connect video
				if (!cvflg) {
					try {
						if (!cv.isConnectionPending()) {
							//if other side is not ready, this will error
							cv.connect(new InetSocketAddress("localhost", vport));
						} else {
							if(cv.finishConnect()) {
								System.out.println("Caller was answered for video");
								cvflg = true;
							}
						}
					} catch (Exception ev) {
						try {
							cv.close();
							cv = SocketChannel.open();
							cv.configureBlocking(false);
						} catch (Exception ev2) {
							ev2.printStackTrace();
						}
						ev.printStackTrace();
					}
				}
			}
			
			ca.configureBlocking(true);
			cv.configureBlocking(true);
			
			la.configureBlocking(true);
			lv.configureBlocking(true);
			
			a_ss.close();
			v_ss.close();
			
			ain = la.socket().getInputStream();
			aout = ca.socket().getOutputStream();
			vin = lv.socket().getInputStream();
			vout = cv.socket().getOutputStream();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Finish opening streams");
	}
	
	
	
	
	public void readText() {
		(new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(tin));
					while (connected) {
						System.out.println("listening for message");
						String msg = br.readLine();
						System.out.println("Message received: " + msg);
						if (msg.charAt(0) == 255) {
							//TODO: initiate negotiations:
							//		this may eventually just be a control character
							//		where the actual control message follows
							connected = false;
							System.out.println("I've been told to close!");
							break;
						}
						System.out.println(msg);
					}
					if (lt.isOpen()) {
						System.out.println("reader closes");
						lt.close();
					}
				} catch (Exception e) {
					
					try {
						if (lt.isOpen()) {
							System.out.println("reader error closes");
							lt.close();
						}
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					
					e.printStackTrace();
				}
				
			}
		
		})).start();
	}
	
	public void writeText() {
		(new Thread(new Runnable() {
		
			@Override
			public void run() {
				String msg;
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(tout));
				//int size = 32;
				//byte[] raw = new byte[size];
				//int count;
				
				try {
					//TODO: this will not work. even after the user has pressed
					//		hangup(), and connected is set to false, the scanner
					//		will keep blocking while waiting for user input.
					//		we could mess with scanner/etc, but we won't even
					//		use scanner in the end; instead, we will listen and
					//		block on input from the user/Display.
					while (connected) {
						//msg = sc.nextLine();
						System.out.println("writer starts block ");
						msg = res.getText();		//block and get data from res
						System.out.println("writer writes " + msg);
						if (msg.charAt(0) == 255) {
							System.out.println("writer will write message and close");
							connected = false;
							System.out.println("value is: " + connected);
						}
						
						System.out.println("writing msg: " + msg);
						bw.write(msg, 0, msg.length());
						bw.newLine();
						bw.flush();
					}
					
					System.out.println("message sent. try to close connection");
					
					if (ct.isOpen()) {
						System.out.println("writer closes");
						bw.close();
						ct.close();
					}
					
				} catch (Exception e) {
					
					try {
						if (ct.isOpen()) {
							System.out.println("writer error closes");
							ct.close();
						}
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					
					e.printStackTrace();
				}
			}
		})).start();
	}
	
	public void hangup() {
		System.out.println("start hangup");
		try {
			
			//TODO: for some reason, setting connected=false is ignored here
			connected = false;
			res.setCtrl((char)255);
			
			if (la.isConnected()) {
				laudio.close();
				//a.close();
			}
			
			if (lv.isConnected()) {
				lv.close();
			}
			
		} catch (Exception e) {
			System.out.println("call says \"shit\"");
			e.printStackTrace();
		}
		System.out.println("All sockets closed!");
	}

}
