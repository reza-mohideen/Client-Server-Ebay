import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Base64;

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
	private Media bidSound;

	public void run(String user) throws Exception {
		customer = user;
		initView(user);
		setUpNetworking();

		String encodedPassword = "Z29zdGFyczk5";
		byte[] decodedBytes = Base64.getDecoder().decode(encodedPassword);

		db_factory aws = new db_factory("auctiodb.cq2ovdkgqk2v.us-east-1.rds.amazonaws.com",
				3306,"auctionDB","admin",new String(decodedBytes));

		conn = aws.getConnection();
	}

	private void initView(String user) {

		// set up frame
		JFrame frame = new JFrame(user + " Client Bid Window");
		JPanel mainPanel = new JPanel();
		frame.setSize(650, 600);
		mainPanel.setBackground(new Color(23,63,95));
		frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
		frame.add(mainPanel);

		// add title
		JLabel title = new JLabel("Welcome to eHills Marketplace " + customer);
		title.setFont(new Font("Segoe UI", Font.BOLD, 24));
		title.setForeground(Color.WHITE);
		title.setBounds(10,20,500,25);
		mainPanel.add(title);

		// add table
		String[] columnNames = { "Item Name", "Item Description", "Current Price", "Buy It Now", "Time Remaining", "Availability" };
		itemTable = new JTable() {
			// change color of cell depending on availability value
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
				Component comp = super.prepareRenderer(renderer, row, col);
				Object value = getModel().getValueAt(row, col);
					if (value.equals(false)) {
						comp.setBackground(Color.red);
					} else if (value.equals(true)) {
						comp.setBackground(Color.green);
					} else {
						comp.setBackground(Color.white);
					}

				return comp;
			}
		};;
		itemTable.setPreferredScrollableViewportSize(new Dimension(600,200));
		itemTable.getTableHeader().setFont(Font.getFont("Segoe UI"));
		itemTable.getTableHeader().setBackground(new Color(32, 136, 203));
		itemTable.getTableHeader().setForeground(new Color(255, 255, 255));
		itemTable.setRowHeight(25);
		model = new DefaultTableModel();
		model.setColumnIdentifiers(columnNames);
		itemTable.setModel(model);
		Component c = itemTable.getCellRenderer(0,0).getTableCellRendererComponent(itemTable,null,false,false,0,0);
		c.setBackground(Color.RED);
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

				// prefill button and textfield
				sendButton.setText("Make Bid on " + (model.getValueAt(i,0)).toString() + "!");
				String _ = ((model.getValueAt(i,2)).toString()).replace("$","");
				double price = Double.parseDouble(_) + 1;
				outgoing.setText(String.valueOf(price));

				// grab bid history of item from database
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

		// add title
		JLabel bidHistory = new JLabel("Bid History");
		bidHistory.setFont(new Font("Segoe UI", Font.BOLD, 24));
		bidHistory.setForeground(Color.WHITE);
		//bidHistory.setBounds(10,20,500,25);
		mainPanel.add(bidHistory);

		// bid history text area
		incoming = new JTextArea();
		incoming.setLineWrap(true);
		incoming.setWrapStyleWord(true);
		incoming.setEditable(false);
		JScrollPane qScroller = new JScrollPane(incoming);
		qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		qScroller.setPreferredSize(new Dimension(600,100));
		incoming.setBounds(10,200,400,400);
		mainPanel.add(qScroller);

		// make bid area
		outgoing = new JTextField(20);
		outgoing.setBounds(10,300,165,25);
		mainPanel.add(outgoing);

		// send bid button
		sendButton = new JButton("Make Bid!");
		sendButton.setBackground(new Color(197, 108, 134));
		sendButton.setForeground(Color.WHITE);
		sendButton.setFocusPainted(false);
		sendButton.addActionListener(new SendButtonListener());
		sendButton.setBounds(10,240,80,25);
		mainPanel.add(sendButton);

		// feedback text for user
		success = new JLabel("");
		success.setForeground(new Color(255,255,255));
		//success.setBounds(10,300,300,25);
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

	// send bid to server
	class SendButtonListener implements ActionListener {
		String soundFile = "mixkit-trumpet-fanfare-2293.wav";
		Media loginSound = new Media(new File(soundFile).toURI().toString());
		MediaPlayer player = new MediaPlayer(loginSound);
		public void actionPerformed(ActionEvent ev) {

			int item_id = itemTable.getSelectedRow();
			writer.println(Integer.toString(item_id) + "," + model.getValueAt(item_id, 0).toString() + "," + outgoing.getText() + "," + customer);
			writer.flush();
			outgoing.setText("");
			outgoing.requestFocus();

			player.play();

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

					// on startup at items to table
					if (server_message[0].equals("initializeTable")) {
						AuctionItem item = new AuctionItem();
						item.stringToItem(message);
					String[] table_data = new String[] {item.getItemName(), item.getItemDescription(),
							"$"+String.valueOf(item.getPrice())+"0", "$"+String.valueOf(item.getBuyItNow())+"0",
							String.valueOf(item.getTimeRemaining()), String.valueOf(item.getSold()),
							String.valueOf(item.getExpired())};
					model.addRow(table_data);

					}

					// update feedback for client
					else if (server_message[0].equals("success")) {
						success.setText(server_message[1]);
					}

					// update values of table
					else if (server_message[0].equals("updateTable")) {
						AuctionItem item = new AuctionItem();
						item.stringToItem(message);
						model.setValueAt(item.getItemName(),Integer.parseInt(server_message[8]), 0);
						model.setValueAt(item.getItemDescription(),Integer.parseInt(server_message[8]), 1);
						model.setValueAt("$"+String.valueOf(item.getPrice())+"0",Integer.parseInt(server_message[8]), 2);
						model.setValueAt("$"+String.valueOf(item.getBuyItNow())+"0",Integer.parseInt(server_message[8]), 3);
						model.setValueAt(item.getTimeRemaining(),Integer.parseInt(server_message[8]), 4);

						if (item.getSold() || item.getExpired()) {
							model.setValueAt(false,Integer.parseInt(server_message[8]), 5);
						}
						else {
							model.setValueAt(true,Integer.parseInt(server_message[8]), 5);
						}
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}