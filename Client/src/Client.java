
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Client {
	private JTextArea incoming;
	private JTextField outgoing;
	private JTable itemTable;
	private BufferedReader reader;
	private PrintWriter writer;

	public void run(String user) throws Exception {
		initView(user);
		setUpNetworking();
	}

	private void initView(String user) {
		JFrame frame = new JFrame(user + " Client Bid Window");
		JPanel mainPanel = new JPanel();
		String[][] data = {
				{ "Kundan Kumar Jha", "4031", "CSE","NULL" },
				{ "Anand Jha", "6014", "IT","NULL"  }
		};
		String[] columnNames = { "Name", "Roll Number", "Department","Hello" };
		itemTable = new JTable(data,columnNames);
		itemTable.setBounds(30, 40, 200, 50);
		JScrollPane sp = new JScrollPane(itemTable);

		incoming = new JTextArea();
		incoming.setLineWrap(true);
		incoming.setWrapStyleWord(true);
		incoming.setEditable(false);
		JScrollPane qScroller = new JScrollPane(incoming);
		qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		incoming.setBounds(10,200,400,400);

		outgoing = new JTextField(20);
		outgoing.setBounds(10,300,165,25);

		JButton sendButton = new JButton("Make Bid!");
		sendButton.addActionListener(new SendButtonListener());
		sendButton.setBounds(10,240,80,25);

		mainPanel.add(sp);
		mainPanel.add(qScroller);
		mainPanel.add(outgoing);
		mainPanel.add(sendButton);
		frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
		frame.setSize(650, 600);
		frame.setVisible(true);

	}

	private void setUpNetworking() throws Exception {
		@SuppressWarnings("resource")
		Socket sock = new Socket("127.0.0.1", 4242);
		InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
		reader = new BufferedReader(streamReader);
		writer = new PrintWriter(sock.getOutputStream());
		System.out.println("networking established");
		Thread readerThread = new Thread(new IncomingReader());
		readerThread.start();
	}

	class SendButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			writer.println(outgoing.getText());
			writer.flush();
			outgoing.setText("");
			outgoing.requestFocus();
		}
	}
/*
	public static void main(String[] args) {
		try {
			new Client().run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
*/
	class IncomingReader implements Runnable {
		public void run() {
			String message;
			try {
				while ((message = reader.readLine()) != null) {
					
						incoming.append(message + "\n");
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
