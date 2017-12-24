import java.net.InetAddress;
import java.util.concurrent.Semaphore;


public class BlockingResource {
	
	public int maxCalls;
	
	SelfCall self;
	
	//controls getting/setting text externally.
	//it makes anybody trying to call getText() block until setText() is called
	Semaphore blocker;
	//this is the blocker flag.
	//it makes sure that once setCtrl() is called, setText() can't be called
	// before getText()
	Semaphore blflg;
	
	//controls getting/setting text internally
	Semaphore tflg;
	Semaphore aflg;
	Semaphore vflg;
	Semaphore callflg;
	
	//hold audio data, text data, and video data
	
	boolean resume;
	boolean held;
	Call hcall;
	Call rcall;
	
	boolean gui;
	
	Semaphore ansflg;
	boolean answered;
	
	boolean callSelf;
	boolean closeSelf;
	
	boolean call;
	boolean calling;
	boolean connected;
	String ip;
	int port;
	boolean disconnected;
	
	Thread blocked;
	
	int volume;
	boolean muted;
	
	String msg;
	
	//TODO: fix this; probably shouldn't be public
	public Display disp;
	
	public BlockingResource(boolean gui, int asize, int vsize) {
		hcall = null;
		rcall = null;
		self = null;
		held = false;
		this.gui = gui;
		maxCalls = 5;
		callSelf = false;
		blflg = new Semaphore(1);
		blocker = new Semaphore(1);
		
		ansflg = new Semaphore(1);
		answered = false;
		
		try {
			ansflg.acquire();
			blocker.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}
		msg = "";
		//TODO: not linked to the display's volume until updated by user
		volume = 50;
		muted = false;
		//indicates that the gui has requested a disconnect
		disconnected = true;
		
		//indicates that the phone is currently calling
		calling = false;
		
		//indicates that the gui has requested a call
		call = false;
		
		//true when the call is connected/in progress
		connected = false;
		tflg = new Semaphore(1);
		aflg = new Semaphore(1);
		vflg = new Semaphore(1);
		callflg = new Semaphore(1);
	}
	
	
	
