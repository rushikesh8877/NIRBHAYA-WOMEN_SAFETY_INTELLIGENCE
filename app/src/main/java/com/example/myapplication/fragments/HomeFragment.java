package com.example.myapplication.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.myapplication.R;
import com.example.myapplication.models.Contact;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private Gson gson;
    private FusedLocationProviderClient fusedLocationClient;

    private LinearLayout containerOneTapSos, containerLiveLocation, containerAiScream, containerOfflineSms;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sharedPreferences = requireContext().getSharedPreferences("NIRBHAYA_PREFS", Context.MODE_PRIVATE);
        gson = new Gson();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        containerOneTapSos = view.findViewById(R.id.containerOneTapSos);
        containerLiveLocation = view.findViewById(R.id.containerLiveLocation);
        containerAiScream = view.findViewById(R.id.containerAiScream);
        containerOfflineSms = view.findViewById(R.id.containerOfflineSms);

        View btnSos = view.findViewById(R.id.btnSOS);
        if (btnSos != null) {
            btnSos.setOnClickListener(v -> triggerSOS());
        }

        View cardOneTapSos = view.findViewById(R.id.cardOneTapSos);
        if (cardOneTapSos != null) {
            cardOneTapSos.setOnClickListener(v -> {
                updateSelection(containerOneTapSos);
                // Optional: Add logic for One-tap SOS if different from main SOS button
            });
        }

        View cardLiveLocation = view.findViewById(R.id.cardLiveLocation);
        if (cardLiveLocation != null) {
            cardLiveLocation.setOnClickListener(v -> {
                updateSelection(containerLiveLocation);
                shareLiveLocation();
            });
        }

        View cardAiScream = view.findViewById(R.id.cardAiScream);
        if (cardAiScream != null) {
            cardAiScream.setOnClickListener(v -> {
                updateSelection(containerAiScream);
                BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_ai);
                }
            });
        }

        View cardOfflineSms = view.findViewById(R.id.cardOfflineSms);
        if (cardOfflineSms != null) {
            cardOfflineSms.setOnClickListener(v -> {
                updateSelection(containerOfflineSms);
                sendOfflineSmsOnly();
            });
        }

        return view;
    }

    private void updateSelection(LinearLayout selectedContainer) {
        containerOneTapSos.setBackgroundResource(0);
        containerLiveLocation.setBackgroundResource(0);
        containerAiScream.setBackgroundResource(0);
        containerOfflineSms.setBackgroundResource(0);

        if (selectedContainer != null) {
            selectedContainer.setBackgroundResource(R.drawable.bg_gradient_purple);
        }
    }

    private void shareLiveLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                String mapsUrl = "https://maps.google.com/?q=" + lat + "," + lon;
                String shareMessage = "My current location: " + mapsUrl;

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, "Share Location via"));
            } else {
                Toast.makeText(getContext(), "Unable to fetch location. Please ensure GPS is on.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error fetching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendOfflineSmsOnly() {
        List<Contact> contactList = loadContacts();
        if (contactList.isEmpty()) {
            Toast.makeText(getContext(), "No contacts found.", Toast.LENGTH_SHORT).show();
            return;
        }
        sendSMSAlerts(contactList, "ALERT! Help me! (Offline SMS)");
    }

    private void triggerSOS() {
        List<Contact> contactList = loadContacts();

        if (contactList.isEmpty()) {
            Toast.makeText(getContext(), "No emergency contacts found. Please add them in Settings.", Toast.LENGTH_LONG).show();
            return;
        }

        fetchLocationAndSendSMS(contactList);
    }

    private List<Contact> loadContacts() {
        String json = sharedPreferences.getString("contacts", null);
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        List<Contact> contactList = gson.fromJson(json, type);
        return (contactList != null) ? contactList : new ArrayList<>();
    }

    private void fetchLocationAndSendSMS(List<Contact> contacts) {
        String username = sharedPreferences.getString("username", "User");
        String alertHeader = "🚨 EMERGENCY SOS! 🚨\nAlert: Manual Trigger\n";
        
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            sendSMSAlerts(contacts, alertHeader + username + " is in danger! (Location denied)");
            makeEmergencyCall(contacts.get(0).getPhoneNumber());
            return;
        }

        // Use getCurrentLocation for better accuracy than getLastLocation
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), location -> {
                    String message;
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        message = alertHeader + username + " needs help! Location: https://maps.google.com/?q=" + lat + "," + lon;
                    } else {
                        message = alertHeader + username + " needs help! Location not available.";
                    }
                    sendSMSAlerts(contacts, message);
                    makeEmergencyCall(contacts.get(0).getPhoneNumber());
                })
                .addOnFailureListener(e -> {
                    sendSMSAlerts(contacts, alertHeader + username + " is in danger! Location failed.");
                    makeEmergencyCall(contacts.get(0).getPhoneNumber());
                });
    }

    private void sendSMSAlerts(List<Contact> contacts, String message) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "SMS Permission required!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SmsManager smsManager;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                smsManager = requireContext().getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }

            ArrayList<String> parts = smsManager.divideMessage(message);
            for (Contact contact : contacts) {
                String phone = contact.getPhoneNumber().replaceAll("[^0-9+]", "").trim();
                if (!phone.isEmpty()) {
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
                }
            }
            Toast.makeText(getContext(), "SOS Alerts sent to " + contacts.size() + " contacts", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "SMS Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void makeEmergencyCall(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Call Permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }
}