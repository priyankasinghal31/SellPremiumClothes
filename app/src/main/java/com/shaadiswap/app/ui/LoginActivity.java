package com.shaadiswap.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shaadiswap.app.R;

import java.util.concurrent.TimeUnit;

/** Phone number + OTP login using Firebase Auth. */
public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private String verificationId;
    private EditText phoneEt, otpEt;
    private Button sendBtn, verifyBtn;
    private ProgressBar progress;

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    signIn(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.otp_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String id,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = id;
                    setLoading(false);
                    otpEt.setVisibility(View.VISIBLE);
                    verifyBtn.setVisibility(View.VISIBLE);
                    otpEt.requestFocus();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();

        phoneEt = findViewById(R.id.phone);
        otpEt = findViewById(R.id.otp);
        sendBtn = findViewById(R.id.send_otp);
        verifyBtn = findViewById(R.id.verify);
        progress = findViewById(R.id.progress);

        sendBtn.setOnClickListener(v -> {
            String phone = phoneEt.getText().toString().trim();
            if (!phone.matches("[6-9]\\d{9}")) {
                phoneEt.setError(getString(R.string.invalid_phone));
                return;
            }
            setLoading(true);
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber("+91" + phone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(callbacks)
                    .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        });

        verifyBtn.setOnClickListener(v -> {
            String code = otpEt.getText().toString().trim();
            if (code.length() != 6 || verificationId == null) {
                otpEt.setError(getString(R.string.invalid_otp));
                return;
            }
            setLoading(true);
            signIn(PhoneAuthProvider.getCredential(verificationId, code));
        });
    }

    private void signIn(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                goNext();
            } else {
                setLoading(false);
                Toast.makeText(this, R.string.login_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** New users set up a profile first; returning users go straight to the feed. */
    private void goNext() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            setLoading(false);
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(d -> {
                    Class<?> next = d.exists() ? MainActivity.class : ProfileSetupActivity.class;
                    Intent i = new Intent(this, next);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendBtn.setEnabled(!loading);
        verifyBtn.setEnabled(!loading);
    }
}
