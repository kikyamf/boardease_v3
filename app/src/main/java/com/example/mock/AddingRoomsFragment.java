package com.example.mock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddingRoomsFragment extends Fragment {

    private LinearLayout containerPrivateRooms, containerBedSpacers;
    private static final int IMAGE_PICK_CODE = 1001;
    private ImageView currentImageView;
    private int bhId = -1;

    private int userId;
    private String bhName, bhAddress, bhDescription, bhRules, bhBathrooms, bhArea, bhBuildYear;
    private ArrayList<Uri> boardingHouseImages;

    private ArrayList<Uri> currentRoomImageList;
    private ImageAdapter currentRoomAdapter;
    private ViewPager2 currentRoomViewPager;
    private View currentRoomPlaceholder;


    private ArrayList<Uri> imageUris = new ArrayList<>();
    private ImageAdapter imageAdapter;


    private String mode = "add"; // "add" or "edit"

    private List<RoomData> allRooms = new ArrayList<>();
    private int currentRoomIndex = 0;
    
    // Progress dialogs
    private ProgressDialog saveProgressDialog;
    private ProgressDialog imageUploadDialog;
    
    // Static variables to preserve room data when navigating back
    private static ArrayList<RoomFormData> savedRoomData = new ArrayList<>();

    // PHP Endpoints
    private static final String ADD_BH_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/add_boarding_house.php";
    private static final String UPDATE_BH_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/update_boarding_house.php";
    private static final String UPLOAD_BH_IMAGE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/upload_bh_image.php";
    private static final String ADD_ROOM_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/add_room.php";
    private static final String UPDATE_ROOM_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/update_room.php";
    private static final String UPLOAD_ROOM_IMAGE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/upload_room_image.php";

    public AddingRoomsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_adding_rooms, container, false);

        containerPrivateRooms = view.findViewById(R.id.containerPrivateRooms);
        containerBedSpacers = view.findViewById(R.id.containerBedSpacers);
        
        // Hide header if used in AddRoomsActivity (has its own header)
        RelativeLayout headerLayout = view.findViewById(R.id.headerLayout);
        if (getActivity() instanceof AddRoomsActivity) {
            headerLayout.setVisibility(View.GONE);
        } else {
            // Show header and setup back button for other contexts
            ImageButton btnBack = view.findViewById(R.id.btnBack);
            btnBack.setOnClickListener(v -> {
                // Save current room data before going back
                saveCurrentRoomData();
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // Receive Boarding House data
        if (getArguments() != null) {
            userId = getArguments().getInt("user_id");
            bhName = getArguments().getString("bh_name");
            bhAddress = getArguments().getString("bh_address");
            bhDescription = getArguments().getString("bh_description");
            bhRules = getArguments().getString("bh_rules");
            bhBathrooms = getArguments().getString("bh_bathrooms");
            bhArea = getArguments().getString("bh_area");
            bhBuildYear = getArguments().getString("bh_build_year");
            boardingHouseImages = getArguments().getParcelableArrayList("bh_images");

            mode = getArguments().getString("mode", "add");
            bhId = getArguments().getInt("bh_id", -1);

            if (mode.equals("edit")) {
                ArrayList<Bundle> existingRooms = getArguments().getParcelableArrayList("rooms");
                if (existingRooms != null) {
                    for (Bundle roomBundle : existingRooms) {
                        addPrefilledRoom(inflater, roomBundle);
                    }
                }
            }
        }

        // Add Private Room
        ImageButton btnAddPrivateRoom = view.findViewById(R.id.btnAddPrivateRoom);
        btnAddPrivateRoom.setOnClickListener(v -> addNewRoom(inflater, "Private Room"));

        // Add Bed Spacer
        ImageButton btnAddBedSpacer = view.findViewById(R.id.btnAddBedSpacer);
        btnAddBedSpacer.setOnClickListener(v -> addNewRoom(inflater, "Bed Spacer"));

        Button btnSaveAll = view.findViewById(R.id.btnSaveAll);
        btnSaveAll.setOnClickListener(v -> showSaveConfirmationDialog());

        // Restore saved room data if any
        restoreSavedRoomData();

        return view;
    }

    private void addNewRoom(LayoutInflater inflater, String category) {
        View form = "Private Room".equals(category)
                ? inflater.inflate(R.layout.item_private_form, containerPrivateRooms, false)
                : inflater.inflate(R.layout.item_bed_form, containerBedSpacers, false);

        if ("Private Room".equals(category)) containerPrivateRooms.addView(form);
        else containerBedSpacers.addView(form);

        ImageButton btnRemove = form.findViewById(R.id.btnRemoveRoom) != null ?
                form.findViewById(R.id.btnRemoveRoom) : form.findViewById(R.id.btnRemoveBed);
        if (btnRemove != null) btnRemove.setOnClickListener(v -> {
            if ("Private Room".equals(category)) containerPrivateRooms.removeView(form);
            else containerBedSpacers.removeView(form);
        });

        // Gallery views inside the form
        final ViewPager2 viewPager = form.findViewById(R.id.viewPagerImages);
        final View placeholder = form.findViewById(R.id.layoutUploadPlaceholder);

        // Create list for this room’s images
        final ArrayList<Uri> roomImageUris = new ArrayList<>();

// Create a wrapper so we can reference the adapter inside the listener
        final ImageAdapter[] adapterHolder = new ImageAdapter[1];

        ImageAdapter.OnImageRemoveListener listener = pos -> {
            if (pos >= 0 && pos < roomImageUris.size()) {
                roomImageUris.remove(pos);
                adapterHolder[0].notifyItemRemoved(pos); // safe call
                if (roomImageUris.isEmpty()) {
                    placeholder.setVisibility(View.VISIBLE);
                    viewPager.setVisibility(View.GONE);
                }
            }
        };

// Create adapter and set it into the wrapper
        final ImageAdapter adapter = new ImageAdapter(getActivity(), roomImageUris, listener);
        adapterHolder[0] = adapter;

        viewPager.setAdapter(adapter);

        // click placeholder to pick images for this specific room
        placeholder.setOnClickListener(v -> {
            currentRoomImageList = roomImageUris;
            currentRoomAdapter = adapter;
            currentRoomViewPager = viewPager;
            currentRoomPlaceholder = placeholder;
            pickImages();
        });

        // store the list on the form for later extraction
        form.setTag(R.id.room_images, roomImageUris);
    }




    private void addPrefilledRoom(LayoutInflater inflater, Bundle roomBundle) {
        String category = roomBundle.getString("category");
        View form;
        if ("Private Room".equals(category)) {
            form = inflater.inflate(R.layout.item_private_form, containerPrivateRooms, false);
            containerPrivateRooms.addView(form);
        } else {
            form = inflater.inflate(R.layout.item_bed_form, containerBedSpacers, false);
            containerBedSpacers.addView(form);
        }

        EditText etTitle = form.findViewById(R.id.etTitle) != null ? form.findViewById(R.id.etTitle) : form.findViewById(R.id.etBedTitle);
        EditText etDesc = form.findViewById(R.id.etDesc) != null ? form.findViewById(R.id.etDesc) : form.findViewById(R.id.etBedDesc);
        EditText etPrice = form.findViewById(R.id.etPrice) != null ? form.findViewById(R.id.etPrice) : form.findViewById(R.id.etBedPrice);
        EditText etCapacity = form.findViewById(R.id.etCapacity) != null ? form.findViewById(R.id.etCapacity) : form.findViewById(R.id.etBedCapacity);
        EditText etTotal = form.findViewById(R.id.etTotalRooms) != null ? form.findViewById(R.id.etTotalRooms) : form.findViewById(R.id.etBedTotalRooms);

        etTitle.setText(roomBundle.getString("title", ""));
        etDesc.setText(roomBundle.getString("description", ""));
        etPrice.setText(roomBundle.getString("price", ""));
        etCapacity.setText(roomBundle.getString("capacity", ""));
        etTotal.setText(roomBundle.getString("totalRooms", ""));

        final ViewPager2 viewPager = form.findViewById(R.id.viewPagerImages);
        final View placeholder = form.findViewById(R.id.layoutUploadPlaceholder);

        // Create list for this room’s images
        final ArrayList<Uri> roomImageUris = new ArrayList<>();

// Create a wrapper so we can reference the adapter inside the listener
        final ImageAdapter[] adapterHolder = new ImageAdapter[1];

        ImageAdapter.OnImageRemoveListener listener = pos -> {
            if (pos >= 0 && pos < roomImageUris.size()) {
                roomImageUris.remove(pos);
                adapterHolder[0].notifyItemRemoved(pos); // safe call
                if (roomImageUris.isEmpty()) {
                    placeholder.setVisibility(View.VISIBLE);
                    viewPager.setVisibility(View.GONE);
                }
            }
        };

// Create adapter and set it into the wrapper
        final ImageAdapter adapter = new ImageAdapter(getActivity(), roomImageUris, listener);
        adapterHolder[0] = adapter;

        viewPager.setAdapter(adapter);


        if (!roomImageUris.isEmpty()) {
            placeholder.setVisibility(View.GONE);
            viewPager.setVisibility(View.VISIBLE);
        }

        placeholder.setOnClickListener(v -> {
            currentRoomImageList = roomImageUris;
            currentRoomAdapter = adapter;
            currentRoomViewPager = viewPager;
            currentRoomPlaceholder = placeholder;
            pickImages();
        });

        form.setTag(R.id.room_images, roomImageUris);

        ImageButton btnRemove = form.findViewById(R.id.btnRemoveRoom) != null ? form.findViewById(R.id.btnRemoveRoom) : form.findViewById(R.id.btnRemoveBed);
        if (btnRemove != null) btnRemove.setOnClickListener(v -> {
            if ("Private Room".equals(category)) containerPrivateRooms.removeView(form);
            else containerBedSpacers.removeView(form);
        });
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

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            if (currentRoomImageList == null || currentRoomAdapter == null || currentRoomViewPager == null || currentRoomPlaceholder == null) {
                Toast.makeText(getActivity(), "No room selected for images", Toast.LENGTH_SHORT).show();
                return;
            }

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    try {
                        requireActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    
                    // Verify image before adding
                    verifyAndAddRoomImage(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                try {
                    requireActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                
                // Verify image before adding
                verifyAndAddRoomImage(uri);
            }

            // reset current room pointers (optional, to avoid accidental reuse)
            currentRoomImageList = null;
            currentRoomAdapter = null;
            currentRoomViewPager = null;
            currentRoomPlaceholder = null;
        }
    }
    
    private void verifyAndAddRoomImage(Uri imageUri) {
        // Show loading message
        Toast.makeText(getActivity(), "Verifying image...", Toast.LENGTH_SHORT).show();
        
        // Use smart verification (API if available, local if not)
        ImageVerification.verifyImage(getActivity(), imageUri, new ImageVerification.VerificationCallback() {
            @Override
            public void onVerificationComplete(boolean isApproved, String reason) {
                if (isApproved) {
                    // Add image to list
                    if (!currentRoomImageList.contains(imageUri)) {
                        currentRoomImageList.add(imageUri);
                        currentRoomAdapter.notifyDataSetChanged();
                        
                        if (!currentRoomImageList.isEmpty()) {
                            currentRoomViewPager.setVisibility(View.VISIBLE);
                            currentRoomPlaceholder.setVisibility(View.GONE);
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









    private void showSaveConfirmationDialog() {
        // Validate all room forms first
        String validationError = validateAllRoomForms();
        if (validationError != null) {
            showValidationDialog(validationError);
            return;
        }

        // Count total rooms
        int privateRooms = containerPrivateRooms.getChildCount();
        int bedSpacers = containerBedSpacers.getChildCount();
        int totalRooms = privateRooms + bedSpacers;
        
        // Count total images
        int totalImages = 0;
        if (boardingHouseImages != null) {
            totalImages += boardingHouseImages.size();
        }
        
        // Count room images
        for (int i = 0; i < containerPrivateRooms.getChildCount(); i++) {
            View form = containerPrivateRooms.getChildAt(i);
            ViewPager2 viewPager = form.findViewById(R.id.viewPagerImages);
            if (viewPager != null && viewPager.getAdapter() != null) {
                totalImages += viewPager.getAdapter().getItemCount();
            }
        }
        for (int i = 0; i < containerBedSpacers.getChildCount(); i++) {
            View form = containerBedSpacers.getChildAt(i);
            ViewPager2 viewPager = form.findViewById(R.id.viewPagerImages);
            if (viewPager != null && viewPager.getAdapter() != null) {
                totalImages += viewPager.getAdapter().getItemCount();
            }
        }
        
        // Create confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Review & Save");
        
        String message = "Are you sure you want to save this boarding house?\n\n" +
                        "📋 Boarding House: " + bhName + "\n" +
                        "📍 Address: " + bhAddress + "\n" +
                        "🏠 Total Rooms: " + totalRooms + " (" + privateRooms + " Private, " + bedSpacers + " Bed Spacers)\n" +
                        "📸 Total Images: " + totalImages + "\n\n" +
                        "This will save all details and images to the database.";
        
        builder.setMessage(message);
        
        builder.setPositiveButton("Save All", (dialog, which) -> {
            dialog.dismiss();
            saveBoardingHouse();
        });
        
        builder.setNegativeButton("Review", (dialog, which) -> {
            dialog.dismiss();
            // Stay on current screen for review
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveBoardingHouse() {
        // Show loading dialog
        saveProgressDialog = new ProgressDialog(getActivity());
        saveProgressDialog.setMessage("Saving boarding house...");
        saveProgressDialog.setCancelable(false);
        saveProgressDialog.show();
        
        // If we're in edit mode and just adding rooms, skip boarding house update
        if (mode.equals("edit") && bhId > 0) {
            System.out.println("DEBUG: Skipping boarding house update, just adding rooms to existing BH ID: " + bhId);
            // Update progress dialog
            if (saveProgressDialog != null) {
                saveProgressDialog.setMessage("Uploading images...");
            }
            uploadBhImagesSequential(0);
            return;
        }
        
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = mode.equals("edit") ? UPDATE_BH_URL : ADD_BH_URL;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        System.out.println("DEBUG: Save BH response: " + response);
                        JSONObject obj = new JSONObject(response);
                        if (obj.has("success")) {
                            if (mode.equals("add")) bhId = obj.getInt("success");
                            System.out.println("DEBUG: BH saved with ID: " + bhId);
                            
                            // Update progress dialog
                            if (saveProgressDialog != null) {
                                saveProgressDialog.setMessage("Uploading images...");
                            }
                            
                            uploadBhImagesSequential(0);
                        } else {
                            if (saveProgressDialog != null) {
                                saveProgressDialog.dismiss();
                            }
                            String errorMsg = obj.optString("error", "Unknown error");
                            System.out.println("DEBUG: BH save error: " + errorMsg);
                            Toast.makeText(getActivity(), "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (saveProgressDialog != null) {
                            saveProgressDialog.dismiss();
                        }
                        System.out.println("DEBUG: BH save exception: " + e.getMessage());
                        Toast.makeText(getActivity(), "Failed parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (saveProgressDialog != null) {
                        saveProgressDialog.dismiss();
                    }
                    System.out.println("DEBUG: BH save network error: " + error.getMessage());
                    Toast.makeText(getActivity(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                if (mode.equals("edit")) params.put("bh_id", String.valueOf(bhId));
                params.put("user_id", String.valueOf(userId));
                params.put("bh_name", bhName);
                params.put("bh_address", bhAddress);
                params.put("bh_description", bhDescription);
                params.put("bh_rules", bhRules);
                params.put("number_of_bathroom", bhBathrooms);
                params.put("area", bhArea);
                params.put("build_year", bhBuildYear);
                params.put("status", "Active");
                return params;
            }
        };
        queue.add(request);
    }

    private void uploadBhImagesSequential(int index) {
        // Check if no images to upload
        if (boardingHouseImages == null || boardingHouseImages.isEmpty()) {
            System.out.println("DEBUG: No boarding house images to upload, proceeding to save rooms");
            saveAllRooms();
            return;
        }
        
        if (index >= boardingHouseImages.size()) {
            System.out.println("DEBUG: All boarding house images uploaded, proceeding to save rooms");
            saveAllRooms();
            return;
        }

        Uri uri = boardingHouseImages.get(index);
        System.out.println("DEBUG: Uploading BH image " + (index + 1) + "/" + boardingHouseImages.size() + ": " + uri.toString());
        
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                System.out.println("ERROR: Could not open input stream for URI: " + uri.toString());
                Toast.makeText(getActivity(), "Error: Could not read image file", Toast.LENGTH_SHORT).show();
                uploadBhImagesSequential(index + 1); // Skip this image and continue
                return;
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; // Increased buffer size
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            String encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            inputStream.close();

            if (encoded.isEmpty()) {
                System.out.println("ERROR: Encoded image is empty for URI: " + uri.toString());
                Toast.makeText(getActivity(), "Error: Image file is empty", Toast.LENGTH_SHORT).show();
                uploadBhImagesSequential(index + 1); // Skip this image and continue
                return;
            }

            RequestQueue queue = Volley.newRequestQueue(getActivity());
            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_BH_IMAGE_URL,
                    response -> {
                        System.out.println("DEBUG: BH Image upload response: " + response);
                        uploadBhImagesSequential(index + 1);
                    },
                    error -> {
                        System.out.println("ERROR: BH Image upload failed: " + error.getMessage());
                        Toast.makeText(getActivity(), "BH Image upload failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        uploadBhImagesSequential(index + 1); // Continue with next image
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("bh_id", String.valueOf(bhId));
                    params.put("image_base64", encoded);
                    return params;
                }
            };
            
            // Set timeout for faster feedback
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(8000, 1, 1.0f));
            queue.add(request);
            
        } catch (Exception e) {
            System.out.println("ERROR: Exception uploading BH image: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            uploadBhImagesSequential(index + 1); // Continue with next image
        }
    }

    private void saveAllRooms() {
        allRooms.clear();
        currentRoomIndex = 0;

        for (int i = 0; i < containerPrivateRooms.getChildCount(); i++)
            allRooms.add(extractRoomData(containerPrivateRooms.getChildAt(i), "Private Room"));

        for (int i = 0; i < containerBedSpacers.getChildCount(); i++)
            allRooms.add(extractRoomData(containerBedSpacers.getChildAt(i), "Bed Spacer"));

        if (allRooms.isEmpty()) {
            // No rooms to save, complete the process
            completeSaveProcess();
            return;
        }

        // Update progress dialog
        if (saveProgressDialog != null) {
            saveProgressDialog.setMessage("Saving rooms...");
        }
        
        addRoomSequential();
    }

    private void addRoomSequential() {
        if (currentRoomIndex >= allRooms.size()) {
            // All rooms saved, complete the process
            completeSaveProcess();
            return;
        }

        RoomData room = allRooms.get(currentRoomIndex);
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = (mode.equals("edit") && room.roomId > 0) ? UPDATE_ROOM_URL : ADD_ROOM_URL;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        System.out.println("DEBUG: Save room response: " + response);
                        JSONObject obj = new JSONObject(response);
                        int roomId = obj.optInt("bhr_id", 0);
                        System.out.println("DEBUG: Room saved with ID: " + roomId);
                        
                        if (room.imageUris != null && !room.imageUris.isEmpty()) {
                            uploadRoomImagesSequential(room.imageUris, 0, roomId);
                        } else {
                            // No images for this room, move to next room
                            currentRoomIndex++;
                            addRoomSequential();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        currentRoomIndex++;
                        addRoomSequential();
                    }
                },
                error -> {
                    System.out.println("ERROR: Room save failed: " + error.getMessage());
                    currentRoomIndex++;
                    addRoomSequential();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                if (mode.equals("edit") && room.roomId > 0) params.put("bhr_id", String.valueOf(room.roomId));
                params.put("bh_id", String.valueOf(bhId));
                params.put("category", room.category);
                params.put("title", room.title);
                params.put("room_description", room.description);
                params.put("price", room.price);
                params.put("capacity", room.capacity);
                params.put("total_rooms", room.totalRooms);
                
                // Debug logging
                System.out.println("DEBUG: Sending room data to server:");
                System.out.println("DEBUG: room_description = '" + room.description + "'");
                System.out.println("DEBUG: title = '" + room.title + "'");
                System.out.println("DEBUG: price = '" + room.price + "'");
                System.out.println("DEBUG: capacity = '" + room.capacity + "'");
                System.out.println("DEBUG: total_rooms = '" + room.totalRooms + "'");
                
                // Debug all parameters being sent
                System.out.println("DEBUG: All parameters being sent:");
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    System.out.println("DEBUG: " + entry.getKey() + " = '" + entry.getValue() + "'");
                }
                
                return params;
            }
        };


        queue.add(request);
    }

    private void uploadRoomImagesSequential(List<Uri> uris, int index, int bhrId) {
        if (index >= uris.size()) {
            // All room images uploaded, move to next room
            currentRoomIndex++;
            addRoomSequential();
            return;
        }
        
        Uri imageUri = uris.get(index);
        System.out.println("DEBUG: Uploading room image " + (index + 1) + "/" + uris.size() + " for room ID: " + bhrId);

        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                System.out.println("ERROR: Could not open input stream for room image URI: " + imageUri.toString());
                uploadRoomImagesSequential(uris, index + 1, bhrId); // Skip this image
                return;
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; // Increased buffer size
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            String encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            inputStream.close();

            if (encoded.isEmpty()) {
                System.out.println("ERROR: Encoded room image is empty for URI: " + imageUri.toString());
                uploadRoomImagesSequential(uris, index + 1, bhrId); // Skip this image
                return;
            }

            RequestQueue queue = Volley.newRequestQueue(getActivity());
            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_ROOM_IMAGE_URL,
                    response -> {
                        System.out.println("DEBUG: Room image upload response: " + response);
                        uploadRoomImagesSequential(uris, index + 1, bhrId);
                    },
                    error -> {
                        System.out.println("ERROR: Room image upload failed: " + error.getMessage());
                        uploadRoomImagesSequential(uris, index + 1, bhrId); // Continue with next image
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("bhr_id", String.valueOf(bhrId));
                    params.put("image_base64", encoded);
                    return params;
                }
            };
            
            // Set timeout for faster feedback
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(8000, 1, 1.0f));
            queue.add(request);
        } catch (Exception e) {
            System.out.println("ERROR: Exception uploading room image: " + e.getMessage());
            e.printStackTrace();
            uploadRoomImagesSequential(uris, index + 1, bhrId); // Continue with next image
        }
    }

    private RoomData extractRoomData(View form, String category) {
        EditText etTitle = form.findViewById(R.id.etTitle) != null ? form.findViewById(R.id.etTitle) : form.findViewById(R.id.etBedTitle);
        EditText etDesc = form.findViewById(R.id.etDesc) != null ? form.findViewById(R.id.etDesc) : form.findViewById(R.id.etBedDesc);
        EditText etPrice = form.findViewById(R.id.etPrice) != null ? form.findViewById(R.id.etPrice) : form.findViewById(R.id.etBedPrice);
        EditText etCapacity = form.findViewById(R.id.etCapacity) != null ? form.findViewById(R.id.etCapacity) : form.findViewById(R.id.etBedCapacity);
        EditText etTotal = form.findViewById(R.id.etTotalRooms) != null ? form.findViewById(R.id.etTotalRooms) : form.findViewById(R.id.etBedTotalRooms);

        List<Uri> imgUris = (List<Uri>) form.getTag(R.id.room_images);
        if (imgUris == null) imgUris = new ArrayList<>();

        return new RoomData(category,
                etTitle.getText().toString().trim(),
                etDesc.getText().toString().trim(),
                etPrice.getText().toString().trim(),
                etCapacity.getText().toString().trim(),
                etTotal.getText().toString().trim(),
                imgUris,
                (form.getTag() instanceof Integer) ? (Integer) form.getTag() : 0
        );
    }

    private static class RoomData {
        String category, title, description, price, capacity, totalRooms;
        List<Uri> imageUris;   // instead of single Uri
        int roomId; // for edit mode

        public RoomData(String category, String title, String description,
                        String price, String capacity, String totalRooms,
                        List<Uri> imageUris, int roomId) {
            this.category = category;
            this.title = title;
            this.description = description;
            this.price = price;
            this.capacity = capacity;
            this.totalRooms = totalRooms;
            this.imageUris = imageUris;
            this.roomId = roomId;
        }
    }

    private void completeSaveProcess() {
        // Dismiss progress dialog
        if (saveProgressDialog != null) {
            saveProgressDialog.dismiss();
        }
        
        // Show success message
        Toast.makeText(getActivity(), "Boarding house and rooms saved successfully!", Toast.LENGTH_LONG).show();
        
        // Clear saved data since save was successful
        AddingBhFragment.clearSavedData();
        clearSavedRoomData();
        
        // Navigate based on context
        if (getActivity() != null) {
            if (getActivity() instanceof AddRoomsActivity) {
                // If used in AddRoomsActivity, just finish the activity (goes back to RoomViewActivity)
                getActivity().finish();
            } else {
                // If used in other contexts (like main Add New Listing flow), go back to AddingBhFragment
                getActivity().getSupportFragmentManager().popBackStack();
                
                // Clear the form by creating a new AddingBhFragment
                AddingBhFragment newFragment = AddingBhFragment.newInstance(userId);
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, newFragment)
                        .commit();
            }
        }
    }
    
    // Method to clear saved room data (call this after successful save)
    public static void clearSavedRoomData() {
        savedRoomData.clear();
    }
    
    // Method to save current room data
    private void saveCurrentRoomData() {
        savedRoomData.clear();
        
        // Save private rooms
        for (int i = 0; i < containerPrivateRooms.getChildCount(); i++) {
            View form = containerPrivateRooms.getChildAt(i);
            RoomFormData roomData = extractRoomFormData(form, "Private Room");
            if (roomData != null) {
                savedRoomData.add(roomData);
            }
        }
        
        // Save bed spacers
        for (int i = 0; i < containerBedSpacers.getChildCount(); i++) {
            View form = containerBedSpacers.getChildAt(i);
            RoomFormData roomData = extractRoomFormData(form, "Bed Spacer");
            if (roomData != null) {
                savedRoomData.add(roomData);
            }
        }
    }
    
    // Method to restore saved room data
    private void restoreSavedRoomData() {
        if (savedRoomData.isEmpty()) return;
        
        LayoutInflater inflater = getLayoutInflater();
        
        for (RoomFormData roomData : savedRoomData) {
            View form = "Private Room".equals(roomData.category)
                    ? inflater.inflate(R.layout.item_private_form, containerPrivateRooms, false)
                    : inflater.inflate(R.layout.item_bed_form, containerBedSpacers, false);
            
            // Populate form with saved data
            populateRoomForm(form, roomData);
            
            // Add to appropriate container
            if ("Private Room".equals(roomData.category)) {
                containerPrivateRooms.addView(form);
            } else {
                containerBedSpacers.addView(form);
            }
            
            // Set up remove button
            ImageButton btnRemove = form.findViewById(R.id.btnRemoveRoom) != null ?
                    form.findViewById(R.id.btnRemoveRoom) : form.findViewById(R.id.btnRemoveBed);
            if (btnRemove != null) {
                btnRemove.setOnClickListener(v -> {
                    if ("Private Room".equals(roomData.category)) {
                        containerPrivateRooms.removeView(form);
                    } else {
                        containerBedSpacers.removeView(form);
                    }
                });
            }
        }
    }
    
    private RoomFormData extractRoomFormData(View form, String category) {
        EditText etTitle = form.findViewById(R.id.etTitle) != null ? form.findViewById(R.id.etTitle) : form.findViewById(R.id.etBedTitle);
        EditText etDesc = form.findViewById(R.id.etDesc) != null ? form.findViewById(R.id.etDesc) : form.findViewById(R.id.etBedDesc);
        EditText etPrice = form.findViewById(R.id.etPrice) != null ? form.findViewById(R.id.etPrice) : form.findViewById(R.id.etBedPrice);
        EditText etCapacity = form.findViewById(R.id.etCapacity) != null ? form.findViewById(R.id.etCapacity) : form.findViewById(R.id.etBedCapacity);
        EditText etTotalRooms = form.findViewById(R.id.etTotalRooms) != null ? form.findViewById(R.id.etTotalRooms) : form.findViewById(R.id.etBedTotalRooms);
        
        if (etTitle == null || etDesc == null || etPrice == null || etCapacity == null || etTotalRooms == null) {
            return null;
        }
        
        String title = etTitle.getText().toString().trim();
        String description = etDesc.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String capacity = etCapacity.getText().toString().trim();
        String totalRooms = etTotalRooms.getText().toString().trim();
        
        // Debug logging
        System.out.println("DEBUG: extractRoomFormData - Extracted form data:");
        System.out.println("DEBUG: title = '" + title + "'");
        System.out.println("DEBUG: description = '" + description + "'");
        System.out.println("DEBUG: price = '" + price + "'");
        System.out.println("DEBUG: capacity = '" + capacity + "'");
        System.out.println("DEBUG: totalRooms = '" + totalRooms + "'");
        
        // Additional debugging for EditText objects
        System.out.println("DEBUG: etDesc object: " + etDesc);
        System.out.println("DEBUG: etDesc text: '" + etDesc.getText().toString() + "'");
        System.out.println("DEBUG: etDesc hint: '" + etDesc.getHint() + "'");
        
        // Get images from ViewPager
        ViewPager2 viewPager = form.findViewById(R.id.viewPagerImages);
        ArrayList<Uri> imageUris = new ArrayList<>();
        if (viewPager != null && viewPager.getAdapter() != null) {
            ImageAdapter adapter = (ImageAdapter) viewPager.getAdapter();
            imageUris.addAll(adapter.getImageUris());
        }
        
        return new RoomFormData(category, title, description, price, capacity, totalRooms, imageUris);
    }
    
    private void populateRoomForm(View form, RoomFormData roomData) {
        EditText etTitle = form.findViewById(R.id.etTitle) != null ? form.findViewById(R.id.etTitle) : form.findViewById(R.id.etBedTitle);
        EditText etDesc = form.findViewById(R.id.etDesc) != null ? form.findViewById(R.id.etDesc) : form.findViewById(R.id.etBedDesc);
        EditText etPrice = form.findViewById(R.id.etPrice) != null ? form.findViewById(R.id.etPrice) : form.findViewById(R.id.etBedPrice);
        EditText etCapacity = form.findViewById(R.id.etCapacity) != null ? form.findViewById(R.id.etCapacity) : form.findViewById(R.id.etBedCapacity);
        EditText etTotalRooms = form.findViewById(R.id.etTotalRooms) != null ? form.findViewById(R.id.etTotalRooms) : form.findViewById(R.id.etBedTotalRooms);
        
        if (etTitle != null) etTitle.setText(roomData.title);
        if (etDesc != null) etDesc.setText(roomData.description);
        if (etPrice != null) etPrice.setText(roomData.price);
        if (etCapacity != null) etCapacity.setText(roomData.capacity);
        if (etTotalRooms != null) etTotalRooms.setText(roomData.totalRooms);
        
        // Set up images
        if (!roomData.imageUris.isEmpty()) {
            ViewPager2 viewPager = form.findViewById(R.id.viewPagerImages);
            View placeholder = form.findViewById(R.id.layoutUploadPlaceholder);
            
            if (viewPager != null && placeholder != null) {
                // Create a copy of the image list to avoid reference issues
                final ArrayList<Uri> imageList = new ArrayList<>(roomData.imageUris);
                final ViewPager2 finalViewPager = viewPager;
                final View finalPlaceholder = placeholder;
                
                // Create the adapter with a final reference
                final ImageAdapter[] adapterRef = new ImageAdapter[1];
                adapterRef[0] = new ImageAdapter(getActivity(), imageList, new ImageAdapter.OnImageRemoveListener() {
                    @Override
                    public void onImageRemoved(int position) {
                        imageList.remove(position);
                        roomData.imageUris.remove(position);
                        if (adapterRef[0] != null) {
                            adapterRef[0].notifyDataSetChanged();
                        }
                        if (imageList.isEmpty()) {
                            finalPlaceholder.setVisibility(View.VISIBLE);
                            finalViewPager.setVisibility(View.GONE);
                        }
                    }
                });
                
                finalViewPager.setAdapter(adapterRef[0]);
                finalPlaceholder.setVisibility(View.GONE);
                finalViewPager.setVisibility(View.VISIBLE);
            }
        }
    }
    
    // Data class for room form data
    private static class RoomFormData {
        String category;
        String title;
        String description;
        String price;
        String capacity;
        String totalRooms;
        ArrayList<Uri> imageUris;
        
        RoomFormData(String category, String title, String description, String price, String capacity, String totalRooms, ArrayList<Uri> imageUris) {
            this.category = category;
            this.title = title;
            this.description = description;
            this.price = price;
            this.capacity = capacity;
            this.totalRooms = totalRooms;
            this.imageUris = imageUris;
        }
    }

    // ========== VALIDATION METHODS ==========
    
    private String validateAllRoomForms() {
        // Check if at least one room is added
        int privateRoomCount = containerPrivateRooms.getChildCount();
        int bedSpacerCount = containerBedSpacers.getChildCount();
        
        if (privateRoomCount == 0 && bedSpacerCount == 0) {
            return "At least one room must be added";
        }
        
        // Validate all private rooms
        for (int i = 0; i < privateRoomCount; i++) {
            View roomForm = containerPrivateRooms.getChildAt(i);
            String error = validatePrivateRoomForm(roomForm, i + 1);
            if (error != null) {
                return error;
            }
        }
        
        // Validate all bed spacers
        for (int i = 0; i < bedSpacerCount; i++) {
            View bedForm = containerBedSpacers.getChildAt(i);
            String error = validateBedSpacerForm(bedForm, i + 1);
            if (error != null) {
                return error;
            }
        }
        
        return null; // All validations passed
    }
    
    private String validatePrivateRoomForm(View form, int roomNumber) {
        EditText etTitle = form.findViewById(R.id.etTitle);
        EditText etDesc = form.findViewById(R.id.etDesc);
        EditText etPrice = form.findViewById(R.id.etPrice);
        EditText etCapacity = form.findViewById(R.id.etCapacity);
        EditText etTotalRooms = form.findViewById(R.id.etTotalRooms);
        
        String title = etTitle.getText().toString().trim();
        String description = etDesc.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String capacity = etCapacity.getText().toString().trim();
        String totalRooms = etTotalRooms.getText().toString().trim();
        
        // Validate required fields
        if (TextUtils.isEmpty(title)) {
            return "Private Room #" + roomNumber + ": Title is required";
        }
        if (TextUtils.isEmpty(price)) {
            return "Private Room #" + roomNumber + ": Price is required";
        }
        if (TextUtils.isEmpty(capacity)) {
            return "Private Room #" + roomNumber + ": Capacity is required";
        }
        if (TextUtils.isEmpty(totalRooms)) {
            return "Private Room #" + roomNumber + ": Total Units is required";
        }
        
        // Validate title (2-50 characters, letters, numbers, spaces, hyphens, and apostrophes)
        if (title.length() < 2 || title.length() > 50) {
            return "Private Room #" + roomNumber + ": Title must be 2-50 characters long";
        }
        if (!Pattern.matches("^[a-zA-Z0-9\\s\\-']+$", title)) {
            return "Private Room #" + roomNumber + ": Title can only contain letters, numbers, spaces, hyphens, and apostrophes";
        }
        
        // Validate price (must be positive number)
        try {
            double priceValue = Double.parseDouble(price);
            if (priceValue <= 0) {
                return "Private Room #" + roomNumber + ": Price must be a positive number";
            }
            if (priceValue > 100000) {
                return "Private Room #" + roomNumber + ": Price cannot exceed ₱100,000";
            }
        } catch (NumberFormatException e) {
            return "Private Room #" + roomNumber + ": Price must be a valid number";
        }
        
        // Validate capacity (1-10 persons)
        try {
            int capacityValue = Integer.parseInt(capacity);
            if (capacityValue < 1 || capacityValue > 10) {
                return "Private Room #" + roomNumber + ": Capacity must be between 1 and 10 persons";
            }
        } catch (NumberFormatException e) {
            return "Private Room #" + roomNumber + ": Capacity must be a valid number";
        }
        
        // Validate total rooms (1-50 units)
        try {
            int totalRoomsValue = Integer.parseInt(totalRooms);
            if (totalRoomsValue < 1 || totalRoomsValue > 50) {
                return "Private Room #" + roomNumber + ": Total Units must be between 1 and 50";
            }
        } catch (NumberFormatException e) {
            return "Private Room #" + roomNumber + ": Total Units must be a valid number";
        }
        
        // Validate description length if provided (max 300 characters)
        if (!TextUtils.isEmpty(description) && description.length() > 300) {
            return "Private Room #" + roomNumber + ": Description cannot exceed 300 characters";
        }
        
        // Validate images (at least 1 image required)
        ArrayList<Uri> imageUris = (ArrayList<Uri>) form.getTag(R.id.room_images);
        if (imageUris == null || imageUris.isEmpty()) {
            return "Private Room #" + roomNumber + ": At least 1 image is required";
        }
        
        return null; // Validation passed
    }
    
    private String validateBedSpacerForm(View form, int bedNumber) {
        EditText etTitle = form.findViewById(R.id.etBedTitle);
        EditText etDesc = form.findViewById(R.id.etBedDesc);
        EditText etPrice = form.findViewById(R.id.etBedPrice);
        EditText etCapacity = form.findViewById(R.id.etBedCapacity);
        EditText etTotalRooms = form.findViewById(R.id.etBedTotalRooms);
        
        String title = etTitle.getText().toString().trim();
        String description = etDesc.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String capacity = etCapacity.getText().toString().trim();
        String totalRooms = etTotalRooms.getText().toString().trim();
        
        // Validate required fields
        if (TextUtils.isEmpty(title)) {
            return "Bed Spacer #" + bedNumber + ": Title is required";
        }
        if (TextUtils.isEmpty(price)) {
            return "Bed Spacer #" + bedNumber + ": Price is required";
        }
        if (TextUtils.isEmpty(capacity)) {
            return "Bed Spacer #" + bedNumber + ": Capacity is required";
        }
        if (TextUtils.isEmpty(totalRooms)) {
            return "Bed Spacer #" + bedNumber + ": Total Units is required";
        }
        
        // Validate title (2-50 characters, letters, numbers, spaces, hyphens, and apostrophes)
        if (title.length() < 2 || title.length() > 50) {
            return "Bed Spacer #" + bedNumber + ": Title must be 2-50 characters long";
        }
        if (!Pattern.matches("^[a-zA-Z0-9\\s\\-']+$", title)) {
            return "Bed Spacer #" + bedNumber + ": Title can only contain letters, numbers, spaces, hyphens, and apostrophes";
        }
        
        // Validate price (must be positive number)
        try {
            double priceValue = Double.parseDouble(price);
            if (priceValue <= 0) {
                return "Bed Spacer #" + bedNumber + ": Price must be a positive number";
            }
            if (priceValue > 50000) {
                return "Bed Spacer #" + bedNumber + ": Price cannot exceed ₱50,000";
            }
        } catch (NumberFormatException e) {
            return "Bed Spacer #" + bedNumber + ": Price must be a valid number";
        }
        
        // Validate capacity (1-6 persons for bed spacers)
        try {
            int capacityValue = Integer.parseInt(capacity);
            if (capacityValue < 1 || capacityValue > 6) {
                return "Bed Spacer #" + bedNumber + ": Capacity must be between 1 and 6 persons";
            }
        } catch (NumberFormatException e) {
            return "Bed Spacer #" + bedNumber + ": Capacity must be a valid number";
        }
        
        // Validate total rooms (1-30 units for bed spacers)
        try {
            int totalRoomsValue = Integer.parseInt(totalRooms);
            if (totalRoomsValue < 1 || totalRoomsValue > 30) {
                return "Bed Spacer #" + bedNumber + ": Total Units must be between 1 and 30";
            }
        } catch (NumberFormatException e) {
            return "Bed Spacer #" + bedNumber + ": Total Units must be a valid number";
        }
        
        // Validate description length if provided (max 300 characters)
        if (!TextUtils.isEmpty(description) && description.length() > 300) {
            return "Bed Spacer #" + bedNumber + ": Description cannot exceed 300 characters";
        }
        
        // Validate images (at least 1 image required)
        ArrayList<Uri> imageUris = (ArrayList<Uri>) form.getTag(R.id.room_images);
        if (imageUris == null || imageUris.isEmpty()) {
            return "Bed Spacer #" + bedNumber + ": At least 1 image is required";
        }
        
        return null; // Validation passed
    }
    
    private void showValidationDialog(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Validation Error")
                .setMessage(errorMessage)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    // Focus on the problematic form if possible
                    focusOnProblematicForm(errorMessage);
                })
                .setCancelable(false);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void focusOnProblematicForm(String errorMessage) {
        // Try to focus on the problematic form
        if (errorMessage.contains("Private Room")) {
            // Scroll to private rooms section
            containerPrivateRooms.requestFocus();
        } else if (errorMessage.contains("Bed Spacer")) {
            // Scroll to bed spacers section
            containerBedSpacers.requestFocus();
        }
    }

}
