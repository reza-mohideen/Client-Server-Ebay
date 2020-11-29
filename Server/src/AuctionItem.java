import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuctionItem implements Serializable {
    public static List<Integer> bids = new ArrayList<>();

    private String item_name;
    private String item_description;
    private double price;
    private double buy_it_now;
    private double last_bid;
    private String customer;
    private int time_remaining;
    private boolean expired;
    private boolean sold;

    public AuctionItem() {

    }

    public AuctionItem(String item_name, String item_description, double price, double buy_it_now, int time_remaining) {
        this.item_name = item_name;
        this.item_description = item_description;
        this.price = price;
        this.buy_it_now = buy_it_now;
        this.time_remaining = time_remaining;
        this.expired = false;
        this.sold = false;
    }


    public void addBid(int bid) {
        bids.add(bid);
        price = bid;
    }
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

    public void setItemName(String s) { item_name = s; }
    public void setItemDescription(String s) { item_description = s;};
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

    public List<Integer> getBids() {
        return bids;
    }

    public void updateTable(Connection conn, int item_id) throws SQLException {
        String query = "INSERT INTO items (item_id, item_name, item_description, price, buy_it_now, expires_at,customer) " +
                "VALUES (?,?,?,?,?,DATE_ADD(CURRENT_TIMESTAMP, INTERVAL " +  Integer.toString(time_remaining) + " SECOND),?)";
        PreparedStatement insert = conn.prepareStatement(query);
        insert.setInt(1, item_id);
        insert.setString(2, item_name);
        insert.setString(3, item_description);
        insert.setDouble(4, price);
        insert.setDouble(5, buy_it_now);
        insert.setString(6, customer);
        insert.executeUpdate();
    }

}