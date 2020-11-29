
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class Client {
	private JTextArea incoming;
	private JTextField outgoing;
	private JTable itemTable;
	private DefaultTableModel model;
	private BufferedReader reader;
	private PrintWriter writer;
	private ObjectInputStream object_reader;
	private ObjectOutputStream object_writer;
	private String[][] data = new String[][] {};
	private String customer;

	public void run(String user) throws Exception {
		customer = user;
		initView(user);
		setUpNetworking();
	}

	private void initView(String user) {
		JFrame frame = new JFrame(user + " Client Bid Window");
		JPanel mainPanel = new JPanel();

		String[] columnNames = { "Item Name", "Item Description", "Price", "Buy It Now", "Time Remaining", "Sold" };
		itemTable = new JTable();
		model = new DefaultTableModel();
		model.setColumnIdentifiers(columnNames);
		itemTable.setBounds(30, 40, 200, 50);
		itemTable.setModel(model);
		JScrollPane sp = new JScrollPane(itemTable);
/*
     JButton selectButton = new JButton("View Item History");
     selectButton.addActionListener(new SendButtonListener());
     selectButton.setBounds(10,50,80,25);
*/
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
		//mainPanel.add(selectButton);
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

		object_writer = new ObjectOutputStream(sock.getOutputStream());
		object_reader = new ObjectInputStream(sock.getInputStream());

		System.out.println("networking established");
		Thread readerThread = new Thread(new IncomingReader());
		readerThread.start();
	}

	class SendButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

        int item_id = itemTable.getSelectedRow();
        try {
           object_writer.writeObject(new String[] {Integer.toString(item_id), model.getValueAt(item_id,0).toString(), outgoing.getText(), customer});
        } catch (IOException e) {
           e.printStackTrace();
        }

			writer.println(outgoing.getText());
			writer.flush();
			outgoing.setText("");
			outgoing.requestFocus();
		}
	}

	public static void main(String[] args) {


		try {
			new Client().run("test");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class IncomingReader implements Runnable {
		public void run() {
			String message;

			try {

				System.out.println("Getting Objects");
				List<AuctionItem> items = (List<AuctionItem>) object_reader.readObject();
				int cnt = 0;
				for (AuctionItem item : items) {
					System.out.println(item.getItemName() + ", " + item.getItemDescription() + ", " + item.getPrice());
					String[] table_data = new String[] {item.getItemName(), item.getItemDescription(),
							String.valueOf(item.getPrice()), String.valueOf(item.getBuyItNow()), "0", "0"};
					model.addRow(table_data);
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}


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