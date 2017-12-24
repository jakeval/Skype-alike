import java.io.*;
import java.net.*;
import javax.sound.sampled.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.concurrent.Semaphore;
import java.util.Scanner;
/*

-a class that for some resource, reads and writes simultaneously

Writing:
 -it will check a file to see if there is info to send
 -if there is info, it will send it

//TODO: this MUST be blocking - BOOOOO!
//		make this work for non-blocking audio
//		currently, it must block to satisfy the (super lame) AudioInputStream


TODO: 
-one thread, text
-volume
-mute button

*/
public class AudioRW {

	private Writer w;
	private Reader r;
	boolean listen;
	int options;
	RWSemaphore flg;
	
	InputStream in;
	OutputStream out;
	
	boolean open;
	
	Closeable connection;
	
	BlockingResource res;
	
	//TODO: This will drastically hurt performance
	private class RWSemaphore extends Semaphore {
	
		private Semaphore cc;
		private Semaphore rc;
		int count;
		
		public RWSemaphore() {
			super(1);
			count = 0;
			cc = new Semaphore(1);
			rc = new Semaphore(1);
		}
		
		public boolean rwacquire() {
			//System.out.println("aquire");
			try {
				cc.acquire();
				rc.acquire();
				if (count == 0) {
					acquire();
				}
				count++;
				cc.release();
				rc.release();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return AudioRW.this.open;
		}
		
		public void rwrelease() {
			try {
				rc.acquire();
				count--;
				if (count == 0) {
					release();
				}
				rc.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public boolean cacquire() {
			//System.out.println("aquire");
			try {
				cc.acquire();
				acquire();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return AudioRW.this.open;
		}
		
		public void crelease() {
			try {
				release();
				cc.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	//options:
	//0 = inactive; don't read or write
	//1 = read
	//2 = write
	//3 = read + write
	public AudioRW(BlockingResource res, Closeable connection, InputStream in, OutputStream out, int op) {
		options = op;
		this.in = in;
		this.out = out;
		this.res = res;
		this.connection = connection;
		flg = new RWSemaphore();
		open = true;
		try {
			if ((op&1) == 1) {
				r = new Reader(in);
				Thread rt = new Thread(r);
				rt.start();
			}
		
			if ((op&2) == 2) {
				w = new Writer(out);
				Thread wt = new Thread(w);
				wt.start();
			}
		} catch (IOException e) {
			System.out.println("IO Error:");
			e.printStackTrace();
		}
	}
	
	
	
	//TODO: once text stream is implemented, this will change.
	//		the text stream will negotiate the hangup. If intentional, it will 
	//		send/receive a control character indicating so. This will be passed
	//		to the other streams, and they will catch/erase closing errors.
	//		otherwise, it will display the errors
	public void close() throws Exception {
		System.out.println("op " + options + " Try to close: start blocking...");
		flg.cacquire();
		System.out.println("op " + options + " Acquired permit! set to false!");
		open = false;
		connection.close();
		flg.crelease();
	}
	
	
	
	
	//TODO:
	// right now, it ALWAYS listens and writes. It should only write when there is something
	// worth writing.
	private class Writer implements Runnable {
		
		TargetDataLine mic;
		OutputStream out;

		public Writer(OutputStream out) throws IOException {
			this.out = out;
			AudioFormat format = getFormat();
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			try {
				mic = (TargetDataLine) AudioSystem.getLine(info);
				mic.open(format, 1024);
				mic.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			int size = 32;
			byte[] raw = new byte[size];
			int count;
			
			//TODO: also try writing directly to socketchannel
			//ByteBuffer buf = ByteBuffer.wrap(raw);
			
			try {
				while (AudioRW.this.flg.rwacquire()) {
					//flg.rwrelease();
					count = mic.read(raw, 0, size);
					if (count > 0) {
						out.write(raw, 0, count);
						out.flush();
					}
					AudioRW.this.flg.rwrelease();
				}
				
			} catch (Exception e) {
				AudioRW.this.flg.rwrelease();
				try {
					System.out.println("op " + options + " writer close error!");
					connection.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				e.printStackTrace();
			}
		}
	}

	
	private class Reader implements Runnable {
		
		SourceDataLine speaker;
		//AudioInputStream ain;
		AudioFormat format;
		InputStream in;

		public Reader(InputStream in) throws IOException {
			//this.ins = ins;
			
			this.in = in;
			try {
				format = getFormat();
				DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
				speaker = (SourceDataLine) AudioSystem.getLine(info);
				speaker.open(format, 1024);
				speaker.start();
				
				int temp = 2000000;
				
				//ain = new AudioInputStream(in, format, 2000000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			int count;
			
			try {
				int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
				byte raw[] = new byte[bufferSize];
				
				while (AudioRW.this.flg.rwacquire()) {
					//flg.rwrelease();
					if ((count = in.read(raw, 0, raw.length)) == -1) {
						break;
					}
					
					double volume = res.getVolume()/50;
					//System.out.println("volume: " + volume);
					for (int i = 0; i < bufferSize; i++) {
						raw[i] = (byte) ((double)raw[i] * volume);
					}
					
					if (count > 0) {
						//System.out.println("read data; send to speaker: " + count);
						speaker.write(raw, 0, count);
					}
					AudioRW.this.flg.rwrelease();
				}
				
				speaker.drain();
				speaker.close();
			} catch (Exception e) {
				System.out.println("op " + options + " reader close error!");
				try {
					connection.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				AudioRW.this.flg.rwrelease();
				e.printStackTrace();
			}
		}	
	}
	
	public void hold() {
		//hold
	}
	
	private AudioFormat getFormat() {
		float sampleRate = 8000;
		int sampleSizeInBits = 8;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = true;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

}
