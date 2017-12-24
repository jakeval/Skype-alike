import java.nio.channels.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.ByteBuffer;


/*
3 channels of communication: audio, video, text

1 reader:
 -splits input and sends to appropriate channel
3 writers:
 -need to give illusion of writing at same time, but can't actually do so
 -each writes a certain amount, then lets others write
 
 -video writes one screen at a time
 -audio writes incrementally
 -text writes one phrase at a time
 
 -audio, video, and text all come from Hub




-the "main" thread listens for user input/options and is prepared to reconfigure things
-the "background" threads handle reading/writing of actual message


ResourceRW's handle reading/writing
Call simply contains them and handles their input/output

reading RW:
 -hub checks to see if RW has written any data


gui keeps checking the output of the RW
if there is info to display, display it



-eliminate video and text for now


*/

//TODO: go through all socket connections/etc, make sure that they are always closed in
//		catch statements, etc; consider edge cases, check for errors (esp. with spamming
//		call/end)
public class Call {
	
	SocketChannel t;
	SocketChannel a;
	SocketChannel v;
	
	AudioRW audio;
	
	int aport, vport;
	int options;
	
	boolean listen;
	
	boolean connected;
	
	BlockingResource res;
	
	InputStream tin;
	OutputStream tout;
	
	InputStream ain;
	OutputStream aout;
	
	InputStream vin;
	OutputStream vout;
	
	Scanner sc;
	
	String ip;
	
