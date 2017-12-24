import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import java.awt.BorderLayout;
//TODO: specify this
import javax.swing.border.*;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JTabbedPane;
import javax.swing.Timer;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.net.InetAddress;

//import java.util.concurrent.TimeUnit;

public class Display extends JPanel implements ActionListener, ChangeListener {

	double height;
	double width;

	double panel_w;
	double panel_h;
	double panel_wmax;
	double panel_hmax;

	JPanel panel;
	JPanel video;
	
	//panel:
	JPanel phone;
	JPanel middle;
	JPanel chat;
	
	//phone:
	JTextField ip;
	JTextField port;
	JButton call;
	
	//middle:				note: later, top=tabs, bottom=general options (offline, etc...)
	JTabbedPane tabs;
	JPanel receiver;
	JPanel options;
	
	//tabs:
	Timer timer;
	JLabel timerLabel;

	//receiver:
	JButton answer;
	JButton ignore;
	
	//options:
	JButton callSelf;
	
	//chat:
	JTextField msg;
	JButton send;
	
	//video:
	JPanel image;
	JPanel controls;
	JSlider volume;
	JToggleButton mute;
	
	
	BlockingResource res;
	
	boolean calling;
	
	long time;
	
	int timerUsers;
	boolean callPending;
	long pendingStart;
	long pendingTimeout;
	String pendingIP;
	
	
	public Display(BlockingResource r, double w, double h) {
		Border border = BorderFactory.createLineBorder(Color.black);
		res = r;
		panel_w = 1/4;
		panel_h = 1;
		
		time = 0;
		
		calling = false;
		
		callPending = false;
		timerUsers = 0;
		pendingStart = 0;
		
		height = h;
		width = w;
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setPreferredSize(new Dimension((int)w, (int)h));
		
//-----------------------------------------------------------------------------		
		
		//panel:
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setPreferredSize(new Dimension((int)w/4, (int)h));
		
		
		//this will be replaced with dynamic resizing; get parent's dimensions upon resize
		double temp = h/4;
		//phone:
		phone = new JPanel();
		phone.setLayout(new BoxLayout(phone, BoxLayout.Y_AXIS));
		phone.setMaximumSize(new Dimension((int)w/4, (int)temp));
		
		//Panel with ip label + input field
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
		p1.setPreferredSize(new Dimension((int)w/4, (int)temp/8));
		//p1.setMinimumSize(new Dimension());
		//p1.setBorder(border);
		
		JLabel l1 = new JLabel("IP: ");
		l1.setPreferredSize(new Dimension((int)w*2/8, (int)temp/8));
		l1.setBorder(border);
		//l1.setMinimumSize(new Dimension(30, ));
		l1.setMaximumSize(new Dimension(100, l1.getPreferredSize().height));
		p1.add(l1);
		
		ip = new JTextField("172.16.52.24");
		ip.setPreferredSize(new Dimension((int)w*6/8, (int)temp/8));
		//ip.setMinimumSize(new Dimension());
		ip.setMaximumSize(new Dimension(1000, l1.getPreferredSize().height));
		ip.setBorder(border);
		p1.add(ip);
		
		//p1.setMaximumSize(new Dimension(new Dimension(30, l1.getPreferredSize().height)));
		
		//p1.setPreferredSize(new Dimension((int)w/4, (int)temp/4));
		
		//Panel with port label + input field
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
		p2.setPreferredSize(new Dimension((int)w/4, (int)temp/8));
		//p2.setMinimumSize(new Dimension());
		//p2.setMaximumSize(new Dimension());
		//p2.setBorder(border);
		
		JLabel l2 = new JLabel("Port: ");
		l2.setPreferredSize(new Dimension((int)w*2/8, (int)temp/8));
		l2.setBorder(border);
		//l2.setMinimumSize(new Dimension());
		l2.setMaximumSize(new Dimension(100, l2.getPreferredSize().height));
		p2.add(l2);
		
		port = new JTextField("");
		port.setPreferredSize(new Dimension((int)w*6/8, (int)temp/8));
		port.setBorder(border);
		//port.setMinimumSize(new Dimension());
		port.setMaximumSize(new Dimension(1000, l2.getPreferredSize().height));
		p2.add(port);
		
		//panel with call button
		JPanel p3 = new JPanel();
		p3.setPreferredSize(new Dimension((int)w/4, (int)temp/3));
		p3.setMinimumSize(new Dimension((int)w/4, (int)temp/3));
		p3.setBorder(border);
		
		call = new JButton("Call");
		call.setPreferredSize(new Dimension((int)w/6, (int)temp/4));
		call.setBorder(border);
		call.addActionListener(this);
		p3.add(call);
		
		phone.add(p1);
		phone.add(Box.createRigidArea(new Dimension(0, 2)));
		phone.add(p2);
		phone.add(Box.createRigidArea(new Dimension(0, 2)));
		phone.add(p3);
		
//-----------------------------------------------------------------------------		

		//middle panel
		middle = new JPanel();
		middle.setPreferredSize(new Dimension((int)w/4, (int)h/3));
		//middle.setBorder(border);
		middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
		
		//tabs:
		tabs = new JTabbedPane(JTabbedPane.TOP);
		//tabs.setVisible(false);
		tabs.setPreferredSize(new Dimension((int)w/4, (int)h*3/12));
		//tabs.setBorder(border);
		
		//timer:
		timer = new Timer(1000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				time++;
				if (callPending && (pendingStart - time) > pendingTimeout) {
					answer(false);
				}
			}
		});
		
		
		receiver = new JPanel();
		receiver.setPreferredSize(new Dimension((int)w/4, (int)h*1/12));
		receiver.setMaximumSize(new Dimension((int)w/4, 80));
		receiver.setBorder(border);
		receiver.setVisible(false);
		
		//answer:
		answer = new JButton("Answer");
		answer.addActionListener(this);
		//ignore:
		ignore = new JButton("Ignore");
		ignore.addActionListener(this);
		
		receiver.add(answer);
		receiver.add(ignore);
		
		//options:
		options = new JPanel();
		options.setPreferredSize(new Dimension((int)w/4, (int)h/12));
		options.setMaximumSize(new Dimension((int)w/4, 40));
		options.setBorder(border);
		
		callSelf = new JButton("Call Self");
		callSelf.setPreferredSize(new Dimension((int)w/10, 25));
		callSelf.setBorder(border);
		callSelf.addActionListener(this);
		
		options.add(callSelf);
		
		middle.add(tabs);
		middle.add(Box.createRigidArea(new Dimension(0, 2)));
		middle.add(receiver);
		middle.add(Box.createRigidArea(new Dimension(0, 2)));
		middle.add(options);
		
