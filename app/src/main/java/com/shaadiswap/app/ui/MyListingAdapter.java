package com.shaadiswap.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

/** List rows with sold / edit / delete actions for the seller's own listings. */
public class MyListingAdapter extends RecyclerView.Adapter<MyListingAdapter.VH> {
    public interface Actions {
        void onOpen(Listing l);

        void onToggleSold(Listing l);

        void onEdit(Listing l);

        void onDelete(Listing l);
    }

    private final List<Listing> items = new ArrayList<>();
    private final Actions actions;

    public MyListingAdapter(Actions actions) {
        this.actions = actions;
    }

    public void setItems(List<Listing> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_listing, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Listing l = items.get(position);
        Glide.with(h.image).load(l.firstImage()).centerCrop().into(h.image);
        h.title.setText(l.title);
        h.price.setText(Fmt.price(l.price));
        int statusRes = "removed".equals(l.status) ? R.string.status_removed
                : l.isSold() ? R.string.status_sold : R.string.status_active;
        h.status.setText(statusRes);
        h.btnSold.setText(l.isSold() ? R.string.relist : R.string.mark_sold);
        h.btnSold.setEnabled(!"removed".equals(l.status));
        h.itemView.setOnClickListener(v -> actions.onOpen(l));
        h.btnSold.setOnClickListener(v -> actions.onToggleSold(l));
        h.btnEdit.setOnClickListener(v -> actions.onEdit(l));
        h.btnDelete.setOnClickListener(v -> actions.onDelete(l));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title, price, status;
        final Button btnSold, btnEdit, btnDelete;

        VH(View v) {
            super(v);
            image = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            price = v.findViewById(R.id.price);
            status = v.findViewById(R.id.status);
            btnSold = v.findViewById(R.id.btn_sold);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
