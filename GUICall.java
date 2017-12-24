import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import java.awt.Dimension;
//import javax.swing.border.*;
//import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

public class GUICall extends JPanel implements ActionListener {

	/*
	public class TabListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if (e.getSource.equals(close)) {
				System.out.println("close!");
			} else if (e.equals(gc.hold)) {
				System.out.println("close!");
			} else if (e.equals(gc.end)) {
				System.out.println("close!");
			}
		}
	}
	

	TabListener tl;
	*/
	
	private static GUICall current;
	
	private Call call;
	
	BlockingResource res;
	String ip;
	String name;
	public int index;

	//tab header:
	public JPanel tab;
	JLabel title;
	JButton close;
	
	//tab body:
	public JButton hold;
	public JButton end;
	
	boolean isActive;
	boolean isSelf;

	/*
	
	Stored:
	 -up to five
	 -
	
	
	
	
	Contents:
	
	title
	close-tab button
	
	hold button
	end button
	
	 Info:
	 ip
	 timer (frozen or active)
	 
	meta:
	 index
	
	*/
	
	//TODO SOON: This will make a new call with the hold state gui defaulting to active;
	//			 should all calls start active?
	//		I may have just fixed above; all calls start active, overriding previous
	public GUICall(String ip, String name, BlockingResource res, boolean active, boolean self, Call c) {
		call = c;
		this.ip = ip;
		this.name = name;
		//this.index = index;
		this.res = res;
		this.isActive = active;
		this.isSelf = self;
		
		if (active && current == null) {
			System.out.println("new current!");
			current = this;
		}
		
		//tl = new TabListener();
		
		tab = new JPanel();
		
		this.title = new JLabel(name);
		
		close = new JButton("x");
		close.addActionListener(this);
		
		tab.setPreferredSize(new Dimension(60, 30));
		tab.setMaximumSize(new Dimension(60, 30));
		//title.setPreferredSize(new Dimension(60, 20));
		title.setMaximumSize(new Dimension(60, 30));
		//close.setPreferredSize(new Dimension(60, 20));
		close.setMaximumSize(new Dimension(10, 10));
		
		
		tab.add(title);
		tab.add(close);
		
		if (active) {
			
			if (current != null && !current.equals(this)) {
				current.hold();
				current = this;
			}
			
			hold = new JButton("hold");
			hold.addActionListener(this);
			this.add(hold);
			if (!self) {
				end = new JButton("end");
				end.addActionListener(this);
				this.add(end);
			}
		} else {
			this.add(new JLabel("Call was not answered"));
		}
		
	}
	
	public void hold() {
		hold.setText("resume");
		res.hold(call);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(close)) {
			System.out.println("close!");
			//TODO: do I need to decrement timerUsers in Display.java? if so, how?
			if (isActive) {
				//TODO: this needs to actually hangup the right call
				//res.hangup();
			}
			((JTabbedPane) this.getParent()).remove(this);
		} else if (e.getSource().equals(end)) {
			//TODO: this needs to actually hang things up
			//res.hangup();
			System.out.println("end!");
			hold.setEnabled(false);
			end.setEnabled(false);
		} else if (e.getSource().equals(hold)) {
			System.out.println("hold/resume!");
			
			if (isSelf) {
				if (hold.getText().equals("hold")) {
					hold.setText("resume");
					res.holdSelf();
				} else {
					if (!current.equals(this)) {
						current.hold();
						current = this;
					}
					hold.setText("hold");
					res.resumeSelf();
				}
			} else if (hold.getText().equals("hold")) {
				hold();
			} else {
				if (!current.equals(this)) {
					current.hold();
					current = this;
				}
				hold.setText("hold");
				res.resume(call);
			}
		}
	}
	
}
