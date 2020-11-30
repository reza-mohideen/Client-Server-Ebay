import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jdk.nashorn.internal.parser.JSONParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;


public class Server{
	public static ArrayList<PrintWriter> clientOutputStreams;
	public static List<AuctionItem> items = new ArrayList<>();
	public static HashMap<Integer, AuctionItem> items_dict = new HashMap<Integer, AuctionItem>();

	public static db_factory aws = new db_factory("auctiodb.cq2ovdkgqk2v.us-east-1.rds.amazonaws.com",
			3306,"auctionDB","admin","gostars99");

	public static Connection conn;

	static {
		try {
			conn = aws.getConnection();
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
	}

	public static void main(String[] args) {
		setUpItems();
		try {
			new Server().setUpNetworking();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void setUpItems() {
		try {
			// create Gson instance
			Gson gson = new Gson();

			// create a reader
			Reader reader = Files.newBufferedReader(Paths.get("items.json"));

			// convert JSON array to list of items
			items = new Gson().fromJson(reader, new TypeToken<List<AuctionItem>>() {}.getType());

			// clear items table in database upon start or restart of server
			System.out.println("Dropping Table...");
			String drop_query = "DROP TABLE IF EXISTS `items`;";

			String create_query = "CREATE TABLE `items` " +
					"  (`id` int(11) NOT NULL AUTO_INCREMENT," +
					"  `item_id` int(11) NOT NULL," +
					"  `item_name` varchar(32) NOT NULL," +
					"  `item_description` varchar(128) NOT NULL," +
					"  `price` double NOT NULL," +
					"  `buy_it_now` double NOT NULL," +
					"  `last_bid` double," +
					"  `customer` varchar(32) DEFAULT NULL," +
					"  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
					"  `expired` tinyint(1) NOT NULL DEFAULT FALSE," +
					"  `sold` tinyint(1) NOT NULL DEFAULT FALSE," +
					"  PRIMARY KEY (`id`)" +
					") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
			Statement s = conn.createStatement();
			int rs = s.executeUpdate(drop_query);
			rs = s.executeUpdate(create_query);

			// input list into database
			System.out.println("Adding items to table...");
			int item_id = 0;
			for (AuctionItem item: items) {
				AuctionItem startTimer = new AuctionItem(item.getItemName(), item.getItemDescription(), item.getPrice(),
						item.getBuyItNow(), item.getTimeRemaining(), item.getItemId());
				items_dict.put(item_id, startTimer);
				String query = "INSERT INTO items (item_id, item_name, item_description, price, buy_it_now) " +
						"VALUES (?,?,?,?,?)";
				System.out.println(item.getItemName());
				PreparedStatement insert = conn.prepareStatement(query);
				insert.setInt(1, item_id);
				insert.setString(2, item.getItemName());
				insert.setString(3, item.getItemDescription());
				insert.setDouble(4, item.getPrice());
				insert.setDouble(5, item.getBuyItNow());
				insert.executeUpdate();
				item_id++;
			}

			// close reader
			reader.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void setUpNetworking() throws Exception {
		clientOutputStreams = new ArrayList<PrintWriter>();
		@SuppressWarnings("resource")
		ServerSocket serverSock = new ServerSocket(4242);
		while (true) {
			Socket clientSocket = serverSock.accept();
			PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
			clientOutputStreams.add(writer);

			Thread t = new Thread(new ClientHandler(clientSocket));
			t.start();
			System.out.println("got a connection");
		}

	}

	private void notifyClients(String message) throws IOException {


		for (PrintWriter writer : clientOutputStreams) {

			writer.println(items_dict);
			writer.flush();
		}
	}

	private void updateClientTable(HashMap<Integer, AuctionItem> items_dict) {
		for (PrintWriter writer : clientOutputStreams) {
			for (Map.Entry<Integer, AuctionItem> entry : items_dict.entrySet()) {
				AuctionItem item = entry.getValue();
				writer.println("updateTable" + "," + item.itemToString() + "," + (entry.getKey()).toString());
				writer.flush();
			}
		}
	}

	class ClientHandler implements Runnable {
		private BufferedReader reader;
		private PrintWriter writer;

		public ClientHandler(Socket clientSocket) throws IOException {
			Socket sock = clientSocket;
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			writer = new PrintWriter(sock.getOutputStream());

			for (Map.Entry<Integer, AuctionItem> entry : items_dict.entrySet()) {
				AuctionItem item = entry.getValue();
				writer.println("initializeTable" + "," + item.itemToString());
				writer.flush();
			}

		}

		@Override
		public void run() {

			String message;
			try {
				while ((message = reader.readLine()) != null) {
					System.out.println("read " + message);
					String[] client_message = message.split(",");
					int item_id = Integer.parseInt(client_message[0]);
					String item_name = client_message[1];
					double bid = Double.parseDouble(client_message[2]);
					String customer = client_message[3];

					AuctionItem bid_item = items_dict.get(item_id);

					if (bid_item.getSold() == false) {
						System.out.println("Bid being placed");

						// check if bid placed is valid
						if (bid > bid_item.getPrice()) {

							bid_item.setWriter(writer);
							// check if bid > buy it now price
							if (bid >= bid_item.getBuyItNow()) {
								bid_item.setSold(true);
								bid_item.setPrice(bid);
								bid_item.setLastBid(Double.valueOf(bid));
								bid_item.setCustomer(customer);

								writer.println("success, Congratulations! You won the " + bid_item.getItemName() + " for $" + String.valueOf(bid_item.getPrice()));
								writer.flush();
							}

							// valid bid < buy it now
							else {
								bid_item.addBid(bid);
								writer.println("success,Sucessfull Bid Placed");
								writer.flush();

								bid_item.setPrice(bid);
								bid_item.setLastBid(Double.valueOf(bid));
								bid_item.setCustomer(customer);
//
//								bid_item.updateTable(conn, item_id);
//								items_dict.put(item_id, bid_item);
//								updateClientTable(items_dict);
							}

							bid_item.updateTable(conn, item_id);
							items_dict.put(item_id, bid_item);
							updateClientTable(items_dict);
						}

						// bid lower than current price
						else {
							writer.println("success,Invalid Bid: Current price of " + bid_item.getItemName() + " is $" + bid_item.getPrice());
							writer.flush();
						}


				}

				//
				else if (bid_item.getExpired() == true) {
						writer.println("success," + item_name + " time already expired");
						writer.flush();
				}
				else {
					writer.println("success," + item_name + " already sold");
					writer.flush();
				}

				}
			} catch (IOException | SQLException e) {
				e.printStackTrace();
			}
		}
	}

}