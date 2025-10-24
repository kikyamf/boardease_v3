package com.example.mock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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

import java.util.ArrayList;

public class AddingBhFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String KEY_SAVED_IMAGES = "saved_bh_images"; // NEW
    
    // Static variables to preserve data when navigating back
    private static String savedBhName = "";
    private static String savedBhAddress = "";
    private static String savedBhDescription = "";
    private static String savedBhRules = "";
    private static String savedBhBathrooms = "";
    private static String savedBhArea = "";
    private static String savedBhBuildYear = "";
    private static ArrayList<Uri> savedImageUris = new ArrayList<>();

    private int userId = -1;

    private EditText etBhName, etBhAddress, etBhDescription, etBhRules, etBathrooms, etArea, etBuildYear;
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
        etBhAddress = view.findViewById(R.id.etAddress);
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
                        if (!imageUris.contains(uri)) imageUris.add(uri);
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    requireActivity().getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    if (!imageUris.contains(uri)) imageUris.add(uri);
                }
                imageAdapter.notifyDataSetChanged();

                if (!imageUris.isEmpty()) {
                    ivPlaceholder.setVisibility(View.GONE);
                    viewPagerImages.setVisibility(View.VISIBLE);
                }

                Toast.makeText(getActivity(), "Image(s) added", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Failed to add images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void goToAddingRooms() {
        // Validate all fields before proceeding
        if (!validateAllFields()) {
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
    
    /**
     * Comprehensive validation for all fields
     * @return true if all validations pass, false otherwise
     */
    private boolean validateAllFields() {
        boolean isValid = true;
        
        // Validate Boarding House Name (Required)
        if (!validateBoardingHouseName()) {
            isValid = false;
        }
        
        // Validate Address (Required)
        if (!validateAddress()) {
            isValid = false;
        }
        
        // Validate Bathrooms (Required)
        if (!validateBathrooms()) {
            isValid = false;
        }
        
        // Validate Area (Optional)
        if (!validateArea()) {
            isValid = false;
        }
        
        // Validate Build Year (Optional)
        if (!validateBuildYear()) {
            isValid = false;
        }
        
        // Validate Images (Required)
        if (!validateImages()) {
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Validate Boarding House Name
     * Requirements: Required, minimum 3 characters, maximum 100 characters, no special characters except spaces and hyphens
     */
    private boolean validateBoardingHouseName() {
        String name = etBhName.getText().toString().trim();
        
        if (TextUtils.isEmpty(name)) {
            etBhName.setError("Boarding house name is required");
            etBhName.requestFocus();
            Toast.makeText(getActivity(), "Please enter a boarding house name", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (name.length() < 3) {
            etBhName.setError("Name must be at least 3 characters long");
            etBhName.requestFocus();
            Toast.makeText(getActivity(), "Boarding house name must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (name.length() > 100) {
            etBhName.setError("Name must not exceed 100 characters");
            etBhName.requestFocus();
            Toast.makeText(getActivity(), "Boarding house name is too long (max 100 characters)", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Check for valid characters (letters, numbers, spaces, hyphens, apostrophes)
        if (!name.matches("^[a-zA-Z0-9\\s\\-']+$")) {
            etBhName.setError("Name contains invalid characters");
            etBhName.requestFocus();
            Toast.makeText(getActivity(), "Name can only contain letters, numbers, spaces, hyphens, and apostrophes", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        etBhName.setError(null);
        return true;
    }
    
    /**
     * Validate Address
     * Requirements: Required, minimum 10 characters, maximum 200 characters
     */
    private boolean validateAddress() {
        String address = etBhAddress.getText().toString().trim();
        
        if (TextUtils.isEmpty(address)) {
            etBhAddress.setError("Address is required");
            etBhAddress.requestFocus();
            Toast.makeText(getActivity(), "Please enter the boarding house address", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (address.length() < 10) {
            etBhAddress.setError("Address must be at least 10 characters long");
            etBhAddress.requestFocus();
            Toast.makeText(getActivity(), "Please provide a complete address (at least 10 characters)", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (address.length() > 200) {
            etBhAddress.setError("Address must not exceed 200 characters");
            etBhAddress.requestFocus();
            Toast.makeText(getActivity(), "Address is too long (max 200 characters)", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        etBhAddress.setError(null);
        return true;
    }
    
    /**
     * Validate Bathrooms
     * Requirements: Required, must be a positive number, maximum 10
     */
    private boolean validateBathrooms() {
        String bathroomsStr = etBathrooms.getText().toString().trim();
        
        if (TextUtils.isEmpty(bathroomsStr)) {
            etBathrooms.setError("Number of bathrooms is required");
            etBathrooms.requestFocus();
            Toast.makeText(getActivity(), "Please enter the number of bathrooms", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            int bathrooms = Integer.parseInt(bathroomsStr);
            
            if (bathrooms <= 0) {
                etBathrooms.setError("Number of bathrooms must be greater than 0");
                etBathrooms.requestFocus();
                Toast.makeText(getActivity(), "Number of bathrooms must be greater than 0", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            if (bathrooms > 10) {
                etBathrooms.setError("Number of bathrooms cannot exceed 10");
                etBathrooms.requestFocus();
                Toast.makeText(getActivity(), "Number of bathrooms cannot exceed 10", Toast.LENGTH_SHORT).show();
                return false;
            }
            
        } catch (NumberFormatException e) {
            etBathrooms.setError("Please enter a valid number");
            etBathrooms.requestFocus();
            Toast.makeText(getActivity(), "Please enter a valid number for bathrooms", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        etBathrooms.setError(null);
        return true;
    }
    
    /**
     * Validate Area (Optional)
     * Requirements: If provided, must be a positive number, maximum 10000 sqm
     */
    private boolean validateArea() {
        String areaStr = etArea.getText().toString().trim();
        
        if (TextUtils.isEmpty(areaStr)) {
            // Area is optional, so empty is valid
            etArea.setError(null);
            return true;
        }
        
        try {
            double area = Double.parseDouble(areaStr);
            
            if (area <= 0) {
                etArea.setError("Area must be greater than 0");
                etArea.requestFocus();
                Toast.makeText(getActivity(), "Area must be greater than 0", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            if (area > 10000) {
                etArea.setError("Area cannot exceed 10,000 sqm");
                etArea.requestFocus();
                Toast.makeText(getActivity(), "Area cannot exceed 10,000 square meters", Toast.LENGTH_SHORT).show();
                return false;
            }
            
        } catch (NumberFormatException e) {
            etArea.setError("Please enter a valid number");
            etArea.requestFocus();
            Toast.makeText(getActivity(), "Please enter a valid number for area", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        etArea.setError(null);
        return true;
    }
    
    /**
     * Validate Build Year (Optional)
     * Requirements: If provided, must be a valid year between 1900 and current year
     */
    private boolean validateBuildYear() {
        String buildYearStr = etBuildYear.getText().toString().trim();
        
        if (TextUtils.isEmpty(buildYearStr)) {
            // Build year is optional, so empty is valid
            etBuildYear.setError(null);
            return true;
        }
        
        try {
            int buildYear = Integer.parseInt(buildYearStr);
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            
            if (buildYear < 1900) {
                etBuildYear.setError("Build year cannot be before 1900");
                etBuildYear.requestFocus();
                Toast.makeText(getActivity(), "Build year cannot be before 1900", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            if (buildYear > currentYear) {
                etBuildYear.setError("Build year cannot be in the future");
                etBuildYear.requestFocus();
                Toast.makeText(getActivity(), "Build year cannot be in the future", Toast.LENGTH_SHORT).show();
                return false;
            }
            
        } catch (NumberFormatException e) {
            etBuildYear.setError("Please enter a valid year");
            etBuildYear.requestFocus();
            Toast.makeText(getActivity(), "Please enter a valid year", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        etBuildYear.setError(null);
        return true;
    }
    
    /**
     * Validate Images
     * Requirements: At least one image is required
     */
    private boolean validateImages() {
        if (imageUris.isEmpty()) {
            Toast.makeText(getActivity(), "Please add at least one image of the boarding house", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void saveCurrentData() {
        // Save current form data to static variables
        savedBhName = etBhName.getText().toString().trim();
        savedBhAddress = etBhAddress.getText().toString().trim();
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
        savedBhAddress = "";
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
}
