import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuctionItem implements Serializable {
    public static List<Double> bids = new ArrayList<>();

    private String item_name;
    private String item_description;
    private double price;
    private double buy_it_now;
    private double last_bid;
    private String customer;
    private int time_remaining;
    private boolean expired;
    private boolean sold;
    private int item_id;

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
    }


    public void addBid(double bid) {
        bids.add(bid);
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

    public List<Double> getBids() {
        return bids;
    }

    public void updateTable(Connection conn, int item_id) throws SQLException {
        String query = "INSERT INTO items (item_id, item_name, item_description, price, buy_it_now, expires_at,customer,last_bid) " +
                "VALUES (?,?,?,?,?,DATE_ADD(CURRENT_TIMESTAMP, INTERVAL " +  Integer.toString(time_remaining) + " SECOND),?,?)";
        PreparedStatement insert = conn.prepareStatement(query);
        insert.setInt(1, item_id);
        insert.setString(2, item_name);
        insert.setString(3, item_description);
        insert.setDouble(4, price);
        insert.setDouble(5, buy_it_now);
        insert.setString(6, customer);
        insert.setDouble(7, last_bid);
        insert.executeUpdate();
    }

}