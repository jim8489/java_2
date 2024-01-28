package com.java.eventregistrationapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        Button button = findViewById(R.id.button3);
        Button button2 = findViewById(R.id.button2);

        button2.setOnClickListener(v -> scanCode());
        button.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            startActivity(intent);
        });
    }

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null && !result.getContents().isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Result");
            if (isUrl(result.getContents())) {
                builder.setMessage("Scanned link: " + result.getContents());
                showVerificationResult("User Not Registered");
            } else {
                builder.setMessage(result.getContents());
                checkDataInDatabase(result.getContents());
            }
            builder.show();
        } else {
            Toast.makeText(MainActivity.this, "No QR Code Found", Toast.LENGTH_SHORT).show();
        }
    });

    private boolean isUrl(String data) {
        try {
            new URL(data);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private void checkDataInDatabase(String scannedData) {
        usersRef.child(scannedData).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()) {
                    boolean qrCodeScanned = (boolean) dataSnapshot.child("qrCodeScanned").getValue();
                    if (qrCodeScanned) {
                        Log.d("DatabaseCheck", "QR Code Already Scanned");
                        showVerificationResult("QR Code Already Scanned");
                    } else {
                        // Fetch user details
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);
                        String number = dataSnapshot.child("number").getValue(String.class);
                        String college = dataSnapshot.child("college").getValue(String.class);

                        // Display user details
                        showUserDetails(username, email, number, college);
                        Log.d("DatabaseCheck", "User Verified");
                        showVerificationResult("User Verified");
                        // Update qrCodeScanned to true
                        usersRef.child(scannedData).child("qrCodeScanned").setValue(true);
                    }
                } else {
                    Log.d("DatabaseCheck", "User Not Registered");
                    showVerificationResult("User Not Registered");
                    showScannedResult(scannedData);
                }
            } else {
                Log.e("DatabaseCheck", "Error checking user data: " + Objects.requireNonNull(task.getException()).getMessage());
                showVerificationResult("User Not Registered");
            }
        });
    }
    private void showUserDetails(String username, String email, String number, String college) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("User Details");
        String message = "Username: " + username + "\n"
                + "Email: " + email + "\n"
                + "Number: " + number + "\n"
                + "College: " + college;
        builder.setMessage(message);
        builder.show();
    }

    private void showVerificationResult(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
    private void showScannedResult(String scannedData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Scanned Result");
        builder.setMessage("Scanned data: " + scannedData);
        builder.show();
    }
}
