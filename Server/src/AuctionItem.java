import java.util.ArrayList;
import java.util.List;

public class AuctionItem {
    public static List<Integer> bids = new ArrayList<>();
    public String itemName;
    public int buyItNowPrice;
    public int currentPrice;
    public boolean sold;

    public AuctionItem() {

    }

    public AuctionItem(String itemName, int currentPrice, int buyItNowPrice) {
        this.itemName = itemName;
        this.buyItNowPrice = buyItNowPrice;
        this.currentPrice = currentPrice;
        this.sold = false;
    }


    public void addBid(int bid) {
        bids.add(bid);
        currentPrice = bid;
    }

    public int getCurrentPrice() {
        return currentPrice;
    }

    public List<Integer> getBids() {
        return bids;
    }

}
