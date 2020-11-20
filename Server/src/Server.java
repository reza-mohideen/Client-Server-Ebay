
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jdk.nashorn.internal.parser.JSONParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Server {
	private ArrayList<PrintWriter> clientOutputStreams;
	//public AuctionItem banana = new AuctionItem("banana",2);
	public static List<AuctionItem> items = new ArrayList<>();
	public static List<String> itemNames = new ArrayList<>();

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

			// convert JSON array to list of users
			items = new Gson().fromJson(reader, new TypeToken<List<AuctionItem>>() {}.getType());

			// print users
			setItemNames(items);

			// close reader
			reader.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void setItemNames(List<AuctionItem> items) {
		for (AuctionItem item: items) {
			itemNames.add(item.itemName);
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

	private void notifyClients(String message) {


		for (PrintWriter writer : clientOutputStreams) {
			writer.println(message);
			writer.flush();
		}
	}

	class ClientHandler implements Runnable {
		private BufferedReader reader;
		private PrintWriter writer;

		public ClientHandler(Socket clientSocket) throws IOException {
			Socket sock = clientSocket;
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			writer = new PrintWriter(sock.getOutputStream());

			for (AuctionItem item: items) {
				if (item.currentPrice != -1) {
					writer.println(item.itemName + " - Current Bid Price: $" + item.currentPrice +
							" | Buy it Now Price: $" + item.buyItNowPrice);
					writer.flush();
				}
				else {
					writer.println(item.itemName + "has already been sold");
					writer.flush();
				}
			}
		}

		public void run() {
			String message;
			try {
				while ((message = reader.readLine()) != null) {
					System.out.println("read " + message);
					String itemName = message.split(" ")[0];
					int bid = Integer.parseInt(message.split(" ")[1]);
					try {
						// check if item exists
						AuctionItem item = items.get(itemNames.indexOf(itemName));

						if (item.sold == false) {
							// check if bid placed > current price
							if (bid > item.getCurrentPrice()) {
								item.addBid(bid);
								notifyClients("new " + item.itemName + " bid: " + item.getCurrentPrice());
							}
							else {
								writer.println("Invalid Bid: current price of " + itemName + " = " + item.getCurrentPrice());
								writer.flush();
							}

							if (bid >= item.buyItNowPrice) {
								item.sold = true;
								item.currentPrice = -1;
								notifyClients(item.itemName + " has been sold for " + bid);
							}
						}

						else {
							writer.println(itemName + " already sold");
							writer.flush();
						}

					}
					catch (Exception e) {
						writer.println("Invalid Item: Try Again");
						writer.flush();
						e.printStackTrace();
					}


				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
