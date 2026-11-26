package com.shaadiswap.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.shaadiswap.app.R;
import com.shaadiswap.app.model.Listing;
import com.shaadiswap.app.util.Options;
import com.shaadiswap.app.util.SimpleItemSelected;
import com.shaadiswap.app.util.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Home feed: latest listings with search and filters. */
public class MainActivity extends AppCompatActivity {
    private final List<Listing> all = new ArrayList<>();
    private ListingAdapter adapter;
    private SwipeRefreshLayout refresh;
    private TextView empty;
    private EditText searchEt, maxPriceEt;
    private Spinner occasionSp, sizeSp, citySp;
    private String[] occasionKeys, sizes, cities;
    private boolean loadFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        occasionKeys = getResources().getStringArray(R.array.occasion_keys);
        sizes = getResources().getStringArray(R.array.sizes);
        cities = getResources().getStringArray(R.array.cities);

        searchEt = findViewById(R.id.search);
        maxPriceEt = findViewById(R.id.filter_max_price);
        occasionSp = findViewById(R.id.filter_occasion);
        sizeSp = findViewById(R.id.filter_size);
        citySp = findViewById(R.id.filter_city);
        empty = findViewById(R.id.empty);
        refresh = findViewById(R.id.refresh);

        occasionSp.setAdapter(Options.adapter(this,
                Options.withAll(this, getResources().getStringArray(R.array.occasion_labels))));
        sizeSp.setAdapter(Options.adapter(this, Options.withAll(this, sizes)));
        citySp.setAdapter(Options.adapter(this, Options.withAll(this, cities)));

        adapter = new ListingAdapter(l -> {
            Intent i = new Intent(this, ListingDetailActivity.class);
            i.putExtra("id", l.id);
            startActivity(i);
        });
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(this, 2));
        list.setAdapter(adapter);

        Runnable onFilter = this::applyFilters;
        searchEt.addTextChangedListener(new SimpleTextWatcher(onFilter));
        maxPriceEt.addTextChangedListener(new SimpleTextWatcher(onFilter));
        occasionSp.setOnItemSelectedListener(new SimpleItemSelected(onFilter));
        sizeSp.setOnItemSelectedListener(new SimpleItemSelected(onFilter));
        citySp.setOnItemSelectedListener(new SimpleItemSelected(onFilter));

        refresh.setOnRefreshListener(this::load);
        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddListingActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) load();
    }

    private void load() {
        refresh.setRefreshing(true);
        FirebaseFirestore.getInstance().collection("listings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .addOnSuccessListener(snap -> {
                    all.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Listing l = d.toObject(Listing.class);
                        if (l == null || "removed".equals(l.status)) continue;
                        l.id = d.getId();
                        all.add(l);
                    }
                    loadFailed = false;
                    refresh.setRefreshing(false);
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    loadFailed = true;
                    refresh.setRefreshing(false);
                    applyFilters();
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_LONG).show();
                });
    }

    private void applyFilters() {
        String q = searchEt.getText().toString().trim().toLowerCase(Locale.ROOT);
        int o = occasionSp.getSelectedItemPosition();
        int s = sizeSp.getSelectedItemPosition();
        int c = citySp.getSelectedItemPosition();
        String occKey = o > 0 ? occasionKeys[o - 1] : null;
        String size = s > 0 ? sizes[s - 1] : null;
        String city = c > 0 ? cities[c - 1] : null;
        long max = 0;
        try {
            max = Long.parseLong(maxPriceEt.getText().toString().trim());
        } catch (NumberFormatException ignored) {
            // No price limit.
        }

        List<Listing> out = new ArrayList<>();
        for (Listing l : all) {
            if (occKey != null && !occKey.equals(l.occasion)) continue;
            if (size != null && !size.equals(l.size)) continue;
            if (city != null && !city.equals(l.city)) continue;
            if (max > 0 && l.price > max) continue;
            if (!q.isEmpty()) {
                String hay = ((l.title == null ? "" : l.title) + " "
                        + (l.description == null ? "" : l.description)).toLowerCase(Locale.ROOT);
                if (!hay.contains(q)) continue;
            }
            out.add(l);
        }
        adapter.setItems(out);
        empty.setText(loadFailed ? R.string.load_failed : R.string.no_listings);
        empty.setVisibility(out.isEmpty() ? TextView.VISIBLE : TextView.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_my_listings) {
            startActivity(new Intent(this, MyListingsActivity.class));
        } else if (id == R.id.menu_favourites) {
            startActivity(new Intent(this, FavouritesActivity.class));
        } else if (id == R.id.menu_language) {
            LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
            String lang = current.isEmpty() ? Locale.getDefault().getLanguage() : current.get(0).getLanguage();
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags("hi".equals(lang) ? "en" : "hi"));
        } else if (id == R.id.menu_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
