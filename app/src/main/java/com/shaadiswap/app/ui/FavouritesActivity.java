package com.shaadiswap.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shaadiswap.app.R;
import com.shaadiswap.app.model.Listing;

import java.util.ArrayList;
import java.util.List;

/** Outfits the user has saved. */
public class FavouritesActivity extends AppCompatActivity {
    private ListingAdapter adapter;
    private TextView empty;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setTitle(R.string.favourites_title);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        empty = findViewById(R.id.empty);
        progress = findViewById(R.id.progress);
        adapter = new ListingAdapter(l -> {
            Intent i = new Intent(this, ListingDetailActivity.class);
            i.putExtra("id", l.id);
            startActivity(i);
        });
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(this, 2));
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).collection("favourites").get()
                .addOnSuccessListener(favs -> {
                    List<DocumentSnapshot> ids = favs.getDocuments();
                    if (ids.isEmpty()) {
                        show(new ArrayList<>());
                        return;
                    }
                    List<Listing> out = new ArrayList<>();
                    int[] pending = {ids.size()};
                    for (DocumentSnapshot f : ids) {
                        db.collection("listings").document(f.getId()).get()
                                .addOnCompleteListener(t -> {
                                    if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) {
                                        Listing l = t.getResult().toObject(Listing.class);
                                        if (l != null && !"removed".equals(l.status)) {
                                            l.id = t.getResult().getId();
                                            out.add(l);
                                        }
                                    }
                                    if (--pending[0] == 0) show(out);
                                });
                    }
                })
                .addOnFailureListener(e -> show(new ArrayList<>()));
    }

    private void show(List<Listing> list) {
        progress.setVisibility(View.GONE);
        adapter.setItems(list);
        empty.setText(R.string.empty_favs);
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
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
