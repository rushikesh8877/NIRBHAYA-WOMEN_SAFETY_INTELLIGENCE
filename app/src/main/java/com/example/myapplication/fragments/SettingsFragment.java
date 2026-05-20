package com.example.myapplication.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapters.ContactAdapter;
import com.example.myapplication.models.Contact;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private EditText etUsername;
    private MaterialButton btnSaveProfile;
    private RecyclerView rvContacts;
    private ContactAdapter adapter;
    private List<Contact> contactList;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        etUsername = view.findViewById(R.id.etUsername);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        rvContacts = view.findViewById(R.id.rvContacts);
        MaterialButton btnAddContact = view.findViewById(R.id.btnAddContact);

        sharedPreferences = requireContext().getSharedPreferences("NIRBHAYA_PREFS", Context.MODE_PRIVATE);
        gson = new Gson();

        loadUsername();
        loadContacts();

        adapter = new ContactAdapter(contactList, position -> {
            contactList.remove(position);
            saveContacts();
            adapter.notifyItemRemoved(position);
        });

        rvContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvContacts.setAdapter(adapter);

        btnAddContact.setOnClickListener(v -> showAddContactDialog());
        btnSaveProfile.setOnClickListener(v -> saveUsername());

        return view;
    }

    private void loadUsername() {
        String username = sharedPreferences.getString("username", "");
        etUsername.setText(username);
    }

    private void saveUsername() {
        String username = etUsername.getText().toString().trim();
        sharedPreferences.edit().putString("username", username).apply();
        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
    }

    private void loadContacts() {
        String json = sharedPreferences.getString("contacts", null);
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        contactList = gson.fromJson(json, type);

        if (contactList == null) {
            contactList = new ArrayList<>();
        }
    }

    private void saveContacts() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(contactList);
        editor.putString("contacts", json);
        editor.apply();
    }

    private void showAddContactDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_contact, null);
        EditText etName = dialogView.findViewById(R.id.etContactName);
        EditText etPhone = dialogView.findViewById(R.id.etContactPhone);

        new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    if (!name.isEmpty() && !phone.isEmpty()) {
                        contactList.add(new Contact(name, phone));
                        saveContacts();
                        adapter.notifyItemInserted(contactList.size() - 1);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}