import javax.swing.JFrame;

/*
Handle threading and interfacing with the rest of the program.
Contain JFrame.
Barrier between graphics and actual program
*/
public class GUI {

	//TODO: shouldn't be public
	public Display disp;
	JFrame frame;
	BlockingResource res;
	
	public GUI(BlockingResource r) {
		frame = new JFrame();
		res = r;
		frame.setSize(800, 650);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		disp = new Display(res, 800, 650);
		frame.add(disp);
	}
	
	public void start() {
		frame.pack();
		frame.setVisible(true);
	}
}
