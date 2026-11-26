package com.shaadiswap.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.shaadiswap.app.R;
import com.shaadiswap.app.model.Listing;
import com.shaadiswap.app.util.Fmt;

import java.util.ArrayList;
import java.util.List;

/** Grid card adapter used by the feed and favourites. */
public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.VH> {
    public interface OnClick {
        void onClick(Listing listing);
    }

    private final List<Listing> items = new ArrayList<>();
    private final OnClick onClick;

    public ListingAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public void setItems(List<Listing> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listing, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Listing l = items.get(position);
        Glide.with(h.image).load(l.firstImage()).centerCrop().into(h.image);
        h.title.setText(l.title);
        h.price.setText(Fmt.price(l.price));
        h.meta.setText(l.size + " • " + l.city);
        h.sold.setVisibility(l.isSold() ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> onClick.onClick(l));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title, price, meta, sold;

        VH(View v) {
            super(v);
            image = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            price = v.findViewById(R.id.price);
            meta = v.findViewById(R.id.meta);
            sold = v.findViewById(R.id.sold);
        }
    }
}
