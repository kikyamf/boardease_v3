package com.example.mock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddingBhFragment extends Fragment {

    private static final int IMAGE_PICK_CODE = 1001;

    private EditText etBhName, etBhAddress, etBhDescription, etBhRules, etBathrooms, etArea, etBuildYear;
    private Button btnCreate, btnAddImage;
    private ViewPager2 viewPagerImages;

    private ArrayList<Uri> imageUris = new ArrayList<>();
    private int createdBhId = -1;
    private int userId = -1;

    private ImageAdapter imageAdapter;

    private final String ADD_BH_URL = "https://yourdomain.com/add_boarding_house.php";
    private final String UPLOAD_IMAGE_URL = "https://yourdomain.com/upload_bh_image.php";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt("user_id", -1);
        }
        if (userId == -1) {
            Toast.makeText(getActivity(), "User not logged in!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adding_bh, container, false);

        // Initialize views
        etBhName = view.findViewById(R.id.etTitle);
        etBhAddress = view.findViewById(R.id.etAddress);
        etBhDescription = view.findViewById(R.id.etDescription);
        etBhRules = view.findViewById(R.id.etRules);
        etBathrooms = view.findViewById(R.id.etBathrooms);
        etArea = view.findViewById(R.id.etArea);
        etBuildYear = view.findViewById(R.id.etBuildYear);
        btnCreate = view.findViewById(R.id.btnCreate);
        btnAddImage = view.findViewById(R.id.btnAddImage);
        viewPagerImages = view.findViewById(R.id.viewPagerImages);

        // Set up adapter for image previews
        imageAdapter = new ImageAdapter(getActivity(), imageUris, position -> {
            imageUris.remove(position);
            imageAdapter.notifyDataSetChanged();
        });
        viewPagerImages.setAdapter(imageAdapter);

        // Add image button using native gallery picker
        btnAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, IMAGE_PICK_CODE);
        });

        // Create Boarding House button
        btnCreate.setOnClickListener(v -> {
            String name = etBhName.getText().toString().trim();
            String address = etBhAddress.getText().toString().trim();
            String description = etBhDescription.getText().toString().trim();
            String rules = etBhRules.getText().toString().trim();
            String bathrooms = etBathrooms.getText().toString().trim();
            String area = etArea.getText().toString().trim();
            String buildYear = etBuildYear.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(address) || TextUtils.isEmpty(bathrooms)) {
                Toast.makeText(getActivity(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            createBoardingHouse(name, address, description, rules, bathrooms, area, buildYear);
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && requestCode == IMAGE_PICK_CODE) {
            // Handle multiple selection
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    getActivity().getContentResolver().takePersistableUriPermission(
                            imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    imageUris.add(imageUri);
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                getActivity().getContentResolver().takePersistableUriPermission(
                        imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                imageUris.add(imageUri);
            }
            imageAdapter.notifyDataSetChanged();
            Toast.makeText(getActivity(), "Image(s) added", Toast.LENGTH_SHORT).show();
        }
    }

    private void createBoardingHouse(String name, String address, String description, String rules,
                                     String bathrooms, String area, String buildYear) {

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        StringRequest request = new StringRequest(Request.Method.POST, ADD_BH_URL,
                response -> {
                    if (response.contains("success:")) {
                        createdBhId = Integer.parseInt(response.replace("success:", ""));
                        Toast.makeText(getActivity(), "Boarding House Created", Toast.LENGTH_SHORT).show();

                        // Upload images
                        if (!imageUris.isEmpty()) {
                            for (Uri uri : imageUris) {
                                uploadImage(uri, createdBhId);
                            }
                        }

                        // Navigate to AddingRoomsFragment
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new AddingRoomsFragment())
                                .addToBackStack(null)
                                .commit();

                    } else {
                        Toast.makeText(getActivity(), "Error: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getActivity(), "Volley Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("bh_name", name);
                params.put("bh_address", address);
                params.put("bh_description", description);
                params.put("bh_rules", rules);
                params.put("number_of_bathroom", bathrooms);
                params.put("area", area);
                params.put("build_year", buildYear);
                params.put("status", "Active");
                return params;
            }
        };

        queue.add(request);
    }

    private void uploadImage(Uri imageUri, int bhId) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            inputStream.close();

            RequestQueue queue = Volley.newRequestQueue(getActivity());

            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_IMAGE_URL,
                    response -> Toast.makeText(getActivity(), "Image uploaded", Toast.LENGTH_SHORT).show(),
                    error -> Toast.makeText(getActivity(), "Image upload failed: " + error.getMessage(), Toast.LENGTH_SHORT).show()
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("bh_id", String.valueOf(bhId));
                    params.put("image_base64", encodedImage);
                    return params;
                }
            };
            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Image processing failed", Toast.LENGTH_SHORT).show();
        }
    }
}
