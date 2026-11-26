package com.shaadiswap.app.model;

import java.util.ArrayList;
import java.util.List;

/** One outfit for sale. Stored in the Firestore "listings" collection. */
public class Listing {
    public String id;
    public String sellerId;
    public String sellerName;
    public String sellerPhone;
    public String title;
    public String description;
    public String size;
    public String condition;
    public String occasion;
    public String city;
    /** "active", "sold" or "removed" (set by admin in the Firebase console). */
    public String status;
    public long price;
    public long originalPrice;
    public long createdAt;
    public List<String> imageUrls = new ArrayList<>();

    public Listing() {
    }

    public String firstImage() {
        return (imageUrls == null || imageUrls.isEmpty()) ? null : imageUrls.get(0);
    }

    public boolean isSold() {
        return "sold".equals(status);
    }
}
