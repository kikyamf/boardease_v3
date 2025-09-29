package com.example.mock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageFragment extends Fragment {

    private RecyclerView recyclerView;
    private ListingAdapter adapter;
    private List<Listing> listingList = new ArrayList<>();
    private int userId = -1; // now will be set via arguments

    private TextView textViewListingCount;

    private static final String ARG_USER_ID = "user_id";

    public ManageFragment() {
        // Required empty public constructor
    }

    // Factory method
    public static ManageFragment newInstance(int userId) {
        ManageFragment fragment = new ManageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID, -1);
        }
        if (userId == -1) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_manage, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewListings);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        textViewListingCount = view.findViewById(R.id.tvListingCount);

        adapter = new ListingAdapter(getContext(), listingList, new ListingAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Listing listing) {
                // Open EditBoardingHouseActivity with basic data - it will fetch complete details
                Intent intent = new Intent(getContext(), EditBoardingHouseActivity.class);
                intent.putExtra("bh_id", listing.getBhId());
                intent.putExtra("bh_name", listing.getBhName());
                intent.putExtra("image_path", listing.getImagePath());
                startActivityForResult(intent, 1001); // Use startActivityForResult to handle updates
            }

            @Override
            public void onDelete(Listing listing) {
                deleteBoardingHouse(listing.getBhId());
            }
        });


        recyclerView.setAdapter(adapter);
        fetchBoardingHouses();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchBoardingHouses(); // refresh list when coming back
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            // Check if the boarding house was updated
            if (data != null && data.getBooleanExtra("updated", false)) {
                // Refresh the list to show updated data
                fetchBoardingHouses();
                Toast.makeText(getContext(), "List refreshed with updated data", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void fetchBoardingHouses() {
        String url = "http://192.168.101.6/BoardEase2/get_boarding_houses.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        listingList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            int bhId = obj.getInt("bh_id");
                            String name = obj.getString("bh_name");
                            String imagePath = obj.optString("image_path", "");

                            // Create image paths list
                            ArrayList<String> imagePaths = new ArrayList<>();
                            if (!imagePath.isEmpty()) {
                                imagePaths.add(imagePath);
                            }

                            // Use the simple constructor since we only have basic data from this API
                            listingList.add(new Listing(bhId, name, imagePath));
                        }
                        adapter.notifyDataSetChanged();

                        int count = listingList.size();
                        if (count == 1) {
                            textViewListingCount.setText("You have (" + count + ") listing");
                        } else {
                            textViewListingCount.setText("You have (" + count + ") listings");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Parsing error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Error fetching data", Toast.LENGTH_SHORT).show()) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }
        };

        Volley.newRequestQueue(getContext()).add(request);
    }

    private void deleteBoardingHouse(int bhId) {
        String url = "http://192.168.101.6/BoardEase2/delete_boarding_house.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                    fetchBoardingHouses();
                },
                error -> Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show()) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bh_id", String.valueOf(bhId));
                return params;
            }
        };

        Volley.newRequestQueue(getContext()).add(request);
    }
}
