package com.example.mock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.Toast;

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

    // PHP Endpoints
    private static final String ADD_BH_URL = "http://192.168.101.6/BoardEase2/add_boarding_house.php";
    private static final String UPDATE_BH_URL = "http://192.168.101.6/BoardEase2/update_boarding_house.php";
    private static final String UPLOAD_BH_IMAGE_URL = "http://192.168.101.6/BoardEase2/upload_bh_image.php";
    private static final String ADD_ROOM_URL = "http://192.168.101.6/BoardEase2/add_room.php";
    private static final String UPDATE_ROOM_URL = "http://192.168.101.6/BoardEase2/update_room.php";
    private static final String UPLOAD_ROOM_IMAGE_URL = "http://192.168.101.6/BoardEase2/upload_room_image.php";

    public AddingRoomsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_adding_rooms, container, false);

        containerPrivateRooms = view.findViewById(R.id.containerPrivateRooms);
        containerBedSpacers = view.findViewById(R.id.containerBedSpacers);
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

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
        btnSaveAll.setOnClickListener(v -> saveBoardingHouse());

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
                    currentRoomImageList.add(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                try {
                    requireActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                currentRoomImageList.add(uri);
            }

            // notify and update UI
            currentRoomAdapter.notifyDataSetChanged();
            currentRoomViewPager.setVisibility(View.VISIBLE);
            currentRoomPlaceholder.setVisibility(View.GONE);

            // reset current room pointers (optional, to avoid accidental reuse)
            currentRoomImageList = null;
            currentRoomAdapter = null;
            currentRoomViewPager = null;
            currentRoomPlaceholder = null;

            Toast.makeText(getActivity(), "Images added", Toast.LENGTH_SHORT).show();
        }
    }









    private void saveBoardingHouse() {
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = mode.equals("edit") ? UPDATE_BH_URL : ADD_BH_URL;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.has("success")) {
                            if (mode.equals("add")) bhId = obj.getInt("success");
                            uploadBhImagesSequential(0);
                        } else {
                            Toast.makeText(getActivity(), "Error: " + obj.optString("error"), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Failed parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getActivity(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
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
        if (boardingHouseImages == null || index >= boardingHouseImages.size()) {
            saveAllRooms();
            return;
        }

        Uri uri = boardingHouseImages.get(index);
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) baos.write(buffer, 0, read);
            String encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            inputStream.close();

            RequestQueue queue = Volley.newRequestQueue(getActivity());
            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_BH_IMAGE_URL,
                    response -> uploadBhImagesSequential(index + 1),
                    error -> Toast.makeText(getActivity(), "BH Image upload failed: " + error.getMessage(), Toast.LENGTH_SHORT).show()
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("bh_id", String.valueOf(bhId));
                    params.put("image_base64", encoded);
                    return params;
                }
            };
            queue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
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
            Toast.makeText(getActivity(), "No rooms to save", Toast.LENGTH_SHORT).show();
            return;
        }

        addRoomSequential();
    }

    private void addRoomSequential() {
        if (currentRoomIndex >= allRooms.size()) {
            Toast.makeText(getActivity(), "All data uploaded successfully", Toast.LENGTH_LONG).show();
            return;
        }

        RoomData room = allRooms.get(currentRoomIndex);
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = (mode.equals("edit") && room.roomId > 0) ? UPDATE_ROOM_URL : ADD_ROOM_URL;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        int roomId = obj.optInt("bhr_id", 0);
                        if (room.imageUris != null && !room.imageUris.isEmpty()) {
                            uploadRoomImagesSequential(room.imageUris, 0, roomId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    currentRoomIndex++;
                    addRoomSequential();
                },
                error -> {
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
                params.put("description", room.description);
                params.put("price", room.price);
                params.put("capacity", room.capacity);
                params.put("total_rooms", room.totalRooms);
                return params;
            }
        };


        queue.add(request);
    }

    private void uploadRoomImagesSequential(List<Uri> uris, int index, int bhrId) {
        if (index >= uris.size()) return;
        Uri imageUri = uris.get(index);

        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) baos.write(buffer, 0, read);
            String encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            inputStream.close();

            RequestQueue queue = Volley.newRequestQueue(getActivity());
            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_ROOM_IMAGE_URL,
                    response -> uploadRoomImagesSequential(uris, index + 1, bhrId),
                    error -> Toast.makeText(getActivity(), "Room image upload failed: " + error.getMessage(), Toast.LENGTH_SHORT).show()
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("bhr_id", String.valueOf(bhrId));
                    params.put("image_base64", encoded);
                    return params;
                }
            };
            queue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
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

}
