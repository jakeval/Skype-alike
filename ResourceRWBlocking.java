import java.io.*;
import java.net.*;
import javax.sound.sampled.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

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
public class ResourceRWBlocking {

	private Writer w;
	private Reader r;
	boolean listen;
	int options;
	
	//options:
	//0 = inactive; don't read or write
	//1 = read
	//2 = write
	//3 = read + write
	public ResourceRWBlocking (SocketChannel audio, int op) {
		options = op;
		try {
			audio.configureBlocking(true);
			w = new Writer(audio);
			r = new Reader(audio);
		} catch (IOException e) {
			System.out.println("IO Error:");
			e.printStackTrace();
		}
		
		if ((op&1) == 1) {
			Thread rt = new Thread(r);
			rt.start();
		}
		
		if ((op&2) == 2) {
			Thread wt = new Thread(w);
			wt.start();
		}
		
		/*
		Thread wt = new Thread(w);
		wt.start();
		Thread rt = new Thread(r);
		rt.start();
		*/
		
	}
	
	
	//TODO:
	// right now, it ALWAYS listens and writes. It should only write when there is something
	// worth writing.
	private class Writer implements Runnable {
		
		TargetDataLine mic;
		//BufferedOutputStream bout;
		SocketChannel out;

		public Writer(SocketChannel out) throws IOException {
			this.out = out;
			AudioFormat format = getFormat();
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			try {
				mic = (TargetDataLine) AudioSystem.getLine(info);
				mic.open(format, 1024);
				mic.start();
				//bout = new BufferedOutputStream(Channels.newOutputStream(out));
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
			ByteBuffer buf = ByteBuffer.wrap(raw);
			
			try {
				while (true) {
					
					//System.out.println(mic.getLevel());
					//System.out.println("remaining: " + buf.remaining());
					count = mic.read(raw, 0, size);
					
					buf.position(0);
					
					
					//System.out.println("Frame out:");
					
					//for (int i = 0; i < raw.length; i++) {
						//System.out.println(raw[i]);
					//}
					
					//System.out.println(mic.getLevel());
					
					//System.out.println(count);
					if (count > 0) {
						//System.out.println("heard data; write to socket: " + count);
						out.write(buf);
						//bout.write(buf, 0, count);
						//bout.flush();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	private class Reader implements Runnable {
		
		SourceDataLine speaker;
		AudioInputStream in;
		AudioFormat format;
		//SocketChannel ins;

		public Reader(SocketChannel ins) throws IOException {
			//this.ins = ins;
			try {
				format = getFormat();
				DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
				speaker = (SourceDataLine) AudioSystem.getLine(info);
				speaker.open(format, 1024);
				speaker.start();
				
				int temp = 2000000;
				
				in = new AudioInputStream(Channels.newInputStream(ins), format, 2000000);
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
				
				//ByteBuffer buf = ByteBuffer.wrap(raw);
				/*
				while ((count = ins.read(buf)) != -1) {
					if (count > 0) {
						System.out.println("Frame in: ");
						for (int i = 0; i < raw.length; i++) {
							System.out.println(raw[i]);
						}
						System.out.println("read data; send to speaker: " + count);
						speaker.write(buf.array(), 0, count);
					}
				}
				*/
				
				
				while ((count = in.read(raw, 0, raw.length)) != -1) {
					if (count > 0) {
						//System.out.println("read data; send to speaker: " + count);
						speaker.write(raw, 0, count);
					}
				}
				
				speaker.drain();
				speaker.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
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
