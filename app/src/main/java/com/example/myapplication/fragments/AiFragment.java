package com.example.myapplication.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.example.myapplication.services.VoiceDetectionService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AiFragment extends Fragment {

    private TextView statusText, resultText;
    private EditText areaInput;
    private Button btnAI, btnVoiceDetect, btnCheckArea;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai, container, false);

        statusText = view.findViewById(R.id.statusText);
        resultText = view.findViewById(R.id.resultText);
        areaInput = view.findViewById(R.id.areaInput);
        btnAI = view.findViewById(R.id.btnAI);
        btnVoiceDetect = view.findViewById(R.id.btnVoiceDetect);
        btnCheckArea = view.findViewById(R.id.checkArea);

        btnAI.setOnClickListener(v -> simulateRiskDetection());
        btnVoiceDetect.setOnClickListener(v -> toggleVoiceDetection());
        btnCheckArea.setOnClickListener(v -> analyzeAreaSafety());

        return view;
    }

    private void simulateRiskDetection() {
        statusText.setText("Environment: Analyzing...");
        resultText.setText("Analyzing surrounding...");
        resultText.setTextColor(Color.GRAY);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;

            String[] risks = {"Safe", "Moderate Risk", "High Risk"};
            int index = new Random().nextInt(risks.length);
            String currentRisk = risks[index];

            int color;
            String message;

            if (currentRisk.equals("Safe")) {
                color = Color.parseColor("#2E7D32"); // Green
                message = "AI Analysis: Based on current surroundings, the risk level is Safe.";
            } else if (currentRisk.equals("Moderate Risk")) {
                color = Color.parseColor("#EF6C00"); // Orange
                message = "AI Analysis: Warning! Moderate risk detected in your surroundings. Stay alert.";
            } else {
                color = Color.RED;
                message = "AI Analysis: Danger! High risk detected. Please move to a safer location immediately.";
            }

            statusText.setText("Environment: " + currentRisk);
            resultText.setText(message);
            resultText.setTextColor(color);
        }, 2000); // 2-second buffer
    }

    private void analyzeAreaSafety() {
        String area = areaInput.getText().toString().trim();
        if (area.isEmpty()) {
            Toast.makeText(getContext(), "Please enter an area name", Toast.LENGTH_SHORT).show();
            return;
        }

        resultText.setText("Analyzing safety for " + area + "...");
        resultText.setTextColor(Color.GRAY);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;

            String input = area.toLowerCase();
            String tips;
            int color;

            if (input.contains("night") || input.contains("dark") || input.contains("jail") ||
                    input.contains("terror") || input.contains("samal") || input.contains("malegaon") ||
                    input.contains("tcs nashik") || input.contains("rg kar hospital") || 
                    input.contains("abandoned") || input.contains("isolated") || input.contains("bar") || 
                    input.contains("pub") || input.contains("slum") || input.contains("narrow lane") || 
                    input.contains("forest") || input.contains("deserted")) {
                tips = "⚠ High Risk: Avoid isolated or high-risk areas. Stay in well-lit places and keep emergency contacts ready.";
                color = Color.RED;
            } else if (input.contains("station") || input.contains("bus stand") || input.contains("market") || 
                    input.contains("mall") || input.contains("theatre") || input.contains("cinema") || 
                    input.contains("crowd") || input.contains("protest") || input.contains("rally")) {
                tips = "⚠ Moderate Risk: Stay alert in crowded areas. Keep belongings secure and watch your surroundings.";
                color = Color.parseColor("#EF6C00");
            } else if (input.contains("road") || input.contains("highway") || input.contains("bridge") || 
                    input.contains("underpass") || input.contains("parking") || input.contains("atm")) {
                tips = "⚠ Moderate Risk: Use caution in transit areas. Avoid walking alone if possible and share your location.";
                color = Color.parseColor("#EF6C00");
            } else if (input.contains("college") || input.contains("school") || input.contains("home") || 
                    input.contains("residence") || input.contains("library") || input.contains("office") || 
                    input.contains("police station") || input.contains("society")) {
                tips = "✅ Safe: This area is generally considered safe.\nMaintain basic awareness and enjoy your time.";
                color = Color.parseColor("#2E7D32");
            } else {
                tips = "⚠ Moderate Risk: Stay alert.\nAvoid isolated areas and keep emergency contacts ready.";
                color = Color.parseColor("#EF6C00");
            }

            resultText.setText("Safety Tips for " + area + ":\n" + tips);
            resultText.setTextColor(color);
        }, 1500); // 1.5-second buffer
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUIBasedOnService();
    }

    private void updateUIBasedOnService() {
        if (isServiceRunning(VoiceDetectionService.class)) {
            btnVoiceDetect.setText("Stop Ai Voice Detection");
            statusText.setText("Voice Detection: ACTIVE (Background)");
        } else {
            btnVoiceDetect.setText("Start Ai Voice Detection");
            statusText.setText("Voice Detection: INACTIVE");
        }
    }

    private void toggleVoiceDetection() {
        if (isServiceRunning(VoiceDetectionService.class)) {
            stopVoiceService();
        } else {
            checkPermissionsAndStartService();
        }
    }

    private void checkPermissionsAndStartService() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), 200);
            return;
        }

        // Special permissions check (Settings redirects)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
            showOverlayPermissionDialog();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showBackgroundLocationDialog();
                return;
            }
        }

        startVoiceService();
    }

    private void showOverlayPermissionDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Permission Required")
                .setMessage("To make emergency calls from the background, please enable 'Display over other apps' for this app.")
                .setPositiveButton("Go to Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBackgroundLocationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Background Location")
                .setMessage("To send your location during an emergency while the screen is locked, please select 'Allow all the time' in the next screen.")
                .setPositiveButton("Go to Settings", (d, w) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 201);
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> startVoiceService()) // Start anyway if they cancel
                .show();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startVoiceService() {
        Intent intent = new Intent(getContext(), VoiceDetectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent);
        } else {
            requireContext().startService(intent);
        }
        btnVoiceDetect.setText("Stop Ai Voice Detection");
        statusText.setText("Voice Detection: ACTIVE (Background)");
        Toast.makeText(getContext(), "Voice detection service started", Toast.LENGTH_SHORT).show();
    }

    private void stopVoiceService() {
        Intent intent = new Intent(getContext(), VoiceDetectionService.class);
        requireContext().stopService(intent);
        btnVoiceDetect.setText("Start Ai Voice Detection");
        statusText.setText("Voice Detection: INACTIVE");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 200) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                checkPermissionsAndStartService(); // Re-run to check for Overlay/Background
            } else {
                Toast.makeText(getContext(), "Mic, SMS, and Location permissions are required for safety features.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 201) {
            startVoiceService(); // Start after background location attempt
        }
    }
}
