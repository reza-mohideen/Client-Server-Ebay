import java.util.ArrayList;
import java.util.List;

public class AuctionItem {
    public static List<Integer> bids = new ArrayList<>();

    //public static int item_id;
    public String item_name;
    public String item_description;
    public double price;
    public double buy_it_now;
    public double last_bid;
    public int customer_id;
    public int time_remaining;
    public boolean expired;
    public boolean sold;

    public AuctionItem() {

    }

    public AuctionItem(String item_name, String item_description, double price, double buy_it_now, int time_remaining) {
        this.item_name = item_name;
        this.item_description = item_description;
        this.price = price;
        this.buy_it_now = buy_it_now;
        //this.last_bid = null;
        //this.customer_id = Integer.parseInt(null);
        this.time_remaining = time_remaining;
        this.expired = false;
        this.sold = false;
    }


    public void addBid(int bid) {
        bids.add(bid);
        price = bid;
    }

    public double getCurrentPrice() {
        return price;
    }

    public List<Integer> getBids() {
        return bids;
    }

}
