package com.example.mock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
    private boolean isRefreshingAfterEdit = false; // Flag to track refresh after edit
    private boolean isFirstLoad = true; // Flag to control loading dialog

    private TextView textViewListingCount;
    private ProgressDialog progressDialog;
    private LinearLayout layoutEmptyState;

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
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

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

            @Override
            public void onView(Listing listing) {
                // Open RoomViewActivity to view rooms
                Intent intent = new Intent(getContext(), RoomViewActivity.class);
                intent.putExtra("bh_id", listing.getBhId());
                intent.putExtra("bh_name", listing.getBhName());
                startActivity(intent);
            }
        });


        recyclerView.setAdapter(adapter);
        fetchBoardingHouses();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Don't show loading dialog when coming back from other activities
        fetchBoardingHouses(false); // refresh list when coming back
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            // Check if the boarding house was updated
            if (data != null && data.getBooleanExtra("updated", false)) {
                // Set flag and show loading message
                isRefreshingAfterEdit = true;
                Toast.makeText(getContext(), "Refreshing listings...", Toast.LENGTH_SHORT).show();
                fetchBoardingHouses(false); // Don't show loading dialog for refresh
            }
        }
    }


    private void fetchBoardingHouses() {
        fetchBoardingHouses(true); // Default to showing loading dialog
    }
    
    private void fetchBoardingHouses(boolean showLoading) {
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_boarding_houses.php";
        
        // Show loading dialog only on first load or when explicitly requested
        if (showLoading && isFirstLoad) {
            showProgressDialog();
        }

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
                        
                        // Show/hide empty state
                        if (count == 0) {
                            recyclerView.setVisibility(View.GONE);
                            layoutEmptyState.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            layoutEmptyState.setVisibility(View.GONE);
                        }
                        
                        // Show success message if this was a refresh after edit
                        if (isRefreshingAfterEdit) {
                            Toast.makeText(getContext(), "Listings updated successfully!", Toast.LENGTH_SHORT).show();
                            isRefreshingAfterEdit = false; // Reset flag
                        }
                        
                        // Mark first load as complete
                        if (isFirstLoad) {
                            isFirstLoad = false;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Parsing error", Toast.LENGTH_SHORT).show();
                    } finally {
                        // Hide loading dialog
                        hideProgressDialog();
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Error fetching data", Toast.LENGTH_SHORT).show();
                    // Hide loading dialog on error
                    hideProgressDialog();
                }) {

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
        // Show confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Boarding House")
                .setMessage("Are you sure you want to delete this boarding house? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User confirmed deletion
                    performDeleteBoardingHouse(bhId);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled, do nothing
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    private void performDeleteBoardingHouse(int bhId) {
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/delete_boarding_house.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        System.out.println("Delete response: " + response);
                        
                        // Check if response is HTML (PHP error)
                        if (response.trim().startsWith("<")) {
                            System.out.println("ERROR: Server returned HTML instead of JSON");
                            Toast.makeText(getContext(), "Server error. Check PHP file.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        // Check if response is empty
                        if (response.trim().isEmpty()) {
                            System.out.println("ERROR: Empty response from server");
                            Toast.makeText(getContext(), "Empty response from server", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Check if response contains success
                        if (response.contains("success")) {
                            Toast.makeText(getContext(), "Boarding house deleted successfully", Toast.LENGTH_SHORT).show();
                            fetchBoardingHouses(false); // Refresh the list without loading dialog
                        } else {
                            Toast.makeText(getContext(), "Failed to delete boarding house: " + response, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        System.out.println("ERROR: Exception processing delete response: " + e.getMessage());
                        Toast.makeText(getContext(), "Error processing delete response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bh_id", String.valueOf(bhId));
                return params;
            }
        };

        Volley.newRequestQueue(getContext()).add(request);
    }
    
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Loading boarding houses...");
            progressDialog.setCancelable(false);
        }
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }
    
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    
    // Public method to refresh data with loading dialog (can be called from outside)
    public void refreshListingsWithLoading() {
        isFirstLoad = true; // Reset flag to show loading dialog
        fetchBoardingHouses(true);
    }
}
