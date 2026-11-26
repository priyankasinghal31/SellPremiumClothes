package com.shaadiswap.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.shaadiswap.app.R;
import com.shaadiswap.app.model.Listing;
import com.shaadiswap.app.util.Constants;
import com.shaadiswap.app.util.ImageUtil;
import com.shaadiswap.app.util.Options;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Create a new listing: details, 4-6 photos, free-listing limit check. */
public class AddListingActivity extends AppCompatActivity {
    private final List<Uri> photos = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private EditText titleEt, descEt, priceEt, origPriceEt;
    private Spinner occasionSp, sizeSp, conditionSp, citySp;
    private LinearLayout strip;
    private TextView photosCount;
    private Button publishBtn;
    private ProgressBar progress;
    private String uid;

    private final ActivityResultLauncher<String> picker =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris == null) return;
                for (Uri u : uris) {
                    if (photos.size() >= Constants.MAX_PHOTOS) {
                        Toast.makeText(this, R.string.max_photos, Toast.LENGTH_SHORT).show();
                        break;
                    }
                    if (!photos.contains(u)) photos.add(u);
                }
                refreshPhotos();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_listing);
        setTitle(R.string.add_listing_title);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }

        titleEt = findViewById(R.id.title);
        descEt = findViewById(R.id.description);
        priceEt = findViewById(R.id.price);
        origPriceEt = findViewById(R.id.orig_price);
        occasionSp = findViewById(R.id.occasion);
        sizeSp = findViewById(R.id.size);
        conditionSp = findViewById(R.id.condition);
        citySp = findViewById(R.id.city);
        strip = findViewById(R.id.photo_strip);
        photosCount = findViewById(R.id.photos_count);
        publishBtn = findViewById(R.id.btn_publish);
        progress = findViewById(R.id.progress);

        occasionSp.setAdapter(Options.adapter(this, getResources().getStringArray(R.array.occasion_labels)));
        sizeSp.setAdapter(Options.adapter(this, getResources().getStringArray(R.array.sizes)));
        conditionSp.setAdapter(Options.adapter(this, getResources().getStringArray(R.array.condition_labels)));
        citySp.setAdapter(Options.adapter(this, getResources().getStringArray(R.array.cities)));

        findViewById(R.id.btn_photos).setOnClickListener(v -> picker.launch("image/*"));
        publishBtn.setOnClickListener(v -> publish());
        refreshPhotos();
    }

    private void refreshPhotos() {
        strip.removeAllViews();
        int size = (int) (88 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        for (Uri u : new ArrayList<>(photos)) {
            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, margin, margin, margin);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(u).into(iv);
            iv.setOnClickListener(v -> {
                photos.remove(u);
                refreshPhotos();
            });
            strip.addView(iv);
        }
        photosCount.setText(getString(R.string.photos_count, photos.size()));
    }

    private void publish() {
        String title = titleEt.getText().toString().trim();
        long price = parse(priceEt);
        long orig = parse(origPriceEt);
        if (title.isEmpty()) {
            titleEt.setError(getString(R.string.err_title));
            return;
        }
        if (price <= 0) {
            priceEt.setError(getString(R.string.err_price));
            return;
        }
        if (photos.size() < Constants.MIN_PHOTOS) {
            Toast.makeText(this, R.string.err_photos, Toast.LENGTH_LONG).show();
            return;
        }
        setBusy(true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("listings").whereEqualTo("sellerId", uid).get()
                .addOnSuccessListener(mine -> {
                    if (mine.size() >= Constants.FREE_LISTINGS) {
                        setBusy(false);
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.limit_title)
                                .setMessage(R.string.limit_message)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                        return;
                    }
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(u -> {
                                if (!u.exists()) {
                                    startActivity(new Intent(this, ProfileSetupActivity.class));
                                    finish();
                                    return;
                                }
                                Listing l = new Listing();
                                l.sellerId = uid;
                                l.sellerName = u.getString("name");
                                l.sellerPhone = u.getString("whatsapp");
                                l.title = title;
                                l.description = descEt.getText().toString().trim();
                                l.price = price;
                                l.originalPrice = orig;
                                String[] occKeys = getResources().getStringArray(R.array.occasion_keys);
                                String[] condKeys = getResources().getStringArray(R.array.condition_keys);
                                l.occasion = occKeys[occasionSp.getSelectedItemPosition()];
                                l.condition = condKeys[conditionSp.getSelectedItemPosition()];
                                l.size = (String) sizeSp.getSelectedItem();
                                l.city = (String) citySp.getSelectedItem();
                                l.status = "active";
                                upload(l);
                            })
                            .addOnFailureListener(e -> fail());
                })
                .addOnFailureListener(e -> fail());
    }

    private void upload(Listing l) {
        List<Uri> toUpload = new ArrayList<>(photos);
        executor.execute(() -> {
            try {
                List<String> urls = new ArrayList<>();
                StorageMetadata meta = new StorageMetadata.Builder().setContentType("image/jpeg").build();
                for (Uri u : toUpload) {
                    byte[] bytes = ImageUtil.compress(getContentResolver(), u);
                    StorageReference ref = FirebaseStorage.getInstance()
                            .getReference("listings/" + uid + "/" + UUID.randomUUID() + ".jpg");
                    Tasks.await(ref.putBytes(bytes, meta));
                    urls.add(Tasks.await(ref.getDownloadUrl()).toString());
                }
                runOnUiThread(() -> save(l, urls));
            } catch (Exception e) {
                runOnUiThread(this::fail);
            }
        });
    }

    private void save(Listing l, List<String> urls) {
        l.imageUrls = urls;
        l.createdAt = System.currentTimeMillis();
        DocumentReference ref = FirebaseFirestore.getInstance().collection("listings").document();
        l.id = ref.getId();
        ref.set(l)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, R.string.listing_published, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> fail());
    }

    private void fail() {
        setBusy(false);
        Toast.makeText(this, R.string.upload_failed, Toast.LENGTH_LONG).show();
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        publishBtn.setEnabled(!busy);
    }

    private static long parse(EditText et) {
        try {
            return Long.parseLong(et.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
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
