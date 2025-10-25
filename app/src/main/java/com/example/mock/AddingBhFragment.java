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

import java.util.Calendar;
import java.util.regex.Pattern;

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
        String address = etBhAddress.getText().toString().trim();
        String bathrooms = etBathrooms.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String buildYear = etBuildYear.getText().toString().trim();
        String description = etBhDescription.getText().toString().trim();
        String rules = etBhRules.getText().toString().trim();

        // Check required fields
        if (TextUtils.isEmpty(name)) {
            return "Boarding House Name is required";
        }
        if (TextUtils.isEmpty(address)) {
            return "Address is required";
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

        // Validate address (minimum 10 characters)
        if (address.length() < 10) {
            return "Address must be at least 10 characters";
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
        } else if (errorMessage.contains("Address")) {
            etBhAddress.requestFocus();
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