//-----------------------------------------------------------------------------		
		
		//Chat panel
		chat = new JPanel();
		chat.setPreferredSize(new Dimension((int)w/4, (int)h/3));
		chat.setBorder(border);
		
		msg = new JTextField();
		msg.setPreferredSize(new Dimension(100, 25));
		send = new JButton("Send!");
		send.addActionListener(this);
		
		chat.add(msg);
		chat.add(send);
		
		
//-----------------------------------------------------------------------------		
		
		panel.add(phone);
		panel.add(Box.createRigidArea(new Dimension(0, 2)));
		panel.add(middle);
		panel.add(Box.createRigidArea(new Dimension(0, 2)));
		panel.add(chat);
		

//-----------------------------------------------------------------------------		
		
		
		//video:
		video = new JPanel();
		//video.setLayout();
		video.setPreferredSize(new Dimension((int)w*3/4, (int)h));
		video.setLayout(new BorderLayout(2, 2));
		//video.setBorder(border);
		
		//image:
		image = new JPanel();
		image.setPreferredSize(new Dimension((int)w*3/4, (int)h*7/8));
		image.setBorder(border);
		
		//controls:
		controls = new JPanel();
		controls.setPreferredSize(new Dimension((int)w*3/4, (int)h/8));
		controls.setMaximumSize(new Dimension((int)w*3/4, (int)h/8));
		controls.setBorder(border);
		controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
		
		//Mute button:
		mute = new JToggleButton("mute", false);
		mute.addActionListener(this);
		
		
		//Volume slider:
		volume = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
		volume.addChangeListener(this);
		
		controls.add(mute);
		controls.add(volume);
		
		video.add(image, BorderLayout.CENTER);
		video.add(controls, BorderLayout.SOUTH);
		
		this.add(panel);
		this.add(Box.createRigidArea(new Dimension(2, 0)));
		this.add(video);
	}
	
	
	
	//this indicates that calling is finished; the call either failed or
	//	was answered. Either way, the "call" button is ready to be used again
	public void setConnected(boolean connected, Call c) {
		//TODO: change this
		addCall(pendingIP, connected, false, c);
		call.setText("Call");
	}
	
	
	/*
	
	Make a new tab - general:
	 -if timer is inactive, activate it
	 -add tab to timer
	 -display timer
	
	Active call:
	 -timer
	 -hold/resume button
	 -end button
	 -show which streams are opened
	 
	Making a call:
	 -make new tab
	 -show ringing icon/indicator
	 -show timer
	 -when connected/disconnected, show status
	
	Receiving a call:
	 -answer/ignore button
	
	*/
	
	//called by resource. indicates that user is being called
	public void receiveCall(long timeout, String ip) {
		//start timer if not already started
		//display the answer panel
		timerUsers++;
		pendingStart = time;
		pendingIP = ip;
		callPending = true;
		receiver.setVisible(true);
		if (timerUsers == 1) {
			timer.start();
		}
	}
	
	//TODO: implement this
	//TODO: remember what this does
	public void receiveStop() {
		
	}
	
	private void addCall(String ip, boolean active, boolean self, Call call) {
		
		//TODO: generate a name for each
		GUICall c = new GUICall(ip, "A", res, active, self, call);
		c.index = tabs.indexOfComponent(c);
		
		if (self) {
			tabs.add(ip, c);
		} else {
			tabs.add(c);
			tabs.setTabComponentAt(tabs.indexOfComponent(c), c.tab);
		}
		
	}
	
	private void removeCall(String ip) {
		System.out.println("remove " + tabs.indexOfTab(ip));
		
		tabs.remove(tabs.indexOfTab(ip));
	}
	
	private void answer(boolean a) {
		receiver.setVisible(false);
		//int index = addCall(pendingIP, a, false);
		if (a) {
			callPending = false;
			//addCall(pendingIP, a);
			res.ansCall(true);
		} else {
			callPending = false;
			timerUsers--;
			if (timerUsers == 0) {
				timer.stop();
			}
			//addCall(pendingIP, a);
			res.ansCall(false);
		}
	}
	
	
	//TODO: consider whether changing value should unmute
	//		currently it does. this is easy to change.
	@Override
	public void stateChanged(ChangeEvent e) {
		JSlider src = (JSlider) e.getSource();
		res.setVolume(src.getValue());
		mute.setSelected(false);
		res.setMute(false);
	}
	
	
	
	//BUTTON HANDLING:
	@Override
	public void actionPerformed(ActionEvent e) {
		
		//ANSWER		call answered
		if (e.getSource().equals(answer)) {
			//pickup
			answer(true);
		} 
		//IGNORE		call ignored
		else if (e.getSource().equals(ignore)) {
			answer(false);
		} 
		//MUTE			sound muted
		else if (e.getSource().equals(mute)) {
			
			if(mute.isSelected()) {
				res.setMute(true);
			} else {
				res.setMute(false);
			}
			
		}
		//SEND			send message
		else if (e.getSource().equals(send)) {
			
			res.setText(msg.getText());
			msg.setText("");
			
		}
		//CALLSELF			call self
		else if (e.getSource().equals(callSelf)) {
			//TODO: right now, this simply assumes everything goes fine; since
			//		the user is calling themselves, and since they will pick up automatically,
			//		I skip the "calling..."/etc. phases, and go right to an active call.
			//		Is this safe?
			//call self and automatically pick up
			//make new tab for call
			//display call info in that tab
			//make "callself" say "end"
			
			if (!callSelf.getText().equals("End")) {
				System.out.println("Call self");
				callSelf.setText("End");
				addCall("localhost", true, true, null);
				res.callSelf(true);
			} else {
				removeCall("localhost");
				res.closeSelf(true);
				callSelf.setText("Call self");
			}
			
		} 
		//CALL			call other
		else {
			String sport = port.getText();
			String procip = "";
			//InetAddress procip = null;
			int procport = -1;
			System.out.println("Pressed!");
			if (!res.isCalling() && !res.isConnected()) {
				call.setText("Calling...");
				
				try {
					//procip = InetAddress.getByName(ip.getText());
					procip = ip.getText();
					if (!port.getText().isEmpty()) {
						procport = Integer.parseInt(port.getText());
					}
				
					//transmit data to my sockets
					//TODO: this will block; hopefully only VERY briefly
					res.call(procip, procport);
					
				} catch (Exception ex) {
					//TODO: print out that ip or port is incorrect
					ex.printStackTrace();
				}
			} else {
			
				System.out.println("***This case is obsoleted and should never happen***");
				
				throw new RuntimeException();
			
				//System.out.println("HANGUP!");
				//res.hangup();
				
				//removeCall();
				
			}
		}
	}

}
