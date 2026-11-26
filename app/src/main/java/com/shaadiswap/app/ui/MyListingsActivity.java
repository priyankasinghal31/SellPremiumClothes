package com.shaadiswap.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.shaadiswap.app.R;
import com.shaadiswap.app.model.Listing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The seller's own listings: mark sold, edit price/text, delete. */
public class MyListingsActivity extends AppCompatActivity implements MyListingAdapter.Actions {
    private MyListingAdapter adapter;
    private TextView empty;
    private ProgressBar progress;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setTitle(R.string.my_listings_title);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        empty = findViewById(R.id.empty);
        progress = findViewById(R.id.progress);
        adapter = new MyListingAdapter(this);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (uid == null) {
            finish();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        db.collection("listings").whereEqualTo("sellerId", uid).get()
                .addOnSuccessListener(snap -> {
                    List<Listing> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Listing l = d.toObject(Listing.class);
                        if (l == null) continue;
                        l.id = d.getId();
                        out.add(l);
                    }
                    Collections.sort(out, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                    progress.setVisibility(View.GONE);
                    adapter.setItems(out);
                    empty.setText(R.string.empty_mine);
                    empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onOpen(Listing l) {
        Intent i = new Intent(this, ListingDetailActivity.class);
        i.putExtra("id", l.id);
        startActivity(i);
    }

    @Override
    public void onToggleSold(Listing l) {
        String next = l.isSold() ? "active" : "sold";
        db.collection("listings").document(l.id).update("status", next)
                .addOnSuccessListener(v -> load())
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onEdit(Listing l) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        EditText titleEt = new EditText(this);
        titleEt.setHint(R.string.title_hint);
        titleEt.setText(l.title);
        EditText priceEt = new EditText(this);
        priceEt.setHint(R.string.price_hint);
        priceEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        priceEt.setText(String.valueOf(l.price));
        EditText descEt = new EditText(this);
        descEt.setHint(R.string.desc_hint);
        descEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        descEt.setText(l.description);
        box.addView(titleEt);
        box.addView(priceEt);
        box.addView(descEt);

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit)
                .setView(box)
                .setPositiveButton(R.string.save, (d, w) -> {
                    long price;
                    try {
                        price = Long.parseLong(priceEt.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        price = 0;
                    }
                    String title = titleEt.getText().toString().trim();
                    if (price <= 0 || title.isEmpty()) {
                        Toast.makeText(this, R.string.err_price, Toast.LENGTH_LONG).show();
                        return;
                    }
                    Map<String, Object> m = new HashMap<>();
                    m.put("title", title);
                    m.put("price", price);
                    m.put("description", descEt.getText().toString().trim());
                    db.collection("listings").document(l.id).update(m)
                            .addOnSuccessListener(v -> load())
                            .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generic,
                                    Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDelete(Listing l) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_confirm)
                .setPositiveButton(R.string.delete, (d, w) ->
                        db.collection("listings").document(l.id).delete()
                                .addOnSuccessListener(v -> {
                                    // Best effort: also remove the photos from Storage.
                                    if (l.imageUrls != null) {
                                        for (String url : l.imageUrls) {
                                            try {
                                                FirebaseStorage.getInstance().getReferenceFromUrl(url).delete();
                                            } catch (IllegalArgumentException ignored) {
                                                // Not a Storage URL.
                                            }
                                        }
                                    }
                                    load();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generic,
                                        Toast.LENGTH_LONG).show()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
