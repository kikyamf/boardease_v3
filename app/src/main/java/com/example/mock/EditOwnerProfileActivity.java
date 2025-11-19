package com.example.mock;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.text.TextWatcher;
import java.net.URLEncoder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditOwnerProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditOwnerProfile";
    private static final String GET_OWNER_PROFILE_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_owner_profile.php";
    private static final String UPDATE_OWNER_PROFILE_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/update_owner_profile.php";
    private static final String UPLOAD_PROFILE_PIC_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/upload_profile_picture.php";
    
    private static final int PICK_IMAGE_REQUEST = 1;
    
    private ImageView ivProfilePic, ivBack;
    private EditText etFirstName, etMiddleName, etLastName, etPhoneNumber, etAddress, btnBirthdate;
    private EditText etBarangay, etDetailedAddress;
    private Spinner spinnerSuffix, spinnerProvince, spinnerMunicipality;
    private Button btnSaveChanges;
    
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private int userId;
    private String currentProfilePicPath = "";
    private Uri selectedImageUri;
    private boolean imageChanged = false;
    
    // Address picker data
    private String selectedProvince = "";
    private String selectedMunicipality = "";
    private String selectedBarangay = "";
    private String selectedDetailedAddress = "";
    private String pendingAddressToParse = ""; // Store address to parse after provinces load

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_owner_profile);
        
        // Get user ID from intent
        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        setupClickListeners();
        loadOwnerProfile();
    }
    
    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        spinnerSuffix = findViewById(R.id.spinnerSuffix);
        btnBirthdate = findViewById(R.id.btnBirthdate);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAddress = findViewById(R.id.etAddress);
        spinnerProvince = findViewById(R.id.spinnerProvince);
        spinnerMunicipality = findViewById(R.id.spinnerMunicipality);
        etBarangay = findViewById(R.id.etBarangay);
        etDetailedAddress = findViewById(R.id.etDetailedAddress);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Setup suffix spinner
        setupSuffixSpinner();
        
        // Initialize address picker
        initializeAddressPicker();
    }
    
    private void setupSuffixSpinner() {
        String[] suffixOptions = {"None", "Jr.", "Sr.", "I", "II", "III", "IV", "V"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, suffixOptions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(0xFF212529); // Black color
                textView.setTextSize(16);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(0xFFFFFFFF); // White text color
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16); // Add padding for better readability
                    textView.setBackgroundColor(0xFF2C2C2C); // Dark gray background (not too black)
                } else {
                    // If view is not TextView, find TextView inside
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (textView != null) {
                        textView.setTextColor(0xFFFFFFFF); // White text color
                        textView.setTextSize(16);
                        textView.setPadding(16, 16, 16, 16);
                    }
                    view.setBackgroundColor(0xFF2C2C2C); // Dark gray background (not too black)
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSuffix.setAdapter(adapter);
    }
    
    private void initializeAddressPicker() {
        // Load provinces first
        loadProvinces();
        
        // Set up province selection listener
        spinnerProvince.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select Province" option
                    String newProvince = parent.getItemAtPosition(position).toString();
                    
                    // If province changed, reset municipality and barangay
                    if (!newProvince.equals(selectedProvince)) {
                        clearMunicipalityAndBarangay();
                        selectedMunicipality = "";
                        selectedBarangay = "";
                        
                        // Set new province and load municipalities
                        selectedProvince = newProvince;
                        loadMunicipalities(selectedProvince);
                    } else {
                        selectedProvince = newProvince;
                    }
                } else {
                    selectedProvince = "";
                    clearMunicipalityAndBarangay();
                    selectedMunicipality = "";
                    selectedBarangay = "";
                }
                updateCompleteAddress();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Set up municipality selection listener
        spinnerMunicipality.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select Municipality" option
                    selectedMunicipality = parent.getItemAtPosition(position).toString();
                } else {
                    selectedMunicipality = "";
                }
                updateCompleteAddress();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Set up barangay input listener
        etBarangay.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                selectedBarangay = s.toString().trim();
                updateCompleteAddress();
            }
        });
        
        // Set up detailed address listener
        etDetailedAddress.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                selectedDetailedAddress = s.toString().trim();
                updateCompleteAddress();
            }
        });
    }
    
    private void loadProvinces() {
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/philippine_address_api.php?action=provinces";
        
        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.getBoolean("success")) {
                        org.json.JSONArray provincesArray = response.getJSONArray("data");
                        
                        String[] provinceNames = new String[provincesArray.length() + 1];
                        provinceNames[0] = "Select Province";
                        
                        for (int i = 0; i < provincesArray.length(); i++) {
                            org.json.JSONObject province = provincesArray.getJSONObject(i);
                            provinceNames[i + 1] = province.getString("name");
                        }
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, provinceNames) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView textView = (TextView) view;
                                textView.setTextColor(0xFF000000); // Pure black for better visibility
                                textView.setTextSize(16);
                                return view;
                            }

                            @Override
                            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                if (view instanceof TextView) {
                                    TextView textView = (TextView) view;
                                    textView.setTextColor(0xFFFFFFFF); // White text color
                                    textView.setTextSize(16);
                                    textView.setPadding(16, 16, 16, 16);
                                    textView.setBackgroundColor(0xFF2C2C2C); // Dark gray background
                                } else {
                                    TextView textView = view.findViewById(android.R.id.text1);
                                    if (textView != null) {
                                        textView.setTextColor(0xFFFFFFFF);
                                        textView.setTextSize(16);
                                        textView.setPadding(16, 16, 16, 16);
                                    }
                                    view.setBackgroundColor(0xFF2C2C2C);
                                }
                                return view;
                            }
                        };
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerProvince.setAdapter(adapter);
                        
                        // If there's a pending address to parse, parse it now
                        if (!pendingAddressToParse.isEmpty()) {
                            parseAndPopulateAddress(pendingAddressToParse);
                            pendingAddressToParse = "";
                        }
                    } else {
                        loadProvincesFallback();
                    }
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    loadProvincesFallback();
                }
            },
            error -> {
                Log.e(TAG, "Network error loading provinces: " + error.getMessage());
                loadProvincesFallback();
            }
        );
        
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
    
    private void loadProvincesFallback() {
        String[] fallbackProvinces = {
            "Select Province",
            "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", "Aurora",
            "Basilan", "Bataan", "Batanes", "Batangas", "Benguet", "Biliran", "Bohol", "Bukidnon", "Bulacan",
            "Cagayan", "Camarines Norte", "Camarines Sur", "Camiguin", "Capiz", "Catanduanes", "Cavite", "Cebu",
            "Cotabato", "Davao del Norte", "Davao del Sur", "Davao Oriental", "Davao de Oro", "Davao Occidental",
            "Dinagat Islands", "Eastern Samar", "Guimaras", "Ifugao", "Ilocos Norte", "Ilocos Sur", "Iloilo",
            "Isabela", "Kalinga", "Laguna", "Lanao del Norte", "Lanao del Sur", "La Union", "Leyte", "Maguindanao",
            "Marinduque", "Masbate", "Metro Manila", "Misamis Occidental", "Misamis Oriental", "Mountain Province",
            "Negros Occidental", "Negros Oriental", "Northern Samar", "Nueva Ecija", "Nueva Vizcaya",
            "Occidental Mindoro", "Oriental Mindoro", "Palawan", "Pampanga", "Pangasinan", "Quezon", "Quirino",
            "Rizal", "Romblon", "Samar", "Sarangani", "Siquijor", "Sorsogon", "South Cotabato", "Southern Leyte",
            "Sultan Kudarat", "Sulu", "Surigao del Norte", "Surigao del Sur", "Tarlac", "Tawi-Tawi", "Zambales",
            "Zamboanga del Norte", "Zamboanga del Sur", "Zamboanga Sibugay"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fallbackProvinces) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(0xFF000000); // Pure black for better visibility
                textView.setTextSize(16);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(0xFFFFFFFF); // White text color
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C); // Dark gray background
                } else {
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (textView != null) {
                        textView.setTextColor(0xFFFFFFFF);
                        textView.setTextSize(16);
                        textView.setPadding(16, 16, 16, 16);
                    }
                    view.setBackgroundColor(0xFF2C2C2C);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(adapter);
        
        // If there's a pending address to parse, parse it now
        if (!pendingAddressToParse.isEmpty()) {
            parseAndPopulateAddress(pendingAddressToParse);
            pendingAddressToParse = "";
        }
    }
    
    private void loadMunicipalities(String province) {
        try {
            String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/philippine_address_api.php?action=municipalities&province_name=" + 
                java.net.URLEncoder.encode(province, "UTF-8");
            
            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            org.json.JSONArray municipalitiesArray = response.getJSONArray("data");
                            
                            if (municipalitiesArray.length() > 0) {
                                String[] municipalityNames = new String[municipalitiesArray.length() + 1];
                                municipalityNames[0] = "Select Municipality";
                                
                                for (int i = 0; i < municipalitiesArray.length(); i++) {
                                    org.json.JSONObject municipality = municipalitiesArray.getJSONObject(i);
                                    municipalityNames[i + 1] = municipality.getString("name");
                                }
                                
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, municipalityNames) {
                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getView(position, convertView, parent);
                                        TextView textView = (TextView) view;
                                        textView.setTextColor(0xFF000000); // Pure black for better visibility
                                        textView.setTextSize(16);
                                        return view;
                                    }

                                    @Override
                                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getDropDownView(position, convertView, parent);
                                        if (view instanceof TextView) {
                                            TextView textView = (TextView) view;
                                            textView.setTextColor(0xFFFFFFFF); // White text color
                                            textView.setTextSize(16);
                                            textView.setPadding(16, 16, 16, 16);
                                            textView.setBackgroundColor(0xFF2C2C2C); // Dark gray background
                                        } else {
                                            TextView textView = view.findViewById(android.R.id.text1);
                                            if (textView != null) {
                                                textView.setTextColor(0xFFFFFFFF);
                                                textView.setTextSize(16);
                                                textView.setPadding(16, 16, 16, 16);
                                            }
                                            view.setBackgroundColor(0xFF2C2C2C);
                                        }
                                        return view;
                                    }
                                };
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerMunicipality.setAdapter(adapter);
                                
                                // Set selected municipality if available
                                if (!selectedMunicipality.isEmpty()) {
                                    for (int i = 0; i < municipalityNames.length; i++) {
                                        if (municipalityNames[i].equals(selectedMunicipality)) {
                                            spinnerMunicipality.setSelection(i);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading municipalities: " + error.getMessage());
                }
            );
            
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error encoding province name: " + e.getMessage());
        }
    }
    
    private void clearMunicipalityAndBarangay() {
        String[] emptyArray = {"Select Municipality"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, emptyArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(0xFF000000); // Pure black for better visibility
                textView.setTextSize(16);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(0xFFFFFFFF); // White text color
                    textView.setTextSize(16);
                    textView.setPadding(16, 16, 16, 16);
                    textView.setBackgroundColor(0xFF2C2C2C); // Dark gray background
                } else {
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (textView != null) {
                        textView.setTextColor(0xFFFFFFFF);
                        textView.setTextSize(16);
                        textView.setPadding(16, 16, 16, 16);
                    }
                    view.setBackgroundColor(0xFF2C2C2C);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMunicipality.setAdapter(adapter);
        clearBarangay();
    }
    
    private void clearBarangay() {
        if (etBarangay != null) {
            etBarangay.setText("");
        }
        selectedBarangay = "";
    }
    
    private void updateCompleteAddress() {
        StringBuilder completeAddress = new StringBuilder();
        
        if (!selectedDetailedAddress.isEmpty()) {
            completeAddress.append(selectedDetailedAddress);
        }
        
        if (!selectedBarangay.isEmpty()) {
            if (completeAddress.length() > 0) completeAddress.append(", ");
            completeAddress.append(selectedBarangay);
        }
        
        if (!selectedMunicipality.isEmpty()) {
            if (completeAddress.length() > 0) completeAddress.append(", ");
            completeAddress.append(selectedMunicipality);
        }
        
        if (!selectedProvince.isEmpty()) {
            if (completeAddress.length() > 0) completeAddress.append(", ");
            completeAddress.append(selectedProvince);
        }
        
        etAddress.setText(completeAddress.toString());
    }
    
    private void parseAndPopulateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return;
        }
        
        // Check if provinces are loaded first
        android.widget.SpinnerAdapter adapter = spinnerProvince.getAdapter();
        if (!(adapter instanceof ArrayAdapter) || adapter == null || adapter.getCount() == 0) {
            // Provinces not loaded yet, store address for later parsing
            pendingAddressToParse = address;
            return;
        }
        
        @SuppressWarnings("unchecked")
        ArrayAdapter<String> provinceAdapter = (ArrayAdapter<String>) adapter;
        
        Log.d(TAG, "Parsing address: " + address);
        
        // Address format from database: "Detailed Address, Barangay, Municipality, Province"
        String[] parts = address.split(",");
        
        // Clean up all parts
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        
        // Try to find province by matching against spinner list (more reliable)
        String foundProvince = null;
        int provinceIndex = -1;
        
        // Check from the end first (province is usually last)
        for (int i = parts.length - 1; i >= 0; i--) {
            String potentialProvince = findProvinceInAddress(parts[i], provinceAdapter);
            if (potentialProvince != null) {
                foundProvince = potentialProvince;
                provinceIndex = i;
                break;
            }
        }
        
        if (foundProvince != null && provinceIndex >= 0) {
            // Found the province, parse the rest
            selectedProvince = foundProvince;
            
            if (provinceIndex == parts.length - 1) {
                // Province is last, parse normally
                if (parts.length >= 4) {
                    selectedDetailedAddress = parts[0];
                    selectedBarangay = parts[1];
                    selectedMunicipality = parts[2];
                } else if (parts.length == 3) {
                    selectedBarangay = parts[0];
                    selectedMunicipality = parts[1];
                } else if (parts.length == 2) {
                    selectedMunicipality = parts[0];
                }
            } else {
                // Province is not last, extract remaining parts
                StringBuilder detailedAddr = new StringBuilder();
                for (int i = 0; i < provinceIndex; i++) {
                    if (i > 0) detailedAddr.append(", ");
                    detailedAddr.append(parts[i]);
                }
                
                if (provinceIndex >= 3) {
                    selectedDetailedAddress = parts[0];
                    selectedBarangay = parts[1];
                    selectedMunicipality = parts[2];
                } else if (provinceIndex == 2) {
                    selectedBarangay = parts[0];
                    selectedMunicipality = parts[1];
                } else if (provinceIndex == 1) {
                    selectedMunicipality = parts[0];
                } else {
                    selectedDetailedAddress = detailedAddr.toString();
                }
            }
        } else {
            // Couldn't find province in spinner list, try standard parsing
            if (parts.length >= 4) {
                selectedDetailedAddress = parts[0];
                selectedBarangay = parts[1];
                selectedMunicipality = parts[2];
                selectedProvince = parts[3]; // Assume last is province
            } else if (parts.length == 3) {
                selectedBarangay = parts[0];
                selectedMunicipality = parts[1];
                selectedProvince = parts[2];
            } else if (parts.length == 2) {
                selectedMunicipality = parts[0];
                selectedProvince = parts[1];
            } else {
                // Single part - show in detailed address, try to find province
                String potentialProvince = findProvinceInAddress(parts[0], provinceAdapter);
                if (potentialProvince != null) {
                    selectedProvince = potentialProvince;
                } else {
                    selectedDetailedAddress = parts[0];
                }
            }
        }
        
        Log.d(TAG, "Parsed - Detailed: " + selectedDetailedAddress + ", Barangay: " + selectedBarangay + 
              ", Municipality: " + selectedMunicipality + ", Province: " + selectedProvince);
        
        // Populate text fields immediately if we have data
        if (!selectedDetailedAddress.isEmpty()) {
            etDetailedAddress.setText(selectedDetailedAddress);
        }
        if (!selectedBarangay.isEmpty()) {
            etBarangay.setText(selectedBarangay);
        }
        
        // Set province spinner if we found a province
        if (!selectedProvince.isEmpty()) {
            setProvinceSelection(selectedProvince);
        }
    }
    
    private String findProvinceInAddress(String text, ArrayAdapter<String> provinceAdapter) {
        if (provinceAdapter == null || text == null || text.trim().isEmpty()) {
            return null;
        }
        
        String cleanText = text.trim();
        
        // Check each province (skip index 0 which is "Select Province")
        // Try exact match first, then contains
        for (int i = 1; i < provinceAdapter.getCount(); i++) {
            String province = provinceAdapter.getItem(i);
            if (province != null) {
                // Exact match (case insensitive)
                if (province.equalsIgnoreCase(cleanText)) {
                    return province;
                }
                // Contains match
                if (cleanText.contains(province) || province.contains(cleanText)) {
                    return province;
                }
            }
        }
        return null;
    }
    
    private void setProvinceSelection(String provinceName) {
        android.widget.SpinnerAdapter spinnerAdapter = spinnerProvince.getAdapter();
        if (spinnerAdapter instanceof ArrayAdapter) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerAdapter;
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(provinceName)) {
                    spinnerProvince.setSelection(i, false); // false to prevent triggering listener
                    selectedProvince = provinceName;
                    // Load municipalities and set municipality after loading
                    loadMunicipalitiesWithCallback(selectedProvince, new Runnable() {
                        @Override
                        public void run() {
                            setMunicipalitySelection(selectedMunicipality);
                        }
                    });
                    break;
                }
            }
        }
    }
    
    private void setMunicipalitySelection(String municipalityName) {
        android.widget.SpinnerAdapter spinnerAdapter = spinnerMunicipality.getAdapter();
        if (spinnerAdapter instanceof ArrayAdapter && !municipalityName.isEmpty()) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerAdapter;
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(municipalityName)) {
                    spinnerMunicipality.setSelection(i, false); // false to prevent triggering listener
                    selectedMunicipality = municipalityName;
                    // Update complete address after a small delay to ensure all fields are set
                    spinnerMunicipality.postDelayed(() -> updateCompleteAddress(), 100);
                    break;
                }
            }
        }
    }
    
    private void loadMunicipalitiesWithCallback(String province, Runnable callback) {
        try {
            String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/philippine_address_api.php?action=municipalities&province_name=" + 
                java.net.URLEncoder.encode(province, "UTF-8");
            
            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            org.json.JSONArray municipalitiesArray = response.getJSONArray("data");
                            
                            if (municipalitiesArray.length() > 0) {
                                String[] municipalityNames = new String[municipalitiesArray.length() + 1];
                                municipalityNames[0] = "Select Municipality";
                                
                                for (int i = 0; i < municipalitiesArray.length(); i++) {
                                    org.json.JSONObject municipality = municipalitiesArray.getJSONObject(i);
                                    municipalityNames[i + 1] = municipality.getString("name");
                                }
                                
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, municipalityNames) {
                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getView(position, convertView, parent);
                                        TextView textView = (TextView) view;
                                        textView.setTextColor(0xFF000000); // Pure black for better visibility
                                        textView.setTextSize(16);
                                        return view;
                                    }

                                    @Override
                                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getDropDownView(position, convertView, parent);
                                        if (view instanceof TextView) {
                                            TextView textView = (TextView) view;
                                            textView.setTextColor(0xFFFFFFFF); // White text color
                                            textView.setTextSize(16);
                                            textView.setPadding(16, 16, 16, 16);
                                            textView.setBackgroundColor(0xFF2C2C2C); // Dark gray background
                                        } else {
                                            TextView textView = view.findViewById(android.R.id.text1);
                                            if (textView != null) {
                                                textView.setTextColor(0xFFFFFFFF);
                                                textView.setTextSize(16);
                                                textView.setPadding(16, 16, 16, 16);
                                            }
                                            view.setBackgroundColor(0xFF2C2C2C);
                                        }
                                        return view;
                                    }
                                };
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerMunicipality.setAdapter(adapter);
                                
                                // Execute callback after municipalities are loaded (on UI thread)
                                if (callback != null) {
                                    runOnUiThread(callback);
                                }
                            }
                        }
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading municipalities: " + error.getMessage());
                }
            );
            
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error encoding province name: " + e.getMessage());
        }
    }
    
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED); // Set result when back is pressed
            finish();
        });
        
        ivProfilePic.setOnClickListener(v -> openImagePicker());
        
        // Find the edit profile overlay if it exists
        ImageView ivEditProfile = findViewById(R.id.ivEditProfile);
        if (ivEditProfile != null) {
            ivEditProfile.setOnClickListener(v -> openImagePicker());
        }
        
        btnBirthdate.setOnClickListener(v -> showDatePicker());
        
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateBirthdateButton();
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    private void updateBirthdateButton() {
        String dateString = dateFormat.format(calendar.getTime());
        btnBirthdate.setText(dateString);
        btnBirthdate.setHint(""); // Remove hint when date is set
    }
    
    private void loadOwnerProfile() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading profile...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        StringRequest request = new StringRequest(Request.Method.POST, GET_OWNER_PROFILE_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            populateForm(jsonResponse);
                        } else {
                            Toast.makeText(EditOwnerProfileActivity.this, 
                                "Failed to load profile: " + jsonResponse.getString("error"), 
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing profile response", e);
                        Toast.makeText(EditOwnerProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error loading profile", error);
                    Toast.makeText(EditOwnerProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }
        };
        
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
    
    private void populateForm(JSONObject profileData) {
        try {
            // Populate text fields
            etFirstName.setText(profileData.optString("f_name", ""));
            etMiddleName.setText(profileData.optString("m_name", ""));
            etLastName.setText(profileData.optString("l_name", ""));
            
            // Set suffix spinner
            String suffix = profileData.optString("suffix", "None");
            String[] suffixOptions = {"None", "Jr.", "Sr.", "I", "II", "III", "IV", "V"};
            for (int i = 0; i < suffixOptions.length; i++) {
                if (suffixOptions[i].equals(suffix)) {
                    spinnerSuffix.setSelection(i);
                    break;
                }
            }
            
            etPhoneNumber.setText(profileData.optString("phone_number", ""));
            
            // Parse and populate address fields
            String address = profileData.optString("p_address", "");
            if (!address.isEmpty()) {
                parseAndPopulateAddress(address);
            }
            
            // Set birthdate
            String birthdate = profileData.optString("birthdate", "");
            if (!birthdate.isEmpty()) {
                btnBirthdate.setText(birthdate);
                btnBirthdate.setHint(""); // Remove hint when date is set
                try {
                    calendar.setTime(dateFormat.parse(birthdate));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing birthdate", e);
                }
            }
            
            // Load profile picture
            String profilePicPath = profileData.optString("profile_picture", "");
            if (!profilePicPath.isEmpty()) {
                currentProfilePicPath = profilePicPath;
                String fullImageUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/" + profilePicPath;
                Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .centerCrop()
                    .into(ivProfilePic);
            } else {
                // Set default profile picture
                ivProfilePic.setImageResource(R.drawable.btn_profile);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error populating form", e);
        }
    }
    
    private void saveProfileChanges() {
        if (!validateForm()) {
            return;
        }
        
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving changes...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // First upload profile picture if changed
        if (imageChanged && selectedImageUri != null) {
            uploadProfilePicture(progressDialog);
        } else {
            updateProfileData(progressDialog, currentProfilePicPath);
        }
    }
    
    private void uploadProfilePicture(ProgressDialog progressDialog) {
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            
            // Resize image to reduce file size
            int maxSize = 800; // Maximum width or height
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            
            if (width > maxSize || height > maxSize) {
                float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
                int newWidth = Math.round(width * ratio);
                int newHeight = Math.round(height * ratio);
                
                originalBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
                Log.d(TAG, "Resized image from " + width + "x" + height + " to " + newWidth + "x" + newHeight);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Reduced quality to 70%
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            
            Log.d(TAG, "Original image size: " + imageBytes.length + " bytes");
            Log.d(TAG, "Encoded image length: " + encodedImage.length());
            
            // Check if image is too large (limit to 1MB)
            if (imageBytes.length > 1024 * 1024) {
                progressDialog.dismiss();
                Toast.makeText(this, "Image is too large. Please select a smaller image.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_PROFILE_PIC_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Upload response: " + response);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                String newProfilePicPath = jsonResponse.getString("profile_picture_path");
                                updateProfileData(progressDialog, newProfilePicPath);
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(EditOwnerProfileActivity.this, 
                                    "Failed to upload profile picture: " + jsonResponse.getString("error"), 
                                    Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            progressDialog.dismiss();
                            Log.e(TAG, "Error parsing upload response", e);
                            Toast.makeText(EditOwnerProfileActivity.this, "Error uploading profile picture", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error uploading profile picture", error);
                        
                        String errorMessage = "Error uploading profile picture";
                        if (error.networkResponse != null) {
                            errorMessage += " (HTTP " + error.networkResponse.statusCode + ")";
                        }
                        
                        Toast.makeText(EditOwnerProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    params.put("profile_picture", encodedImage);
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/x-www-form-urlencoded");
                    return headers;
                }
            };
            
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
            
        } catch (IOException e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateProfileData(ProgressDialog progressDialog, String profilePicPath) {
        StringRequest request = new StringRequest(Request.Method.POST, UPDATE_OWNER_PROFILE_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(EditOwnerProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK); // Set result to indicate successful update
                            finish();
                        } else {
                            Toast.makeText(EditOwnerProfileActivity.this, 
                                "Failed to update profile: " + jsonResponse.getString("error"), 
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing update response", e);
                        Toast.makeText(EditOwnerProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error updating profile", error);
                    Toast.makeText(EditOwnerProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("f_name", etFirstName.getText().toString().trim());
                params.put("m_name", etMiddleName.getText().toString().trim());
                params.put("l_name", etLastName.getText().toString().trim());
                params.put("suffix", spinnerSuffix.getSelectedItem().toString());
                params.put("birthdate", btnBirthdate.getText().toString());
                params.put("phone_number", etPhoneNumber.getText().toString().trim());
                params.put("p_address", etAddress.getText().toString().trim());
                params.put("profile_picture", profilePicPath);
                return params;
            }
        };
        
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
    
    private boolean validateForm() {
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return false;
        }
        
        if (etLastName.getText().toString().trim().isEmpty()) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return false;
        }
        
        if (btnBirthdate.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please select your birthdate", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (etPhoneNumber.getText().toString().trim().isEmpty()) {
            etPhoneNumber.setError("Phone number is required");
            etPhoneNumber.requestFocus();
            return false;
        }
        
        // Validate address fields
        if (spinnerProvince.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a province", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (spinnerMunicipality.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a municipality/city", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (etBarangay.getText().toString().trim().isEmpty()) {
            etBarangay.setError("Barangay is required");
            etBarangay.requestFocus();
            return false;
        }
        
        if (etDetailedAddress.getText().toString().trim().isEmpty()) {
            etDetailedAddress.setError("Detailed address is required");
            etDetailedAddress.requestFocus();
            return false;
        }
        
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                verifyAndSetProfileImage(selectedImageUri);
            }
        }
    }

    /**
     * Verifies and sets profile image if approved
     */
    private void verifyAndSetProfileImage(Uri imageUri) {
        // Show loading message
        Toast.makeText(this, "Verifying profile image...", Toast.LENGTH_SHORT).show();
        
        // Use smart verification (API if available, local if not)
        ImageVerification.verifyImage(this, imageUri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    // Set the image as selected
                    selectedImageUri = imageUri;
                    imageChanged = true;
                    
                    // Use Glide to load the selected image with proper circular clipping
                    Glide.with(EditOwnerProfileActivity.this)
                        .load(imageUri)
                        .centerCrop()
                        .into(ivProfilePic);
                    
                    Toast.makeText(EditOwnerProfileActivity.this, "✅ Profile image approved", Toast.LENGTH_SHORT).show();
                } else {
                    // Show rejection reason
                    Toast.makeText(EditOwnerProfileActivity.this, "❌ Image rejected: " + reason, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(EditOwnerProfileActivity.this, "Image verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}










