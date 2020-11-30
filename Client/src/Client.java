import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Client {
	private JTextArea incoming;
	private JTextField outgoing;
	private JTable itemTable;
	private JLabel success;
	private DefaultTableModel model;
	private BufferedReader reader;
	private PrintWriter writer;
	private String[][] data = new String[][] {};
	private String customer;
	private Connection conn;
	private JButton sendButton;

	public void run(String user) throws Exception {
		customer = user;
		initView(user);
		setUpNetworking();

		db_factory aws = new db_factory("auctiodb.cq2ovdkgqk2v.us-east-1.rds.amazonaws.com",
				3306,"auctionDB","admin","gostars99");

		conn = aws.getConnection();
	}

	private void initView(String user) {
		JFrame frame = new JFrame(user + " Client Bid Window");
		JPanel mainPanel = new JPanel();
		frame.setSize(650, 600);
		frame.getContentPane().add(BorderLayout.CENTER, mainPanel);

		frame.add(mainPanel);

		String[] columnNames = { "Item Name", "Item Description", "Current Price", "Buy It Now", "Time Remaining", "Sold", "Expired" };
		itemTable = new JTable();
		model = new DefaultTableModel();
		model.setColumnIdentifiers(columnNames);
		itemTable.setBounds(30, 40, 200, 50);
		itemTable.setModel(model);
		JScrollPane sp = new JScrollPane(itemTable);
		mainPanel.add(sp);

		// get selected row data From table to textfields
		itemTable.addMouseListener(new MouseAdapter(){

			@Override
			public void mouseClicked(MouseEvent e){

				int i = itemTable.getSelectedRow();
				incoming.setText("");
				String query = "SELECT * FROM items WHERE item_name = '" + (model.getValueAt(i,0)).toString() +
						"' AND customer IS NOT NULL";
				sendButton.setText("Make Bid on " + (model.getValueAt(i,0)).toString() + "!");
				try {
					Statement s = conn.createStatement();
					ResultSet rs = s.executeQuery(query);

					int cnt = 0;
					while (rs.next()) {
						String item_name = rs.getString("item_name");
						double last_bid = rs.getDouble("last_bid");
						String customer = rs.getString("customer");
						Timestamp time = rs.getTimestamp("created_at");
						boolean sold = rs.getBoolean("sold");
						String row = String.valueOf(time) + " - " + item_name + " - $" + String.valueOf(last_bid) + "0 - " + customer;
						if (sold) {
							row = row + " SOLD TO: " + customer;
						}
						else {
							row = row + " STILL FOR SALE";
						}
						incoming.append(row + "\n");
						cnt++;
					}

					if (cnt == 0) {incoming.append("No bids placed yet on " + (model.getValueAt(i,0)).toString());}
				} catch (SQLException throwables) {
					throwables.printStackTrace();
				}

			}
		});

		incoming = new JTextArea();
		incoming.setLineWrap(true);
		incoming.setWrapStyleWord(true);
		incoming.setEditable(false);
		JScrollPane qScroller = new JScrollPane(incoming);
		qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		incoming.setBounds(10,200,400,400);
		mainPanel.add(qScroller);

		outgoing = new JTextField(20);
		outgoing.setBounds(10,300,165,25);
		mainPanel.add(outgoing);

		sendButton = new JButton("Make Bid!");
		sendButton.addActionListener(new SendButtonListener());
		sendButton.setBounds(10,240,80,25);
		mainPanel.add(sendButton);

		success = new JLabel("");
		success.setBounds(10,300,300,25);
		mainPanel.add(success);

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

			int item_id = itemTable.getSelectedRow();
			writer.println(Integer.toString(item_id) + "," + model.getValueAt(item_id,0).toString() + "," + outgoing.getText() + "," + customer);
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
		@Override
		public void run() {

			String message;
			try {
				while (true) {
					message = reader.readLine();
					String[] server_message = message.split(",");
					if (server_message[0].equals("initializeTable")) {
						AuctionItem item = new AuctionItem();
						item.stringToItem(message);
					String[] table_data = new String[] {item.getItemName(), item.getItemDescription(),
							String.valueOf(item.getPrice()), String.valueOf(item.getBuyItNow()),
							String.valueOf(item.getTimeRemaining()), String.valueOf(item.getSold()),
							String.valueOf(item.getExpired())};
					model.addRow(table_data);

					}

					else if (server_message[0].equals("success")) {
						success.setText(server_message[1]);
					}
					else if (server_message[0].equals("updateTable")) {
						AuctionItem item = new AuctionItem();
						item.stringToItem(message);
						model.setValueAt(item.getItemName(),Integer.parseInt(server_message[8]), 0);
						model.setValueAt(item.getItemDescription(),Integer.parseInt(server_message[8]), 1);
						model.setValueAt(String.valueOf(item.getPrice()),Integer.parseInt(server_message[8]), 2);
						model.setValueAt(String.valueOf(item.getBuyItNow()),Integer.parseInt(server_message[8]), 3);
						model.setValueAt(item.getTimeRemaining(),Integer.parseInt(server_message[8]), 4);
						model.setValueAt(item.getSold(),Integer.parseInt(server_message[8]), 5);
						model.setValueAt(item.getExpired(),Integer.parseInt(server_message[8]), 6);
					}

				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}