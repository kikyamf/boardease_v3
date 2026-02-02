package com.example.mock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.ArrayList;

public class AddingBhFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String KEY_SAVED_IMAGES = "saved_bh_images"; // NEW
    
    // Mapbox Access Token
    private static final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoibmFtem1hcDA0IiwiYSI6ImNtanhubnN3MzJncTMzZHFzNHc4azB2MWUifQ.84NPjWYDgq3i20GLhbFTtg";

    // Static variables to preserve data when navigating back
    private static String savedBhName = "";
    private static String savedBhDescription = "";
    private static String savedBhRules = "";
    private static String savedBhBathrooms = "";
    private static String savedBhArea = "";
    private static String savedBhBuildYear = "";
    
    // Address static variables
    private static String savedProvince = "";
    private static String savedMunicipality = "";
    private static String savedBarangay = "";
    private static String savedDetailedAddress = "";
    private static boolean savedAddressConfirmed = false;
    
    private static ArrayList<Uri> savedImageUris = new ArrayList<>();

    private int userId = -1;

    private EditText etBhName, etBhDescription, etBhRules, etBathrooms, etArea, etBuildYear;
    // New Address Fields
    private Spinner spinnerProvince, spinnerMunicipality, spinnerBarangay;
    private EditText etDetailedAddress;
    private WebView webViewMap;
    private Button btnConfirmAddress;
    private TextView tvAddressStatus;
    
    // Address State
    private String selectedProvince = "";
    private String selectedMunicipality = "";
    private String selectedBarangay = "";
    private boolean isAddressConfirmed = false;
    
    private ViewPager2 viewPagerImages;
    private ImageView ivPlaceholder;

    private ArrayList<Uri> imageUris = new ArrayList<>();
    private ImageAdapter imageAdapter;

    public static AddingBhFragment newInstance(int userId) {
        AddingBhFragment fragment = new AddingBhFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) userId = getArguments().getInt(ARG_USER_ID, -1);

        // NEW: Restore saved images if coming back after rotation
        if (savedInstanceState != null) {
            ArrayList<Uri> savedUris = savedInstanceState.getParcelableArrayList(KEY_SAVED_IMAGES);
            if (savedUris != null) {
                imageUris.clear();
                imageUris.addAll(savedUris);
            }
        }
        
        // Restore saved data from static variables
        restoreSavedData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adding_bh, container, false);

        etBhName = view.findViewById(R.id.etTitle);
        // Address Fields
        spinnerProvince = view.findViewById(R.id.spinnerProvince);
        spinnerMunicipality = view.findViewById(R.id.spinnerMunicipality);
        spinnerBarangay = view.findViewById(R.id.spinnerBarangay);
        etDetailedAddress = view.findViewById(R.id.etDetailedAddress);
        webViewMap = view.findViewById(R.id.webViewMap);
        btnConfirmAddress = view.findViewById(R.id.btnConfirmAddress);
        tvAddressStatus = view.findViewById(R.id.tvAddressStatus);
        
        etBhDescription = view.findViewById(R.id.etDescription);
        etBhRules = view.findViewById(R.id.etRules);
        etBathrooms = view.findViewById(R.id.etBathrooms);
        etArea = view.findViewById(R.id.etArea);
        etBuildYear = view.findViewById(R.id.etBuildYear);
        viewPagerImages = view.findViewById(R.id.viewPagerImages);
        ivPlaceholder = view.findViewById(R.id.ivPlaceholder);

        // Setup adapter
        imageAdapter = new ImageAdapter(getActivity(), imageUris, position -> {
            imageUris.remove(position);
            imageAdapter.notifyDataSetChanged();
            if (imageUris.isEmpty()) {
                ivPlaceholder.setVisibility(View.VISIBLE);
                viewPagerImages.setVisibility(View.GONE);
            }
        });
        viewPagerImages.setAdapter(imageAdapter);

        // Restore UI state (if images already exist)
        if (!imageUris.isEmpty()) {
            ivPlaceholder.setVisibility(View.GONE);
            viewPagerImages.setVisibility(View.VISIBLE);
        }

        // click placeholder to pick images
        ivPlaceholder.setOnClickListener(v -> pickImages());

        // Next button
        view.findViewById(R.id.btnNext).setOnClickListener(v -> goToAddingRooms());

        // Restore saved data to fields
        populateFieldsWithSavedData();
        
        return view;
    }
    
    private void restoreSavedData() {
        // Restore images from static variable
        if (!savedImageUris.isEmpty()) {
            imageUris.clear();
            imageUris.addAll(savedImageUris);
        }
    }
    
    private void populateFieldsWithSavedData() {
        if (etBhName != null) etBhName.setText(savedBhName);
        if (etBhAddress != null) etBhAddress.setText(savedBhAddress);
        if (etBhDescription != null) etBhDescription.setText(savedBhDescription);
        if (etBhRules != null) etBhRules.setText(savedBhRules);
        if (etBathrooms != null) etBathrooms.setText(savedBhBathrooms);
        if (etArea != null) etArea.setText(savedBhArea);
        if (etBuildYear != null) etBuildYear.setText(savedBhBuildYear);
        
        // Update image adapter if there are saved images
        if (!imageUris.isEmpty() && imageAdapter != null) {
            imageAdapter.notifyDataSetChanged();
            if (ivPlaceholder != null) {
                ivPlaceholder.setVisibility(View.GONE);
            }
        }
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, 1001);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            try {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        requireActivity().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        
                        // Verify image before adding
                        verifyAndAddImage(uri);
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    requireActivity().getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    
                    // Verify image before adding
                    verifyAndAddImage(uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Failed to add images", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void verifyAndAddImage(Uri imageUri) {
        // Show loading message
        Toast.makeText(getActivity(), "Verifying image...", Toast.LENGTH_SHORT).show();
        
        // Use smart verification (API if available, local if not)
        ImageVerification.verifyImage(getActivity(), imageUri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    // Add image to list
                    if (!imageUris.contains(imageUri)) {
                        imageUris.add(imageUri);
                        imageAdapter.notifyDataSetChanged();
                        
                        if (!imageUris.isEmpty()) {
                            ivPlaceholder.setVisibility(View.GONE);
                            viewPagerImages.setVisibility(View.VISIBLE);
                        }
                        
                        Toast.makeText(getActivity(), "✅ Image approved and added", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Show rejection reason
                    Toast.makeText(getActivity(), "❌ Image rejected: " + reason, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onVerificationError(String error) {
                Toast.makeText(getActivity(), "Image verification failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToAddingRooms() {
        // Validate all fields
        String validationError = validateAllFields();
        if (validationError != null) {
            showValidationDialog(validationError);
            return;
        }

        // Get validated data
        String name = etBhName.getText().toString().trim();
        String address = etBhAddress.getText().toString().trim();
        String bathrooms = etBathrooms.getText().toString().trim();

        // Save current data to static variables for persistence
        saveCurrentData();

        AddingRoomsFragment fragment = new AddingRoomsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("user_id", userId);
        bundle.putString("bh_name", name);
        bundle.putString("bh_address", address);
        bundle.putString("bh_description", etBhDescription.getText().toString().trim());
        bundle.putString("bh_rules", etBhRules.getText().toString().trim());
        bundle.putString("bh_bathrooms", bathrooms);
        bundle.putString("bh_area", etArea.getText().toString().trim());
        bundle.putString("bh_build_year", etBuildYear.getText().toString().trim());
        bundle.putParcelableArrayList("bh_images", imageUris);
        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private String validateAllFields() {
        // Validate required fields
        String name = etBhName.getText().toString().trim();
        String bathrooms = etBathrooms.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String buildYear = etBuildYear.getText().toString().trim();
        String description = etBhDescription.getText().toString().trim();
        String rules = etBhRules.getText().toString().trim();
        
        // Address validation
        String province = spinnerProvince.getSelectedItem() != null ? spinnerProvince.getSelectedItem().toString() : "";
        String municipality = spinnerMunicipality.getSelectedItem() != null ? spinnerMunicipality.getSelectedItem().toString() : "";
        String barangay = spinnerBarangay.getSelectedItem() != null ? spinnerBarangay.getSelectedItem().toString() : "";
        String detailed = etDetailedAddress.getText().toString().trim();

        // Check required fields
        if (TextUtils.isEmpty(name)) {
            return "Boarding House Name is required";
        }
        
        if (province.equals("Select Province") || province.isEmpty()) {
            return "Please select a Province";
        }
        if (municipality.equals("Select Municipality") || municipality.isEmpty()) {
            return "Please select a Municipality";
        }
        if (barangay.equals("Select Barangay") || barangay.isEmpty()) {
            return "Please select a Barangay";
        }
        if (TextUtils.isEmpty(detailed)) {
            return "Detailed Address is required";
        }
        
        if (!isAddressConfirmed) {
            return "Please confirm the address by clicking the Confirm Address button";
        }

        if (TextUtils.isEmpty(bathrooms)) {
            return "Number of Bathrooms is required";
        }

        // Validate name (2-50 characters, letters, numbers, spaces, hyphens, and apostrophes)
        if (name.length() < 2 || name.length() > 50) {
            return "Boarding House Name must be 2-50 characters long";
        }
        if (!Pattern.matches("^[a-zA-Z0-9\\s\\-']+$", name)) {
            return "Boarding House Name can only contain letters, numbers, spaces, hyphens, and apostrophes";
        }

        // Validate bathrooms (must be a positive number 1-10)
        try {
            int bathroomCount = Integer.parseInt(bathrooms);
            if (bathroomCount < 1 || bathroomCount > 10) {
                return "Number of Bathrooms must be between 1 and 10";
            }
        } catch (NumberFormatException e) {
            return "Number of Bathrooms must be a valid number";
        }

        // Validate area if provided (must be positive number)
        if (!TextUtils.isEmpty(area)) {
            try {
                double areaValue = Double.parseDouble(area);
                if (areaValue <= 0) {
                    return "Area must be a positive number";
                }
                if (areaValue > 10000) {
                    return "Area cannot exceed 10,000 square meters";
                }
            } catch (NumberFormatException e) {
                return "Area must be a valid number";
            }
        }

        // Validate build year if provided (1900 to current year)
        if (!TextUtils.isEmpty(buildYear)) {
            try {
                int year = Integer.parseInt(buildYear);
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                if (year < 1900 || year > currentYear) {
                    return "Build Year must be between 1900 and " + currentYear;
                }
            } catch (NumberFormatException e) {
                return "Build Year must be a valid year";
            }
        }

        // Validate description length if provided (max 500 characters)
        if (!TextUtils.isEmpty(description) && description.length() > 500) {
            return "Description cannot exceed 500 characters";
        }

        // Validate rules length if provided (max 300 characters)
        if (!TextUtils.isEmpty(rules) && rules.length() > 300) {
            return "Rules cannot exceed 300 characters";
        }

        // Validate images (at least 1 image required)
        if (imageUris.isEmpty()) {
            return "At least 1 image is required";
        }

        return null; // All validations passed
    }

    private void showValidationDialog(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Validation Error")
                .setMessage(errorMessage)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    // Focus on the problematic field if possible
                    focusOnProblematicField(errorMessage);
                })
                .setCancelable(false);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void focusOnProblematicField(String errorMessage) {
        // Focus on the field that has the error
        if (errorMessage.contains("Boarding House Name")) {
            etBhName.requestFocus();
        } else if (errorMessage.contains("Select") || errorMessage.contains("Province") || errorMessage.contains("Municipality") || errorMessage.contains("Barangay")) {
            // Can't really focus spinner, maybe scroll to top
            spinnerProvince.requestFocus();
        } else if (errorMessage.contains("Detailed Address")) {
            etDetailedAddress.requestFocus();
        } else if (errorMessage.contains("Confirm Address")) {
            btnConfirmAddress.requestFocus();
        } else if (errorMessage.contains("Bathrooms")) {
            etBathrooms.requestFocus();
        } else if (errorMessage.contains("Area")) {
            etArea.requestFocus();
        } else if (errorMessage.contains("Build Year")) {
            etBuildYear.requestFocus();
        } else if (errorMessage.contains("Description")) {
            etBhDescription.requestFocus();
        } else if (errorMessage.contains("Rules")) {
            etBhRules.requestFocus();
        } else if (errorMessage.contains("image")) {
            // Focus on image placeholder
            ivPlaceholder.requestFocus();
        }
    }
    
    private void saveCurrentData() {
        // Save current form data to static variables
        savedBhName = etBhName.getText().toString().trim();
        // savedBhAddress removed
        
        savedProvince = spinnerProvince.getSelectedItem() != null ? spinnerProvince.getSelectedItem().toString() : "";
        savedMunicipality = spinnerMunicipality.getSelectedItem() != null ? spinnerMunicipality.getSelectedItem().toString() : "";
        savedBarangay = spinnerBarangay.getSelectedItem() != null ? spinnerBarangay.getSelectedItem().toString() : "";
        savedDetailedAddress = etDetailedAddress.getText().toString().trim();
        savedAddressConfirmed = isAddressConfirmed;
        
        savedBhDescription = etBhDescription.getText().toString().trim();
        savedBhRules = etBhRules.getText().toString().trim();
        savedBhBathrooms = etBathrooms.getText().toString().trim();
        savedBhArea = etArea.getText().toString().trim();
        savedBhBuildYear = etBuildYear.getText().toString().trim();
        
        // Save images
        savedImageUris.clear();
        savedImageUris.addAll(imageUris);
    }
    
    // Method to clear saved data (call this after successful save)
    public static void clearSavedData() {
        savedBhName = "";
        // savedBhAddress removed
        savedProvince = "";
        savedMunicipality = "";
        savedBarangay = "";
        savedDetailedAddress = "";
        savedAddressConfirmed = false;
        
        savedBhDescription = "";
        savedBhRules = "";
        savedBhBathrooms = "";
        savedBhArea = "";
        savedBhBuildYear = "";
        savedImageUris.clear();
    }

    // NEW: Save images when fragment is about to be destroyed
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_SAVED_IMAGES, imageUris);
    }
    
    // ==========================================
    // Address Picker Logic
    // ==========================================
    
    private void initializeAddressPicker() {
        // Initialize spinners with default items
        List<String> defaultProvince = new ArrayList<>();
        defaultProvince.add("Select Province");
        ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, defaultProvince);
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(provinceAdapter);

        List<String> defaultMunicipality = new ArrayList<>();
        defaultMunicipality.add("Select Municipality");
        ArrayAdapter<String> municipalityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, defaultMunicipality);
        municipalityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMunicipality.setAdapter(municipalityAdapter);

        List<String> defaultBarangay = new ArrayList<>();
        defaultBarangay.add("Select Barangay");
        ArrayAdapter<String> barangayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, defaultBarangay);
        barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangay.setAdapter(barangayAdapter);

        // Load Provinces
        loadProvinces();

        // Province Selection Listener
        spinnerProvince.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (!selected.equals("Select Province") && !selected.equals(selectedProvince)) {
                    selectedProvince = selected;
                    loadMunicipalities(selected);
                    // Reset lower levels
                    spinnerMunicipality.setSelection(0);
                    spinnerBarangay.setSelection(0);
                    selectedMunicipality = "";
                    selectedBarangay = "";
                    invalidateAddressConfirmation();
                } else if (savedProvince != null && !savedProvince.isEmpty() && selected.equals("Select Province")) {
                   // Initial load with saved state
                   // We don't trigger reload here, we wait for province list to populate and set selection
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Municipality Selection Listener
        spinnerMunicipality.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (!selected.equals("Select Municipality") && !selected.equals(selectedMunicipality)) {
                    selectedMunicipality = selected;
                    loadBarangays(selected);
                    // Reset lower levels
                    spinnerBarangay.setSelection(0);
                    selectedBarangay = "";
                    invalidateAddressConfirmation();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Barangay Selection Listener
        spinnerBarangay.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (!selected.equals("Select Barangay")) {
                    selectedBarangay = selected;
                    invalidateAddressConfirmation();
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Detailed Address Text Watcher
        etDetailedAddress.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                invalidateAddressConfirmation();
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    private void invalidateAddressConfirmation() {
        if (isAddressConfirmed) {
            isAddressConfirmed = false;
            tvAddressStatus.setText("* Address changed. Please confirm again.");
            tvAddressStatus.setTextColor(Color.parseColor("#DC3545")); // Red
            btnConfirmAddress.setText("Confirm Address on Map");
            btnConfirmAddress.setEnabled(true);
        }
    }
    
    private void loadProvinces() {
        String url = "https://boardease.calapebohol.com/philippine_address_api.php?action=provinces";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            // Check if response is valid JSON array or object wrapping array
                            // The API usually returns [ "Province A", "Province B" ]
                            
                            // Handle HTML error response
                            if (response.trim().startsWith("<!DOCTYPE html>")) {
                                Log.e("API_ERROR", "Received HTML response instead of JSON");
                                return;
                            }
                            
                            JSONArray jsonArray = new JSONArray(response);
                            List<String> provinces = new ArrayList<>();
                            provinces.add("Select Province");
                            
                            for (int i = 0; i < jsonArray.length(); i++) {
                                provinces.add(jsonArray.getString(i));
                            }
                            
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, provinces);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerProvince.setAdapter(adapter);
                            
                            // Restore saved province if checking
                            if (!savedProvince.isEmpty()) {
                                int position = adapter.getPosition(savedProvince);
                                if (position >= 0) {
                                    spinnerProvince.setSelection(position);
                                }
                            }
                            
                        } catch (JSONException e) {
                            Log.e("API_ERROR", "JSON Parse error: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("API_ERROR", "Volley error: " + error.getMessage());
                        Toast.makeText(getContext(), "Failed to load provinces", Toast.LENGTH_SHORT).show();
                    }
                });
        
        Volley.newRequestQueue(requireContext()).add(stringRequest);
    }
    
    private void loadMunicipalities(String province) {
        if (province.isEmpty() || province.equals("Select Province")) return;
        
        String url = "";
        try {
            url = "https://boardease.calapebohol.com/philippine_address_api.php?action=municipalities&province_name=" + URLEncoder.encode(province, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                             if (response.trim().startsWith("<!DOCTYPE html>")) {
                                return;
                            }
                            JSONArray jsonArray = new JSONArray(response);
                            List<String> municipalities = new ArrayList<>();
                            municipalities.add("Select Municipality");
                            
                            for (int i = 0; i < jsonArray.length(); i++) {
                                municipalities.add(jsonArray.getString(i));
                            }
                            
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, municipalities);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerMunicipality.setAdapter(adapter);
                            
                            // Restore saved municipality
                            if (!savedMunicipality.isEmpty() && province.equals(savedProvince)) {
                                int position = adapter.getPosition(savedMunicipality);
                                if (position >= 0) {
                                    spinnerMunicipality.setSelection(position);
                                }
                            }
                            
                        } catch (JSONException e) {
                            Log.e("API_ERROR", "JSON Parse error: " + e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("API_ERROR", "Error loading municipalities");
            }
        });
        
        Volley.newRequestQueue(requireContext()).add(stringRequest);
    }
    
    private void loadBarangays(String municipality) {
        if (municipality.isEmpty() || municipality.equals("Select Municipality")) return;
        
        String url = "";
        try {
            url = "https://boardease.calapebohol.com/philippine_address_api.php?action=barangays&municipality_name=" + URLEncoder.encode(municipality, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                             if (response.trim().startsWith("<!DOCTYPE html>")) {
                                return;
                            }
                            JSONArray jsonArray = new JSONArray(response);
                            List<String> barangays = new ArrayList<>();
                            barangays.add("Select Barangay");
                            
                            for (int i = 0; i < jsonArray.length(); i++) {
                                barangays.add(jsonArray.getString(i));
                            }
                            
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, barangays);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerBarangay.setAdapter(adapter);
                            
                            // Restore saved barangay
                            if (!savedBarangay.isEmpty() && municipality.equals(savedMunicipality)) {
                                int position = adapter.getPosition(savedBarangay);
                                if (position >= 0) {
                                    spinnerBarangay.setSelection(position);
                                }
                            }
                            
                        } catch (JSONException e) {
                            Log.e("API_ERROR", "JSON Parse error: " + e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("API_ERROR", "Error loading barangays");
            }
        });
        
        Volley.newRequestQueue(requireContext()).add(stringRequest);
    }
    
    // ==========================================
    // Mapbox Logic
    // ==========================================
    
    private void setupMapWebView() {
         webViewMap.getSettings().setJavaScriptEnabled(true);
         webViewMap.getSettings().setBuiltInZoomControls(false);
         webViewMap.getSettings().setDomStorageEnabled(true);
         
         // Set WebViewClient
         webViewMap.setWebViewClient(new WebViewClient() {
             @Override
             public void onPageFinished(WebView view, String url) {
                 super.onPageFinished(view, url);
             }
         });
         
         // Initial Empty Map or Instructions
         String html = "<html><body style='display:flex;justify-content:center;align-items:center;height:100%;font-family:sans-serif;color:#666;text-align:center;'>Map will appear here after confirming address.</body></html>";
         webViewMap.loadData(html, "text/html", "UTF-8");
    }
    
    private void confirmAddress() {
        String province = spinnerProvince.getSelectedItem() != null ? spinnerProvince.getSelectedItem().toString() : "";
        String municipality = spinnerMunicipality.getSelectedItem() != null ? spinnerMunicipality.getSelectedItem().toString() : "";
        String barangay = spinnerBarangay.getSelectedItem() != null ? spinnerBarangay.getSelectedItem().toString() : "";
        String detailed = etDetailedAddress.getText().toString().trim();
        
        if (province.equals("Select Province") || province.isEmpty()) {
            showValidationDialog("Please select a Province");
            return;
        }
        if (municipality.equals("Select Municipality") || municipality.isEmpty()) {
            showValidationDialog("Please select a Municipality");
            return;
        }
        if (barangay.equals("Select Barangay") || barangay.isEmpty()) {
            showValidationDialog("Please select a Barangay");
            return;
        }
        if (detailed.isEmpty()) {
            showValidationDialog("Please enter detailed address");
            return;
        }
        
        String fullAddress = detailed + ", " + barangay + ", " + municipality + ", " + province;
        loadMap(fullAddress);
    }
    
    private void loadMap(String address) {
        tvAddressStatus.setText("Loading map...");
        tvAddressStatus.setTextColor(Color.GRAY);
        btnConfirmAddress.setEnabled(false);
        
        String htmlContent = generateMapboxMapHtml(address);
        webViewMap.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        
        isAddressConfirmed = true;
        tvAddressStatus.setText("Address Confirmed ✓");
        tvAddressStatus.setTextColor(Color.parseColor("#198754")); // Green
        btnConfirmAddress.setText("Address Confirmed");
    }
    
    private String generateMapboxMapHtml(String address) {
        String escapedAddress = address.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
        
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">" +
                   "<script src=\"https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js\"></script>" +
                   "<link href=\"https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css\" rel=\"stylesheet\" />" +
               "<style>" +
               "body { margin: 0; padding: 0; }" +
               "#map { position: absolute; top: 0; bottom: 0; width: 100%; }" +
               "</style>" +
               "</head>" +
               "<body>" +
                   "<div id=\"map\"></div>" +
                   "<script>" +
                   "mapboxgl.accessToken = '" + MAPBOX_ACCESS_TOKEN + "'; " +
                   "var map = new mapboxgl.Map({ " +
                   "  container: 'map', " +
                   "  style: 'mapbox://styles/mapbox/streets-v12', " +
                   "  center: [120.9842, 14.5995], " + // Default
                   "  zoom: 13 " +
                   "}); " +
                   "var address = '" + escapedAddress + "'; " +
                   "map.on('load', function() { " +
                   "  fetch('https://api.mapbox.com/geocoding/v5/mapbox.places/' + encodeURIComponent(address) + '.json?access_token=' + mapboxgl.accessToken + '&limit=1') " +
                   "    .then(response => response.json()) " +
                   "    .then(data => { " +
                   "      if (data && data.features && data.features.length > 0) { " +
                   "        var coordinates = data.features[0].center; " +
                   "        map.flyTo({ center: coordinates, zoom: 15 }); " +
                   "        new mapboxgl.Marker({ color: '#FF6B6B' }) " +
                   "          .setLngLat(coordinates) " +
                   "          .addTo(map); " +
                   "      } " +
                   "    }) " +
                   "    .catch(error => console.error(error)); " +
                   "}); " +
                   "</script>" +
               "</body>" +
               "</html>";
    }
}
