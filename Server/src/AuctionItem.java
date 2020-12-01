import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AuctionItem implements Serializable {

    private String item_name;
    private String item_description;
    private double price;
    private double buy_it_now;
    private double last_bid;
    private String customer;
    private int time_remaining;
    private boolean expired;
    private boolean sold;
    private Timer t;
    private int item_id;
    private PrintWriter writer;

    public AuctionItem() {

    }

    public AuctionItem(String item_name, String item_description, double price, double buy_it_now, int time_remaining, int item_id) {
        this.item_id = item_id;
        this.item_name = item_name;
        this.item_description = item_description;
        this.price = price;
        this.buy_it_now = buy_it_now;
        this.time_remaining = time_remaining;
        this.expired = false;
        this.sold = false;

        this.t = new Timer();

        // countdown timer ever one second
        t.scheduleAtFixedRate(
                new TimerTask() {

                    @Override
                    public void run() {
                        // print countdown time to client table
                        try {
                            for (PrintWriter writer : Server.clientOutputStreams) {

                                writer.println("updateTable" + "," + AuctionItem.this.itemToString() + "," + String.valueOf(AuctionItem.this.item_id));
                                writer.flush();
                            }
                        }
                        catch (NullPointerException e) {
                            System.out.println("No client connected yet");
                        }

                        // decrement time
                        --AuctionItem.this.time_remaining;

                        // end timer if item is sold
                        if (AuctionItem.this.sold == true) {

                            for (PrintWriter writer : Server.clientOutputStreams) {
                                AuctionItem.this.time_remaining = 0;
                                writer.println("updateTable" + "," + AuctionItem.this.itemToString() + "," + String.valueOf(AuctionItem.this.item_id));
                                writer.flush();
                            }
                            t.cancel();
                            t.purge();
                        }

                        // end timer if time run out and send update to client and database table
                        if (AuctionItem.this.time_remaining == 0) {

                            AuctionItem.this.expired = true;
                            AuctionItem.this.sold = true;

                            // update table with winner
                            for (PrintWriter writer : Server.clientOutputStreams) {

                                writer.println("updateTable" + "," + AuctionItem.this.itemToString() + "," + String.valueOf(AuctionItem.this.item_id));
                                writer.flush();

                            }

                            // update database
                            try {
                                AuctionItem.this.updateTable(Server.conn, AuctionItem.this.item_id);
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                            }

                            // try and contact winner client
                            try {
                                AuctionItem.this.writer.println("success, Congratulations! You won the " + AuctionItem.this.item_name + " for $" + String.valueOf(AuctionItem.this.price) + "0");
                                AuctionItem.this.writer.flush();
                            }
                            catch (NullPointerException e) {
                                System.out.println("No clients bid on " + item_name + " when timer ended");
                            }

                            t.cancel();
                            t.purge();
                        }
                    }
                },0,1000
        );
    }


    public void addBid(double bid) {
        price = bid;
    }

    public int getItemId() {return item_id;}
    public String getItemName() {return item_name;}
    public String getItemDescription() {return item_description;}
    public double getPrice() {
        return price;
    }
    public double getBuyItNow() {
        return buy_it_now;
    }
    public boolean getSold() {
        return sold;
    }
    public int getTimeRemaining() {return time_remaining; }
    public String getCustomer() {return customer;}
    public boolean getExpired() {return expired;}
    public PrintWriter getWriter() {return writer;}

    public void setItemName(String s) { item_name = s; }
    public void setItemDescription(String s) { item_description = s;}
    public void setPrice(double p) {
        price = p;
    }
    public void setBuyItNow(double p) {
        buy_it_now = p;
    }
    public void setSold(boolean b) {
        sold = b;
    }
    public void setTimeRemaining(int i) { time_remaining = i; }
    public void setCustomer(String s) { customer = s;}
    public void setLastBid(Double d) { last_bid = d;}
    public void setWriter(PrintWriter w) {writer = w;}

    public String itemToString() {
        String item = item_name + "," + item_description + "," + price + "," + buy_it_now + ","
                + time_remaining + "," + sold + "," + expired;
        return item;
    }

    public AuctionItem stringToItem(String s) {
        this.item_name = s.split(",")[1];
        this.item_description = s.split(",")[2];
        this.price = Double.parseDouble(s.split(",")[3]);
        this.buy_it_now = Double.parseDouble(s.split(",")[4]);
        this.time_remaining = Integer.parseInt(s.split(",")[5]);
        this.sold = Boolean.parseBoolean(s.split(",")[6]);
        this.expired = Boolean.parseBoolean(s.split(",")[7]);

        return this;
    }

    public void updateTable(Connection conn, int item_id) throws SQLException {
        String query = "INSERT INTO items (item_id, item_name, item_description, price, buy_it_now,customer,last_bid,expired,sold) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        PreparedStatement insert = conn.prepareStatement(query);
        insert.setInt(1, item_id);
        insert.setString(2, item_name);
        insert.setString(3, item_description);
        insert.setDouble(4, price);
        insert.setDouble(5, buy_it_now);
        insert.setString(6, customer);
        insert.setDouble(7, last_bid);
        insert.setBoolean(8,expired);
        insert.setBoolean(9,sold);
        insert.executeUpdate();
    }



}