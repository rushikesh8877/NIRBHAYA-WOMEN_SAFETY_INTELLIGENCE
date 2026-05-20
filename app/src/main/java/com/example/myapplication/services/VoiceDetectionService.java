package com.example.myapplication.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.models.Contact;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class VoiceDetectionService extends Service implements SensorEventListener {

    private static final String TAG = "VoiceDetectionService";
    private static final String CHANNEL_ID = "VoiceDetectionChannel";

    private SharedPreferences sharedPreferences;
    private Gson gson;
    private FusedLocationProviderClient fusedLocationClient;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    
    // Shake detection settings
    private static final float SHAKE_THRESHOLD = 25.0f;
    private long lastTriggerTime = 0;
    private static final long COOLDOWN_MS = 5000;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean isListening = false;

    // Keywords to trigger SOS
    private static final String[] KEYWORDS = {"help", "save me", "emergency", "stop", "bachao", "nirbhaya"};

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("NIRBHAYA_PREFS", Context.MODE_PRIVATE);
        gson = new Gson();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Initialize Speech Recognizer
        initializeSpeechRecognizer();
        createNotificationChannel();
        startForegroundService();
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NIRBHAYA Protection Active")
                .setContentText("Listening for emergency keywords and shakes")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Use a better icon if you have one
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE | ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            
            // Attempt to keep the session alive for a longer duration (up to 3 minutes)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 180000); // 3 minutes
                speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 180000);
                speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 180000);
            }

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {}
                @Override
                public void onBeginningOfSpeech() {}
                @Override
                public void onRmsChanged(float rmsdB) {}
                @Override
                public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {}
                @Override
                public void onError(int error) {
                    // Restart listening if it times out or errors
                    if (isListening) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (isListening) speechRecognizer.startListening(speechIntent);
                        }, 2000);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    processResults(results);
                    if (isListening) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (isListening) speechRecognizer.startListening(speechIntent);
                        }, 2000);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    processResults(partialResults);
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private void processResults(Bundle bundle) {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            for (String match : matches) {
                String result = match.toLowerCase();
                for (String keyword : KEYWORDS) {
                    if (result.contains(keyword)) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastTriggerTime > COOLDOWN_MS) {
                            lastTriggerTime = currentTime;
                            Log.d(TAG, "Keyword Detected: " + keyword);
                            triggerSOS("Voice Keyword: " + keyword);
                        }
                        return;
                    }
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startVoiceDetection();
        return START_STICKY;
    }

    private void startVoiceDetection() {
        if (isListening) return;
        isListening = true;
        if (speechRecognizer != null) {
            speechRecognizer.startListening(speechIntent);
            Log.d(TAG, "Keyword detection started");
        }
    }

    private double calculateAmplitude(short[] buffer, int readSize) {
        long sum = 0;
        for (int i = 0; i < readSize; i++) {
            sum += Math.abs(buffer[i]);
        }
        return (double) sum / readSize;
    }

    private void triggerSOS(String triggerType) {
        Log.d(TAG, "SOS TRIGGERED: " + triggerType);
        List<Contact> contactList = loadContacts();
        if (!contactList.isEmpty()) {
            fetchLocationAndSendSMS(contactList, triggerType);
        }
    }

    private List<Contact> loadContacts() {
        String json = sharedPreferences.getString("contacts", null);
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        List<Contact> contactList = gson.fromJson(json, type);
        return (contactList != null) ? contactList : new ArrayList<>();
    }

    private void fetchLocationAndSendSMS(List<Contact> contacts, String triggerType) {
        String username = sharedPreferences.getString("username", "User");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            sendSMSAlerts(contacts, "🚨 EMERGENCY SOS! 🚨\nAlert: " + triggerType + "\n" + username + " is in danger. Location permission not granted.");
            makeEmergencyCall(contacts.get(0).getPhoneNumber());
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        sendSOSWithLocation(contacts, username, location.getLatitude(), location.getLongitude(), triggerType);
                    } else {
                        tryBalancedLocation(contacts, username, triggerType);
                    }
                })
                .addOnFailureListener(e -> tryBalancedLocation(contacts, username, triggerType));
    }

    private void tryBalancedLocation(List<Contact> contacts, String username, String triggerType) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        sendSOSWithLocation(contacts, username, location.getLatitude(), location.getLongitude(), triggerType);
                    } else {
                        tryLastKnownLocation(contacts, username, triggerType);
                    }
                })
                .addOnFailureListener(e -> tryLastKnownLocation(contacts, username, triggerType));
    }

    private void tryLastKnownLocation(List<Contact> contacts, String username, String triggerType) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
            if (lastLoc != null) {
                sendSOSWithLocation(contacts, username, lastLoc.getLatitude(), lastLoc.getLongitude(), triggerType);
            } else {
                String message = "🚨 EMERGENCY SOS! 🚨\nAlert: " + triggerType + "\nTriggered by " + username + ". Location not available.";
                sendSMSAlerts(contacts, message);
                makeEmergencyCall(contacts.get(0).getPhoneNumber());
            }
        });
    }

    private void sendSOSWithLocation(List<Contact> contacts, String username, double lat, double lon, String triggerType) {
        String message = "🚨 EMERGENCY SOS! 🚨\nAlert: " + triggerType + "\n" + username + " needs help immediately!\nLocation: https://maps.google.com/?q=" + lat + "," + lon;
        sendSMSAlerts(contacts, message);
        makeEmergencyCall(contacts.get(0).getPhoneNumber());
    }

    private void sendSMSAlerts(List<Contact> contacts, String message) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS Permission not granted in service");
            return;
        }

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = this.getSystemService(SmsManager.class);
            } else {
                // Use the deprecated method for older versions as a fallback
                smsManager = SmsManager.getDefault();
            }

            if (contacts.isEmpty()) {
                Log.e(TAG, "No contacts found to send SMS");
                return;
            }

            ArrayList<String> parts = smsManager.divideMessage(message);
            for (Contact contact : contacts) {
                String phone = contact.getPhoneNumber().replaceAll("[^0-9+]", "").trim();
                if (!phone.isEmpty()) {
                    Log.d(TAG, "Attempting to send SMS to: " + phone);
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
                    Log.d(TAG, "SMS sent command issued for: " + phone);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Failed to send SMS", e);
        }
    }

    private void makeEmergencyCall(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String uri = "tel:" + phoneNumber.trim();
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse(uri));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(callIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "NIRBHAYA Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculate total acceleration
            double acceleration = Math.sqrt(x * x + y * y + z * z);

            // Shake detection (less sensitive threshold)
            if (acceleration > SHAKE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTriggerTime > COOLDOWN_MS) {
                    lastTriggerTime = currentTime;
                    Log.d(TAG, "Shake Detected! Acceleration: " + acceleration);
                    triggerSOS("Shake Detection");
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroy() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