	//adds text. read by getText()
	public void setText(String text) {
		System.out.println("User set text");
		try {
			blflg.acquire();
			tflg.acquire();
			if (!text.equals("")) {
				msg += text;
				if (blocker.availablePermits() == 0) {
					System.out.println("set text released block");
					blocker.release();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		tflg.release();
		blflg.release();
	}
	
	/*
	 adds control text.
	 this may have side effects, such as causing getText() to stop
	 blocking.
	 
	 TODO-note:
	 	what *should* happen is that immediately after blocker is released,
	 	getText() is allowed in and reads this value.
	 	It is essential that getText() ALWAYS beats setText().
	 	It is very likely that it will, since 
	*/
	public void setCtrl(char ctrl) {
		System.out.println("Program set ctrl");
		try {
			blflg.acquire();
			tflg.acquire();
			//TODO: this may lose a message sent IMMEDIATELY before hanging up
			msg = String.valueOf(ctrl);
			
			if (blocker.availablePermits() == 0) {
				System.out.println("set ctrl released block");
				blocker.release();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		tflg.release();
	}
	
	/*
	 this method attempts to retrieve any text entered by the user.
	 once retrieved, the text is removed from storage.
	 if no text is available when this is called, it will block until text
	 is available.
	*/
	public String getText() {
		//this.Thread.sleep();
		String send = "";
		try {
			if (msg.equals("")) {
				System.out.println("getText blocks...");
				blocker.acquire();
			}
			System.out.println("getText passed block!");
			tflg.acquire();
			blflg.release();
			
			System.out.println("getText accessed text!");
			
			send = msg;
			msg = "";
		} catch (Exception e) {
			System.out.println("Uh oh!!!");
			e.printStackTrace();
		}
		tflg.release();
		System.out.println("Returning " + send);
		return send;
	}
	
	
	
	
	
	
	//don't need semaphores - should be able to write/read at same time
	public void setVolume(int v) {
		volume = v;
	}
	
	public void setMute(boolean mute) {
		muted = mute;
	}
	
	public double getVolume() {
		if (muted) {
			return 0;
		}
		return (double) volume;
	}
	
	
	
	
	
	
	
	//Call state - calling, hanging up, etc...
	
	
	//Hold:
	public Call held() {
		return hcall;
	}
	
	public void hold(Call call) {
		hcall = call;
	}
	
	//Resume:
	public Call resumed() {
		return rcall;
	}
	
	public void resume(Call call) {
		rcall = call;
	}
	
	//Self...
	public void resumeSelf() {
		
	}
	
	public void holdSelf() {
		
	}
	
	//TODO: fix hanging up
	//Hangup/call behavior:
	/*
	public void hangup() {
		try {
			callflg.acquire();
			disconnected = true;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void finishHangup() {
		try {
			callflg.acquire();
			connected = false;
			disp.setConnected(false);
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	*/
	public boolean isDisconnected() {
		boolean c = false;
		try {
			callflg.acquire();
			c = disconnected;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return c;
	}
	
	public boolean closedSelf() {
		return closeSelf;
	}
	
	public void closeSelf(boolean c) {
		closeSelf = c;
	}
	
	public void setSelf(SelfCall self) {
		this.self = self;
	}
	
	public SelfCall getSelf() {
		return self;
	}
	
	//TODO: should these have semaphores?
	public boolean calledSelf() {
		return callSelf;
	}
	
	public void callSelf(boolean c) {
		//127.0.0.1
		callSelf = c;
	}
	
	//ip, port, and call=call/hangup
	public void call(String ip, int port) {
		try {
			callflg.acquire();
			this.ip = ip;
			this.port = port;
			call = true;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//returns whether the a call has been requested
	public boolean call() {
		boolean c = false;
		try {
			callflg.acquire();
			c = call;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return c;
	}
	
	//returns whether the phone is ringing
	public boolean isCalling() {
		boolean c = false;
		try {
			callflg.acquire();
			c = calling;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return c;
	}
	
	//sets whether the phone is ringing
	public void setCalling(boolean c) {
		try {
			callflg.acquire();
			calling = c;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//sets whether the phone was answered
	public void setConnected(boolean c) {
		try {
			callflg.acquire();
			calling = false;
			call = false;
			connected = c;
			disconnected = !c;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	//called by the gui upon answering/ignore a call
	public void ansCall(boolean c) {
		answered = c;
		ansflg.release();
	}
	
	//Blocks. tells gui that the user is being called
	//also, tell gui how long the call will be displayed for; let gui "end" call itself
	public boolean receiveCall(long timeout, String ip) {
		if (gui)
			disp.receiveCall(timeout, ip);
		try {
			//blocks...
			ansflg.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return answered;
	}
	
	//If remote caller drops call before local user answers/ignores it, Hub calls
	// this method. It will "poke" the gui to let it know that the call was dropped
	//it does *not* need to release the block on receiveCall, since that has failed
	// in the try/catch loop
	public void ansFailed() {
		if (gui) {
			disp.receiveStop();
		}
	}
	
	
	//TODO: consider making Call use this directly, instead of setting values for a 
	//		polling gui
	public void updateGuiConnected(boolean connected, Call c) {
		if (gui) {
			disp.setConnected(connected, c);
		}
	}
	
	//returns whether the phone was answered, and a call is in progress
	public boolean isConnected() {
		boolean c = false;
		try {
			callflg.acquire();
			c = connected;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return c;
	}
	
	public String getIP() {
		String adr = null;
		try {
			callflg.acquire();
			adr = ip;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return adr;
	}
	
	public int getPort() {
		int prt = -2;
		try {
			callflg.acquire();
			prt = port;
			callflg.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return prt;
	}
	
}
