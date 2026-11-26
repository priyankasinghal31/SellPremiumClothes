package com.shaadiswap.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shaadiswap.app.R;
import com.shaadiswap.app.util.Options;

import java.util.HashMap;
import java.util.Map;

/** Collects the name, city and WhatsApp number that buyers will use to reach the seller. */
public class ProfileSetupActivity extends AppCompatActivity {
    private EditText nameEt, whatsappEt;
    private Spinner citySp;
    private Button saveBtn;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_profile_setup);
        nameEt = findViewById(R.id.name);
        whatsappEt = findViewById(R.id.whatsapp);
        citySp = findViewById(R.id.city);
        saveBtn = findViewById(R.id.save);
        progress = findViewById(R.id.progress);

        citySp.setAdapter(Options.adapter(this, getResources().getStringArray(R.array.cities)));

        String phone = user.getPhoneNumber();
        if (phone != null && phone.startsWith("+91") && phone.length() == 13) {
            whatsappEt.setText(phone.substring(3));
        }

        saveBtn.setOnClickListener(v -> save(user));
    }

    private void save(FirebaseUser user) {
        String name = nameEt.getText().toString().trim();
        String whatsapp = whatsappEt.getText().toString().trim();
        if (name.isEmpty()) {
            nameEt.setError(getString(R.string.name_required));
            return;
        }
        if (!whatsapp.matches("[6-9]\\d{9}")) {
            whatsappEt.setError(getString(R.string.invalid_phone));
            return;
        }
        progress.setVisibility(View.VISIBLE);
        saveBtn.setEnabled(false);

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("city", (String) citySp.getSelectedItem());
        data.put("whatsapp", whatsapp);
        data.put("phone", user.getPhoneNumber());
        data.put("createdAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(data)
                .addOnSuccessListener(v -> {
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    saveBtn.setEnabled(true);
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show();
                });
    }
}
