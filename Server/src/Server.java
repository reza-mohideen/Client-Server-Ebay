
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jdk.nashorn.internal.parser.JSONParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class Server{
	private ArrayList<PrintWriter> clientOutputStreams;
	//public AuctionItem banana = new AuctionItem("banana",2);
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
					"  `customer` int(11) DEFAULT NULL," +
					"  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
					"  `expires_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
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
				items_dict.put(item_id, item);
				String query = "INSERT INTO items (item_id, item_name, item_description, price, buy_it_now, expires_at) " +
						"VALUES (?,?,?,?,?,DATE_ADD(CURRENT_TIMESTAMP, INTERVAL " +  item.getTimeRemaining() + " SECOND))";
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


			// print users
			//setItemNames(items);

			// close reader
			reader.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static ResultSet getBidHistory(List<String> user_bid) throws SQLException {
		String item_name = user_bid.get(0);
		int bid = Integer.parseInt(user_bid.get(1));

		String item_query = "SELECT * FROM items WHERE item_name = '" + item_name + "' AND last_bid != NULL";
		Statement s = conn.createStatement();
		ResultSet rs = s.executeQuery(item_query);

		return rs;
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

	private void notifyClients(String message) {


		for (PrintWriter writer : clientOutputStreams) {
			writer.println(message);
			writer.flush();
		}
	}

	class ClientHandler implements Runnable {
		private BufferedReader reader;
		private PrintWriter writer;
		private ObjectInputStream object_reader;
		private ObjectOutputStream object_writer;

		public ClientHandler(Socket clientSocket) throws IOException {
			Socket sock = clientSocket;
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			writer = new PrintWriter(sock.getOutputStream());
			object_reader = new ObjectInputStream(sock.getInputStream());
			object_writer = new ObjectOutputStream(sock.getOutputStream());
			object_writer.writeObject(items);

			for (AuctionItem item: items) {

				if (item.getPrice() != -1) {
					writer.println(item.getItemName() + " - Current Bid Price: $" + item.getPrice() +
							" | Buy it Now Price: $" + item.getBuyItNow());
					writer.flush();
				}
				else {
					writer.println(item.getItemName() + "has already been sold");
					writer.flush();
				}
			}

		}


		public void run() {
			String message;

			try {

				String[] user_bid = (String[]) object_reader.readObject();
				int item_id = Integer.parseInt(user_bid[0]);
				String item_name = user_bid[1];
				int bid = Integer.parseInt(user_bid[2]);
				String customer = user_bid[3];

				AuctionItem bid_item = items_dict.get(item_id);
				if (bid_item.getSold() == false) {
					// check if bid placed > current price
					if (bid > bid_item.getPrice()) {
						bid_item.addBid(bid);
						notifyClients("new " + bid_item.getItemName() + " bid: " + bid_item.getPrice());
						bid_item.updateTable(conn, item_id);
					} else {
						writer.println("Invalid Bid: current price of " + bid_item + " = " + bid_item.getPrice());
						writer.flush();
					}

					if (bid >= bid_item.getBuyItNow()) {
						bid_item.setSold(true);
						bid_item.setPrice(bid);
						notifyClients(bid_item.getItemName() + " has been sold for " + bid);
						bid_item.updateTable(conn, item_id);
					}
				} else {
					writer.println(item_name + " already sold");
					writer.flush();
				}

			} catch (IOException | ClassNotFoundException | SQLException e) {
				e.printStackTrace();
			}
		}
	}

}