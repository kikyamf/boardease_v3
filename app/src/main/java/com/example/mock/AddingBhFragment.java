package com.example.mock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.view.MotionEvent;
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
import java.util.List;
import java.util.regex.Pattern;
import java.util.Calendar;
import android.util.Log;
import com.android.volley.toolbox.StringRequest;
import android.graphics.Color;

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
    private static boolean savedAddressConfirmed = false;
    
    private static ArrayList<Uri> savedImageUris = new ArrayList<>();

    private int userId = -1;

    private EditText etBhName, etBhDescription, etBhRules, etBathrooms, etArea, etBuildYear;
    // New Address Fields
    private Spinner spinnerProvince, spinnerMunicipality, spinnerBarangay;
    private WebView webViewMap;
    private android.widget.LinearLayout llMapLoading;
    private TextView tvAddressStatus; // Keeping for error messages if needed, or remove if unused in updated XML
    
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

        Log.d("AddBhDebug", "onCreateView called");
        
        // Initialize UI components
        etBhName = view.findViewById(R.id.etTitle);
        // Address Fields
        spinnerProvince = view.findViewById(R.id.spinnerProvince);
        spinnerMunicipality = view.findViewById(R.id.spinnerMunicipality);
        spinnerBarangay = view.findViewById(R.id.spinnerBarangay);
        webViewMap = view.findViewById(R.id.webViewMap);
        llMapLoading = view.findViewById(R.id.llMapLoading);
        
        // Port premium WebView setup from Details Activity
        setupMapWebView();
        
        etBhDescription = view.findViewById(R.id.etDescription);
        etBhRules = view.findViewById(R.id.etRules);
        etBathrooms = view.findViewById(R.id.etBathrooms);
        etArea = view.findViewById(R.id.etArea);
        etBuildYear = view.findViewById(R.id.etBuildYear);
        viewPagerImages = view.findViewById(R.id.viewPagerImages);
        ivPlaceholder = view.findViewById(R.id.ivPlaceholder);

        // Initialize Address Picker
        initializeAddressPicker();

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
        
        // Populate Address components
        if (!savedProvince.isEmpty()) {
            // Spinners need to be set after items are loaded. 
            // The loading methods (loadProvinces, etc.) already have logic to set selection if savedX matches.
        }
        
        if (etBhDescription != null) etBhDescription.setText(savedBhDescription);
        if (etBhRules != null) etBhRules.setText(savedBhRules);
        if (etBathrooms != null) etBathrooms.setText(savedBhBathrooms);
        if (etArea != null) etArea.setText(savedBhArea);
        if (etBuildYear != null) etBuildYear.setText(savedBhBuildYear);
        
        //Address confirmation status logic restoration already in onCreateView
        
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
        

        // Construct full address
        String province = spinnerProvince.getSelectedItem() != null ? spinnerProvince.getSelectedItem().toString() : "";
        String municipality = spinnerMunicipality.getSelectedItem() != null ? spinnerMunicipality.getSelectedItem().toString() : "";
        String barangay = spinnerBarangay.getSelectedItem() != null ? spinnerBarangay.getSelectedItem().toString() : "";
        
        String fullAddress = barangay + ", " + municipality + ", " + province;
        
        String bathrooms = etBathrooms.getText().toString().trim();

        // Save current data to static variables for persistence
        saveCurrentData();

        AddingRoomsFragment fragment = new AddingRoomsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("user_id", userId);
        bundle.putString("bh_name", name);
        bundle.putString("bh_address", fullAddress);
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
        
        if (!isAddressConfirmed) {
            // Check if loading? Or just allow it if inputs are valid and let logic handle it.
            // For now, require it to be true (set by startMapLoadingAnimation).
            // If it's still false, it means map hasn't started loading or failed.
            // Since we trigger it automatically, maybe we just return null but map might not be ready?
            // User said "confirm fields... then map shows".
            // Let's assume validation passes if map is loading.
            // But strict validation requires isAddressConfirmed.
             return "Please wait for the address map to load.";
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
        // Initialize spinners with default items using custom adapter for visibility
        List<String> defaultProvince = new ArrayList<>();
        defaultProvince.add("Select Province");
        
        ArrayAdapter<String> provinceAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, defaultProvince) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) textView.setTextColor(0xFF212529);
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) textView.setTextColor(0xFF212529);
                view.setBackgroundColor(0xFFFFFFFF);
                return view;
            }
        };
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(provinceAdapter);

        List<String> defaultMunicipality = new ArrayList<>();
        defaultMunicipality.add("Select Municipality");
        ArrayAdapter<String> municipalityAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, defaultMunicipality) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) textView.setTextColor(0xFF212529);
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) textView.setTextColor(0xFF212529);
                view.setBackgroundColor(0xFFFFFFFF);
                return view;
            }
        };
        municipalityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMunicipality.setAdapter(municipalityAdapter);

        List<String> defaultBarangay = new ArrayList<>();
        defaultBarangay.add("Select Barangay");
        ArrayAdapter<String> barangayAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, defaultBarangay) {
             @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) textView.setTextColor(0xFF212529);
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) textView.setTextColor(0xFF212529);
                view.setBackgroundColor(0xFFFFFFFF);
                return view;
            }
        };
        barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangay.setAdapter(barangayAdapter);

        // Validation: Prompt for Property Name when spinners are clicked if name is empty
        View.OnTouchListener propertyNameValidator = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (etBhName != null) {
                    String name = etBhName.getText().toString().trim();
                    if (name.isEmpty()) {
                        etBhName.setError("Please input the property name first");
                        etBhName.requestFocus();
                        // Optional: Show a toast for better visibility
                        Toast.makeText(requireContext(), "Input property name first", Toast.LENGTH_SHORT).show();
                        return true; // Consume click
                    }
                }
            }
            return false;
        };

        spinnerProvince.setOnTouchListener(propertyNameValidator);
        spinnerMunicipality.setOnTouchListener(propertyNameValidator);
        spinnerBarangay.setOnTouchListener(propertyNameValidator);

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
                    // Automatic consolidated trigger
                    checkAndTriggerMapUpdate();
                } else {
                     selectedBarangay = "";
                     isAddressConfirmed = false;
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    private void checkAndTriggerMapUpdate() {
        if (selectedProvince == null || selectedProvince.isEmpty() || selectedProvince.equals("Select Province") ||
            selectedMunicipality == null || selectedMunicipality.isEmpty() || selectedMunicipality.equals("Select Municipality") ||
            selectedBarangay == null || selectedBarangay.isEmpty() || selectedBarangay.equals("Select Barangay")) {
            return;
        }

        startMapLoadingAnimation();
    }
    
    private void startMapLoadingAnimation() {
        // Show map immediately, no artificial delay
        if (llMapLoading != null) llMapLoading.setVisibility(View.GONE);
        if (webViewMap != null) webViewMap.setVisibility(View.VISIBLE);
        
        // Construct address
        String fullAddress = selectedBarangay + ", " + selectedMunicipality + ", " + selectedProvince;
        
        // Load map
        loadMap(fullAddress);
        
        // Update status
        isAddressConfirmed = true;
    }

    private void invalidateAddressConfirmation() {
       isAddressConfirmed = false;
       // hide map or show invalid state if desired, for now just reset flag
    }
    
    private void loadProvinces() {
        String url = "https://boardease.calapebohol.com/philippine_address_api.php?action=provinces";
        
        Log.d("AddressPicker", "Loading provinces from: " + url);
        
        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            response -> {
                try {
                    Log.d("AddressPicker", "Provinces response received: " + response.toString());
                    
                    if (response.getBoolean("success")) {
                        org.json.JSONArray provincesArray = response.getJSONArray("data");
                        Log.d("AddressPicker", "Found " + provincesArray.length() + " provinces");
                        
                        String[] provinceNames = new String[provincesArray.length() + 1];
                        provinceNames[0] = "Select Province";
                        
                        for (int i = 0; i < provincesArray.length(); i++) {
                            org.json.JSONObject province = provincesArray.getJSONObject(i);
                            provinceNames[i + 1] = province.getString("name");
                        }
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            requireContext(), android.R.layout.simple_spinner_item, provinceNames) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                                if (textView != null) {
                                    textView.setTextColor(0xFF212529); // Dark grey for selected item
                                }
                                return view;
                            }
                            
                            @Override
                            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                if (view instanceof TextView) {
                                    TextView textView = (TextView) view;
                                    textView.setTextColor(0xFF212529);
                                    textView.setTextSize(16);
                                    textView.setPadding(16, 16, 16, 16);
                                    textView.setBackgroundColor(0xFFFFFFFF);
                                } else {
                                    TextView textView = view.findViewById(android.R.id.text1);
                                    if (textView != null) {
                                        textView.setTextColor(0xFF212529);
                                        textView.setTextSize(16);
                                        textView.setPadding(16, 16, 16, 16);
                                    }
                                    view.setBackgroundColor(0xFFFFFFFF);
                                }
                                return view;
                            }
                        };
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerProvince.setAdapter(adapter);
                        
                        // Restore saved province if checking
                        if (!savedProvince.isEmpty()) {
                            int position = adapter.getPosition(savedProvince);
                            if (position >= 0) {
                                spinnerProvince.setSelection(position);
                            }
                        }
                        
                        Log.d("AddressPicker", "Provinces loaded successfully");
                    } else {
                        Log.e("AddressPicker", "API returned success=false");
                        loadProvincesFallback();
                    }
                } catch (org.json.JSONException e) {
                    Log.e("AddressPicker", "JSON parsing error: " + e.getMessage());
                    Log.e("AddressPicker", "Response was: " + response.toString());
                    e.printStackTrace();
                    loadProvincesFallback();
                }
            },
            error -> {
                Log.e("AddressPicker", "Network error loading provinces: " + error.getMessage());
                loadProvincesFallback();
            }
        );
        
        com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(requireContext());
        queue.add(request);
    }

    private void loadProvincesFallback() {
        Log.d("AddressPicker", "Using fallback provinces data");
        
        // Fallback provinces data
        String[] fallbackProvinces = {
            "Select Province", "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay", "Antique", "Apayao", "Aurora",
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
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_spinner_item, fallbackProvinces);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(adapter);
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

        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
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
                            
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                requireContext(), android.R.layout.simple_spinner_item, municipalityNames) {
                                @Override
                                public View getView(int position, View convertView, ViewGroup parent) {
                                    View view = super.getView(position, convertView, parent);
                                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                                    if (textView != null) {
                                        textView.setTextColor(0xFF212529);
                                    }
                                    return view;
                                }
                                
                                @Override
                                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                  View view = super.getDropDownView(position, convertView, parent);
                                    if (view instanceof TextView) {
                                        TextView textView = (TextView) view;
                                        textView.setTextColor(0xFF212529);
                                        textView.setTextSize(16);
                                        textView.setPadding(16, 16, 16, 16);
                                        textView.setBackgroundColor(0xFFFFFFFF);
                                    } else {
                                        TextView textView = view.findViewById(android.R.id.text1);
                                        if (textView != null) {
                                            textView.setTextColor(0xFF212529);
                                            textView.setTextSize(16);
                                            textView.setPadding(16, 16, 16, 16);
                                        }
                                        view.setBackgroundColor(0xFFFFFFFF);
                                    }
                                    return view;
                                }
                            };
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerMunicipality.setAdapter(adapter);
                            
                            // Restore saved municipality
                            if (!savedMunicipality.isEmpty() && province.equals(savedProvince)) {
                                int position = adapter.getPosition(savedMunicipality);
                                if (position >= 0) {
                                    spinnerMunicipality.setSelection(position);
                                }
                            }
                        } else {
                            Log.w("AddressPicker", "No municipalities found for province: " + province);
                        }
                    } else {
                        Log.e("AddressPicker", "API returned success=false for municipalities");
                    }
                } catch (JSONException e) {
                    Log.e("AddressPicker", "JSON Parse error: " + e.getMessage());
                }
            }, 
            error -> {
                Log.e("AddressPicker", "Error loading municipalities: " + error.getMessage());
            }
        );
        
        com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(requireContext());
        queue.add(request);
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
        
        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.getBoolean("success")) {
                        org.json.JSONArray barangaysArray = response.getJSONArray("data");
                        
                        List<String> barangays = new ArrayList<>();
                        barangays.add("Select Barangay");
                        
                        for (int i = 0; i < barangaysArray.length(); i++) {
                            org.json.JSONObject barangay = barangaysArray.getJSONObject(i);
                            barangays.add(barangay.getString("name"));
                        }
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            requireContext(), android.R.layout.simple_spinner_item, barangays) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                                if (textView != null) {
                                    textView.setTextColor(0xFF212529);
                                }
                                return view;
                            }
                            
                            @Override
                            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                 if (view instanceof TextView) {
                                    TextView textView = (TextView) view;
                                    textView.setTextColor(0xFF212529);
                                    textView.setTextSize(16);
                                    textView.setPadding(16, 16, 16, 16);
                                    textView.setBackgroundColor(0xFFFFFFFF);
                                } else {
                                    TextView textView = view.findViewById(android.R.id.text1);
                                    if (textView != null) {
                                        textView.setTextColor(0xFF212529);
                                        textView.setTextSize(16);
                                        textView.setPadding(16, 16, 16, 16);
                                    }
                                    view.setBackgroundColor(0xFFFFFFFF);
                                }
                                return view;
                            }
                        };
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerBarangay.setAdapter(adapter);
                        
                        // Restore saved barangay
                        if (!savedBarangay.isEmpty() && municipality.equals(savedMunicipality)) {
                            int position = adapter.getPosition(savedBarangay);
                            if (position >= 0) {
                                spinnerBarangay.setSelection(position);
                            }
                        }
                    } else {
                         Log.e("AddressPicker", "API returned success=false for barangays");
                    }
                    
                } catch (JSONException e) {
                    Log.e("AddressPicker", "JSON Parse error: " + e.getMessage());
                }
            },
            error -> {
                 Log.e("AddressPicker", "Error loading barangays: " + error.getMessage());
            }
        );
        
        com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(requireContext());
        queue.add(request);
    }
    
    // ==========================================
    // Mapbox Logic
    // ==========================================
    
    private void setupMapWebView() {
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setBuiltInZoomControls(false);
        webViewMap.getSettings().setDisplayZoomControls(false);
        webViewMap.getSettings().setSupportZoom(false);
        webViewMap.getSettings().setUseWideViewPort(true);
        webViewMap.getSettings().setLoadWithOverviewMode(true);
        webViewMap.getSettings().setDomStorageEnabled(true);
        webViewMap.setBackgroundColor(0xFFF5F5F5);

        // Ensure WebView fits exactly in its container
        webViewMap.setPadding(0, 0, 0, 0);
        webViewMap.setClipToOutline(true);

        // Set outline for rounded corners to match CardView
        float cornerRadius = getResources().getDisplayMetrics().density * 12; // 12dp radius
        webViewMap.post(() -> {
            webViewMap.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
                }
            });
            webViewMap.setClipToOutline(true);
        });

        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("MapDebug", "Map page finished loading");
                hideMapLoader();
            }
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("MapDebug", "Error loading map: " + description);
                hideMapLoader();
            }
        });

        // Long-press or double-tap detection to open full-screen map
        webViewMap.setOnTouchListener(new View.OnTouchListener() {
            private long lastTapTime = 0;
            private static final long DOUBLE_TAP_DELAY = 300;
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTapTime < DOUBLE_TAP_DELAY) {
                        Log.d("MapDebug", "Double-tap detected on map");
                        openFullScreenMap();
                        lastTapTime = 0;
                        return true;
                    }
                    lastTapTime = currentTime;
                }
                return false;
            }
        });
        
        // Wrap WebView in FrameLayout and add full-screen button (programmatic overlay)
        webViewMap.post(() -> {
            android.view.ViewParent parent = webViewMap.getParent();
            if (parent instanceof android.view.ViewGroup) {
                android.view.ViewGroup parentGroup = (android.view.ViewGroup) parent;
                int index = parentGroup.indexOfChild(webViewMap);
                
                android.widget.FrameLayout mapContainer = new android.widget.FrameLayout(requireContext());
                android.view.ViewGroup.LayoutParams originalParams = webViewMap.getLayoutParams();
                mapContainer.setLayoutParams(originalParams);
                
                parentGroup.removeView(webViewMap);
                android.widget.FrameLayout.LayoutParams webViewParams = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                );
                webViewMap.setLayoutParams(webViewParams);
                mapContainer.addView(webViewMap);
                
                // Full Screen Button
                android.app.Activity activity = getActivity();
                if (activity == null) return;
                
                android.widget.ImageButton fullScreenIconButton = new android.widget.ImageButton(activity);
                fullScreenIconButton.setImageResource(R.drawable.fullscreen);
                int buttonSize = (int)(getResources().getDisplayMetrics().density * 34); // Slightly larger
                android.widget.FrameLayout.LayoutParams fullScreenButtonParams = new android.widget.FrameLayout.LayoutParams(buttonSize, buttonSize);
                fullScreenButtonParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                fullScreenButtonParams.setMargins(12, 12, 12, 12); // Safer margins
                fullScreenIconButton.setLayoutParams(fullScreenButtonParams);
                
                android.graphics.drawable.GradientDrawable roundedBackground = new android.graphics.drawable.GradientDrawable();
                roundedBackground.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                roundedBackground.setCornerRadius(getResources().getDisplayMetrics().density * 8);
                roundedBackground.setColor(0xCC000000);
                fullScreenIconButton.setBackground(roundedBackground);
                fullScreenIconButton.setColorFilter(0xFFFFFFFF);
                fullScreenIconButton.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                fullScreenIconButton.setPadding(8, 8, 8, 8);
                fullScreenIconButton.setElevation(15.0f); // Ensure it's on top and clickable
                
                fullScreenIconButton.setOnClickListener(v -> {
                    Log.d("MapDebug", "Full screen button clicked programmatically");
                    openFullScreenMap();
                });
                
                mapContainer.addView(fullScreenIconButton);
                parentGroup.addView(mapContainer, index);
            }
        });

        String html = "<html><body style='display:flex;justify-content:center;align-items:center;height:100%;font-family:sans-serif;color:#666;text-align:center;'>Map will appear here after selecting address.</body></html>";
        webViewMap.loadData(html, "text/html", "UTF-8");
    }

    private void openFullScreenMap() {
        if (selectedBarangay == null || selectedBarangay.isEmpty()) return;
        
        String fullAddress = selectedBarangay + ", " + selectedMunicipality + ", " + selectedProvince;
        String bhName = "Boarding House";
        if (etBhName != null && etBhName.getText() != null) {
            String inputName = etBhName.getText().toString().trim();
            if (!inputName.isEmpty()) bhName = inputName;
        }
        
        try {
            Log.d("MapDebug", "Opening robust full screen map for: " + fullAddress);
            
            // Use Activity context for dialog to ensure window attributes work correctly
            android.app.Activity activity = getActivity();
            if (activity == null) return;
            
            android.app.Dialog fullScreenDialog = new android.app.Dialog(activity);
            fullScreenDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            
            android.view.Window window = fullScreenDialog.getWindow();
            if (window != null) {
                // Ported from Details Activity: robust full screen setup
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                );
                
                // Set immersive fullscreen mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                    window.getDecorView().setSystemUiVisibility(flags);
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.setStatusBarColor(0xFF000000);
                    window.setNavigationBarColor(0xFF000000);
                }
            }
            
            // Full Screen WebView
            WebView fullScreenWebView = new WebView(activity);
            fullScreenWebView.getSettings().setJavaScriptEnabled(true);
            fullScreenWebView.getSettings().setBuiltInZoomControls(true);
            fullScreenWebView.getSettings().setDisplayZoomControls(true);
            fullScreenWebView.getSettings().setDomStorageEnabled(true);
            fullScreenWebView.setWebViewClient(new WebViewClient());
            
            String htmlContent = generateMapboxMapHtml(fullAddress, bhName);
            fullScreenWebView.loadDataWithBaseURL("https://boardease.calapebohol.com", htmlContent, "text/html", "UTF-8", null);
            
            android.widget.FrameLayout container = new android.widget.FrameLayout(activity);
            android.view.ViewGroup.LayoutParams containerParams = new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            container.setLayoutParams(containerParams);
            container.setBackgroundColor(0xFF000000);
            
            fullScreenWebView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
            container.addView(fullScreenWebView);
            
            // Close Button
            ImageView closeButton = new ImageView(activity);
            closeButton.setImageResource(R.drawable.ic_close);
            closeButton.setColorFilter(0xFFFFFFFF);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(0xCC000000);
            closeButton.setBackground(bg);
            
            int btnSize = (int)(getResources().getDisplayMetrics().density * 40);
            android.widget.FrameLayout.LayoutParams closeParams = new android.widget.FrameLayout.LayoutParams(btnSize, btnSize);
            closeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            closeParams.setMargins(16, 16, 16, 16);
            closeButton.setLayoutParams(closeParams);
            closeButton.setPadding(10, 10, 10, 10);
            closeButton.setOnClickListener(v -> fullScreenDialog.dismiss());
            
            container.addView(closeButton);
            fullScreenDialog.setContentView(container);
            fullScreenDialog.setCancelable(true);
            fullScreenDialog.show();
            
            // Re-apply layout after show to ensure full coverage
            if (window != null) {
                android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                window.setLayout(metrics.widthPixels, metrics.heightPixels);
            }
        } catch (Exception e) {
            Log.e("MapDebug", "Error opening full screen map: " + e.getMessage(), e);
        }
    }

    private void hideMapLoader() {
        if (llMapLoading != null) {
             llMapLoading.setVisibility(View.GONE);
        }
        if (webViewMap != null) {
            webViewMap.setVisibility(View.VISIBLE);
        }
    }
    
    // confirmAddress removed
    
    private void loadMap(String address) {
        String bhName = etBhName.getText().toString().trim();
        if (bhName.isEmpty()) bhName = "Boarding House";
        
        String htmlContent = generateMapboxMapHtml(address, bhName);
        webViewMap.loadDataWithBaseURL("https://boardease.calapebohol.com", htmlContent, "text/html", "UTF-8", null);
        
        isAddressConfirmed = true;
    }
    
    private String generateMapboxMapHtml(String address, String locationName) {
        String escapedAddress = address.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
        String escapedName = locationName.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
        
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">" +
                   "<script src=\"https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js\"></script>" +
                   "<link href=\"https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css\" rel=\"stylesheet\" />" +
               "<style>" +
               "* { margin: 0; padding: 0; box-sizing: border-box; } " +
               "body { margin: 0; padding: 0; font-family: sans-serif; width: 100%; height: 100%; overflow: hidden; }" +
               "#map { position: absolute; top: 0; bottom: 0; width: 100%; }" +
               ".mapboxgl-popup-content { padding: 12px; font-family: Arial, sans-serif; } " +
               ".mapboxgl-popup-content b { font-size: 14px; color: #333; } " +
               ".mapboxgl-popup-content p { margin: 4px 0 0 0; font-size: 12px; color: #666; } " +
               ".mapboxgl-ctrl-attrib, .mapboxgl-ctrl-logo { display: none !important; } " +
               "</style>" +
               "</head>" +
               "<body>" +
                   "<div id=\"map\"></div>" +
                   "<script>" +
                   "mapboxgl.accessToken = '" + MAPBOX_ACCESS_TOKEN + "'; " +
                   "var map = new mapboxgl.Map({ " +
                   "  container: 'map', " +
                   "  style: 'mapbox://styles/mapbox/streets-v12', " +
                   "  center: [120.9842, 14.5995], " + 
                   "  zoom: 13, " +
                   "  attributionControl: false " +
                   "}); " +
                   "var address = '" + escapedAddress + "'; " +
                   "var locationName = '" + escapedName + "'; " +
                   "map.on('load', function() { " +
                   "  fetch('https://api.mapbox.com/geocoding/v5/mapbox.places/' + encodeURIComponent(address) + '.json?access_token=' + mapboxgl.accessToken + '&limit=1') " +
                   "    .then(response => response.json()) " +
                   "    .then(data => { " +
                   "      if (data && data.features && data.features.length > 0) { " +
                   "        var coordinates = data.features[0].center; " +
                   "        map.flyTo({ center: coordinates, zoom: 15, duration: 1000 }); " +
                   "        var marker = new mapboxgl.Marker({ color: '#FF6B6B' }) " +
                   "          .setLngLat(coordinates) " +
                   "          .addTo(map); " +
                   "        var popup = new mapboxgl.Popup({ offset: 25 }) " +
                   "          .setHTML('<b>' + locationName + '</b><p>' + address + '</p>'); " +
                   "        marker.setPopup(popup); " +
                   "        popup.addTo(map); " +
                   "        console.log('MAP_LOADED');" + 
                   "      } " +
                   "    }) " +
                   "    .catch(error => console.error('JS Error:', error)); " +
                   "}); " +
                   "</script>" +
               "</body>" +
               "</html>";
    }
}