	Thread tr;
	Thread tw;
	
	
	public Call(BlockingResource r, SocketChannel s, String ip, int aport, int vport, boolean listen, int op) {
		connected = true;
		t = s;
		res = r;
		this.ip = ip;
		this.listen = listen;
		this.aport = aport;
		this.vport = vport;
		options = op;
		
		sc = new Scanner(System.in);
		
		System.out.println("Call started!");
		
		try {
			s.configureBlocking(true);
			final ByteChannel bt = wrapChannel(s);
			tin = Channels.newInputStream(bt);
			tout = Channels.newOutputStream(bt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		if (listen) {
			listenStreams(true, true);
		} else {
			callStreams(true, true);
		}
		
		
		//Start text connection
		if ((op & 1) == 1) {
			readText();
		}
		if ((op & 2) == 2) {
			writeText();
		}
		
		
		
		//assume that both audio and video are enabled:
		audio = new AudioRW(r, a, ain, aout, op);

		
	}
	
	
	private ByteChannel wrapChannel(final ByteChannel c) {
		return new ByteChannel() {
			public int write(ByteBuffer src) throws IOException {
				return c.write(src);
			}
			
			public int read(ByteBuffer dst) throws IOException {
				return c.read(dst);
			}
			
			public boolean isOpen() {
				return c.isOpen();
			}
			
			public void close() throws IOException {
				c.close();
			}
		};
	}
	
	/*
	 Opens audio and video streams
	*/
	public int listenStreams(boolean audio, boolean video) {
		boolean aflg = false;
		boolean vflg = false;
		
		System.out.println("Start listening for audio + video: " + aport + ", " + vport);
		
		try {
			ServerSocketChannel a_ss = ServerSocketChannel.open();
			a_ss.socket().bind(new InetSocketAddress(aport));
			a_ss.configureBlocking(false);
			
			ServerSocketChannel v_ss = ServerSocketChannel.open();
			v_ss.socket().bind(new InetSocketAddress(vport));
			v_ss.configureBlocking(false);
			
			int count = 0;
			
			while (!(aflg && vflg) && count < 200) {
				
				if (!aflg) {
					a = a_ss.accept();
					if (a != null && a.isConnected()) {
						System.out.println("Listener received audio!");
						a.configureBlocking(false);
						aflg = true;
					}
				}
				
				if (!vflg) {
					v = v_ss.accept();
					if (!vflg && v != null && v.isConnected()) {
						System.out.println("Listener received video!");
						v.configureBlocking(false);
						vflg = true;
					}
				}
				
				if (aflg || vflg) {
					System.out.println("Listener count: " + count);
					count++;
				}
			}
			
			System.out.println("a: " + a.isConnected());
			System.out.println("v: " + v.isConnected());
			
			a.configureBlocking(true);
			v.configureBlocking(true);
			
			a_ss.close();
			v_ss.close();
			
			
			final ByteChannel ba = wrapChannel(a);
			final ByteChannel bv = wrapChannel(v);
			
			ain = Channels.newInputStream(ba);
			aout = Channels.newOutputStream(ba);
			vin = Channels.newInputStream(bv);
			vout = Channels.newOutputStream(bv);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 2;
	}
	
	public int callStreams(boolean audio, boolean video) {
		boolean aflg = false;
		boolean vflg = false;
		
		System.out.println("Start calling for audio + video: " + aport + ", " + vport);
		
		try {
			a = SocketChannel.open();
			v = SocketChannel.open();
			
			a.configureBlocking(false);
			v.configureBlocking(false);
			
			//otherwise, the string returned is "/172.16.52.27:4324"
			//String ip = t.getRemoteAddress().toString().substring(1,13);
			
			//System.out.println("Address: " +  ip);
			
			//Try to connect to other side
			while (!(aflg && vflg)) {
				
				//try to connect audio
				try {
					if (!aflg) {
						if (!a.isConnectionPending()) {
							//if other side is not ready, this will error
							a.connect(new InetSocketAddress(ip, aport));
						} else {
							if(a.finishConnect()) {
								System.out.println("Caller was answered for audio");
								aflg = true;
							}
						}
					}
				} catch (Exception ea) {
					try {
						a.close();
						a = SocketChannel.open();
						a.configureBlocking(false);
						aflg = false;
					} catch (Exception ea2) {
						ea2.printStackTrace();
					}
					ea.printStackTrace();
				}
				
				//try to connect video
				try {
					if (!vflg) {
						if (!v.isConnectionPending()) {
							//if other side is not ready, this will error
							v.connect(new InetSocketAddress(ip, vport));
						} else {
							if(v.finishConnect()) {
								System.out.println("Caller was answered for video");
								vflg = true;
							}
						}
					}
				} catch (Exception ev) {
					try {
						v.close();
						v = SocketChannel.open();
						v.configureBlocking(false);
						vflg = false;
					} catch (Exception ev2) {
						ev2.printStackTrace();
					}
					ev.printStackTrace();
				}
				
			}
			
			System.out.println("a: " + a.isConnected());
			System.out.println("v: " + v.isConnected());
			
			a.configureBlocking(true);
			v.configureBlocking(true);
			
			final ByteChannel ba = wrapChannel(a);
			final ByteChannel bv = wrapChannel(v);
			
			ain = Channels.newInputStream(ba);
			aout = Channels.newOutputStream(ba);
			vin = Channels.newInputStream(bv);
			vout = Channels.newOutputStream(bv);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 2;
	}
	
	//TODO: make sure this handles the caller unexpectedly dropping the call; currently,
	//		it does not
	public void readText() {
		tr = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					
					InputStreamReader in = new InputStreamReader(tin, "UTF-8");
					char[] buf = new char[128];
					
					while (connected) {
						System.out.println("begin listening");
						try {
							System.out.println("listening for message");
						
							int c = in.read(buf, 0, buf.length);
							//tin.read(buf);
							String msg = new String(buf, 0, c);
							System.out.println("Message received: " + msg);
							if (buf[0] == 255) {
								//TODO: initiate negotiations:
								//		this may eventually just be a control character
								//		where the actual control message follows
								connected = false;
								System.out.println("I've been told to close!");
								break;
						}
						} catch (IOException ioe) {
							ioe.printStackTrace();
							System.out.println("Interrupted!!!!!");
						}
					}
					if (t.isOpen()) {
						System.out.println("reader closes");
						t.close();
					}
				} catch (Exception e) {
					
					try {
						if (t.isOpen()) {
							System.out.println("reader error closes");
							t.close();
						}
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					
					e.printStackTrace();
				}
			}
		
		});
		tr.start();
	}
	
	public void writeText() {
		tw = new Thread(new Runnable() {
		
			@Override
			public void run() {
				String msg;
				
				try {
				
					OutputStreamWriter out = new OutputStreamWriter(tout, "UTF-8");
				
					while (connected) {
					
						try {
					
							msg = res.getText();		//block and get data from res
							//hangup
							if (msg.charAt(0) == 255) {
								System.out.println("writer will write hangup message and close");
								connected = false;
							}
							//hold
							if (msg.charAt(0) == 256) {
								System.out.println("writer will write hold message and hold");
								tr.wait();
								tw.wait();
							}
							//resume
							if (msg.charAt(0) == 257) {
								System.out.println("writer will write resume message and resume");
							
							}
						
							System.out.println("writing msg: " + msg);
							out.write(msg, 0, msg.length());
							System.out.println("written");
							out.flush();
							System.out.println("flushed");
						
						} catch (IOException ioe) {
							ioe.printStackTrace();
							System.out.println("Writer interrupted!!!!");
						}
					}
					
					System.out.println("message sent. try to close connection");
					
					if (t.isOpen()) {
						System.out.println("writer closes");
						//bw.close();
						t.close();
					}
					
				} catch (Exception e) {
					
					try {
						if (t.isOpen()) {
							System.out.println("writer error closes");
							t.close();
						}
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					
					e.printStackTrace();
				}
			}
		});
		tw.start();
	}
	
	public void hold() {
		//t.setKeepAlive(true);
		//a.setKeepAlive(true);
		//v.setKeepAlive(true);
		
		audio.hold();
		//video.hold();
		//hold text
	}
	
	public void resume() {
		//t.setKeepAlive(false);
		//a.setKeepAlive(false);
		//v.setKeepAlive(false);
		
		//audio.hold();
		//video.hold();
		//hold text
		
		//res.setCtrl((char)257);
		tw.interrupt();
		tr.interrupt();
	}
	
	public void hangup() {
		System.out.println("start hangup");
		try {
			
			//TODO: change to isOpen() (maybe)
			if (t.isConnected()) {
				//TODO: for some reason, setting connected=false is ignored here
				connected = false;
				res.setCtrl((char)255);
			}
			
			if (a.isConnected()) {
				audio.close();
				//a.close();
			}
			
			if (v.isConnected()) {
				v.close();
			}
			
		} catch (Exception e) {
			System.out.println("call says \"shit\"");
			e.printStackTrace();
		}
		
		System.out.println("All sockets closed!");
	}
	
}
