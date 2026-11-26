package com.shaadiswap.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shaadiswap.app.R;
import com.shaadiswap.app.model.Listing;
import com.shaadiswap.app.util.Fmt;
import com.shaadiswap.app.util.Options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Full listing: photos, details, WhatsApp chat, favourite and report. */
public class ListingDetailActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String uid;
    private Listing listing;
    private DocumentReference favRef;
    private boolean isFav;
    private Button favBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_detail);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        String id = getIntent().getStringExtra("id");
        if (uid == null || id == null) {
            finish();
            return;
        }
        db.collection("listings").document(id).get()
                .addOnSuccessListener(d -> {
                    Listing l = d.toObject(Listing.class);
                    if (l == null || "removed".equals(l.status)) {
                        Toast.makeText(this, R.string.listing_not_found, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    l.id = d.getId();
                    listing = l;
                    bind();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void bind() {
        findViewById(R.id.progress).setVisibility(View.GONE);
        findViewById(R.id.content).setVisibility(View.VISIBLE);
        Listing l = listing;
        setTitle(l.title);

        ViewPager2 pager = findViewById(R.id.pager);
        pager.setAdapter(new PhotoAdapter(l.imageUrls));

        ((TextView) findViewById(R.id.title)).setText(l.title);
        ((TextView) findViewById(R.id.price)).setText(Fmt.price(l.price));
        TextView orig = findViewById(R.id.orig_price);
        if (l.originalPrice > 0) {
            orig.setText(getString(R.string.original_price, Fmt.price(l.originalPrice)));
        } else {
            orig.setVisibility(View.GONE);
        }
        findViewById(R.id.sold_badge).setVisibility(l.isSold() ? View.VISIBLE : View.GONE);

        String details = getString(R.string.type_line,
                Options.label(this, R.array.occasion_keys, R.array.occasion_labels, l.occasion))
                + "\n" + getString(R.string.size_line, l.size)
                + "\n" + getString(R.string.condition_line,
                Options.label(this, R.array.condition_keys, R.array.condition_labels, l.condition))
                + "\n" + l.city;
        ((TextView) findViewById(R.id.details)).setText(details);
        ((TextView) findViewById(R.id.description)).setText(l.description);
        ((TextView) findViewById(R.id.seller)).setText(getString(R.string.seller_label, l.sellerName, l.city));

        boolean mine = uid.equals(l.sellerId);
        Button wa = findViewById(R.id.btn_whatsapp);
        Button report = findViewById(R.id.btn_report);
        favBtn = findViewById(R.id.btn_fav);

        if (mine) {
            wa.setVisibility(View.GONE);
            report.setVisibility(View.GONE);
            favBtn.setVisibility(View.GONE);
            return;
        }

        wa.setOnClickListener(v -> openWhatsApp());
        report.setOnClickListener(v -> showReportDialog());

        favRef = db.collection("users").document(uid).collection("favourites").document(l.id);
        favRef.get().addOnSuccessListener(d -> {
            isFav = d.exists();
            updateFavButton();
        });
        updateFavButton();
        favBtn.setOnClickListener(v -> toggleFavourite());
    }

    private void openWhatsApp() {
        String phone = listing.sellerPhone == null ? "" : listing.sellerPhone.replaceAll("\\D", "");
        if (phone.length() == 10) phone = "91" + phone;
        String msg = getString(R.string.wa_message, listing.title);
        Uri uri = Uri.parse("https://wa.me/" + phone + "?text=" + Uri.encode(msg));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.whatsapp_missing, Toast.LENGTH_LONG).show();
        }
    }

    private void updateFavButton() {
        favBtn.setText(isFav ? R.string.favourite_remove : R.string.favourite_add);
    }

    private void toggleFavourite() {
        favBtn.setEnabled(false);
        Runnable done = () -> {
            favBtn.setEnabled(true);
            updateFavButton();
        };
        if (isFav) {
            favRef.delete().addOnSuccessListener(v -> {
                isFav = false;
                done.run();
            }).addOnFailureListener(e -> done.run());
        } else {
            Map<String, Object> m = new HashMap<>();
            m.put("createdAt", System.currentTimeMillis());
            favRef.set(m).addOnSuccessListener(v -> {
                isFav = true;
                done.run();
            }).addOnFailureListener(e -> done.run());
        }
    }

    private void showReportDialog() {
        String[] reasons = getResources().getStringArray(R.array.report_reasons);
        String[] keys = getResources().getStringArray(R.array.report_reason_keys);
        int[] chosen = {0};
        new AlertDialog.Builder(this)
                .setTitle(R.string.report_title)
                .setSingleChoiceItems(reasons, 0, (d, which) -> chosen[0] = which)
                .setPositiveButton(R.string.report_send, (d, w) -> {
                    Map<String, Object> r = new HashMap<>();
                    r.put("listingId", listing.id);
                    r.put("reporterId", uid);
                    r.put("reason", keys[chosen[0]]);
                    r.put("createdAt", System.currentTimeMillis());
                    db.collection("reports").add(r)
                            .addOnSuccessListener(x -> Toast.makeText(this, R.string.report_thanks,
                                    Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generic,
                                    Toast.LENGTH_LONG).show());
                })
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

    private static class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.VH> {
        private final List<String> urls;

        PhotoAdapter(List<String> urls) {
            this.urls = urls;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Glide.with(h.iv).load(urls.get(position)).into(h.iv);
        }

        @Override
        public int getItemCount() {
            return urls == null ? 0 : urls.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView iv;

            VH(ImageView iv) {
                super(iv);
                this.iv = iv;
            }
        }
    }
}
