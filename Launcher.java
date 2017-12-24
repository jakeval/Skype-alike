import java.util.Scanner;


/*

Thread for each generator




GUI:
----------------------------------------------------
T1:														shouldn't do other processing
 -gui
 -generates text
 
(T2:													
 -listens for data to display
 -displays data)
----------------------------------------------------


HUB CONTROL:
----------------------------------------------------
T3:														responsiveness
 -listen to commands from gui; act on commands
 -manage calls; choose which streams are active

T4:														??? - can I be delayed?
 -listen for new calls
 -create call + socket family
----------------------------------------------------



T5:
 -audio
 
T6:
 -video
 
T7:
 -text


Audio  -  one thread:
 -read from mic
 -write to socket

Video  -  one thread:
 -read from Robot
 -compress
 -write to socket




The rest:
----------------------------------------------------
 -generate audio
 -
----------------------------------------------------


SOCKET IO:	
----------------------------------------------------
T5:
 -write sockets
 
T6:
 -read sockets
----------------------------------------------------


DATA PROCESSING:
----------------------------------------------------
 -listen to mic
 -get video from Robot
 -compress video
 
 -write received audio to speaker
 -display video
----------------------------------------------------


*/

public class Launcher {
	
	public static void main (String args[]) {
		
		BlockingResource r = null;
		if (args.length > 1 && args[1].equals("-nogui")) {
			r = new BlockingResource(false, 128, 128);
		} else {
			r = new BlockingResource(true, 128, 128);
		
			GUI gui = new GUI(r);
			gui.start();
			
			r.disp = gui.disp;
		}
		
		String local = args[0];
		
		BlockingHub hub = new BlockingHub(r, local);
		hub.start();

		
	}

}
