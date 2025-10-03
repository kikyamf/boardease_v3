package com.example.mock;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class OwnerHomeFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private int userId = -1; // default to -1

    // UI elements
    private TextView tvOwnerName, tvListingsCount, tvBoardersCount, tvViewsCount, tvPopularTitle, tvPopularVisits;
    private ImageView imgPopularListing, ivNotification, ivMessage;
    private LinearLayout numofListings, layoutTotalBoarders;

    public OwnerHomeFragment() {
        // Required empty public constructor
    }

    public static OwnerHomeFragment newInstance(int userId) {
        OwnerHomeFragment fragment = new OwnerHomeFragment();
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
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_home, container, false);

        // Bind views
        tvOwnerName = view.findViewById(R.id.tvOwnerName);
        tvListingsCount = view.findViewById(R.id.tvListingsCount);
        tvBoardersCount = view.findViewById(R.id.tvBoardersCount);
        tvViewsCount = view.findViewById(R.id.tvViewsCount);
        tvPopularTitle = view.findViewById(R.id.tvPopularTitle);
        tvPopularVisits = view.findViewById(R.id.tvPopularVisits);
        imgPopularListing = view.findViewById(R.id.imgPopularListing);
        numofListings = view.findViewById(R.id.noofListings);
        layoutTotalBoarders = view.findViewById(R.id.layoutTotalBoarders);
        ivNotification = view.findViewById(R.id.ivNotification);
        ivMessage = view.findViewById(R.id.ivMessage);

        ivNotification.setOnClickListener(v -> {
            if (getContext() != null) { // or getActivity() if inside a fragment
                Intent intent = new Intent(getContext(), Notification.class);
                startActivity(intent);
            }
        });

        ivMessage.setOnClickListener(v -> {
            if (getContext() != null) { // or getActivity() if inside a fragment
                Intent intent = new Intent(getContext(), Messages.class);
                startActivity(intent);
            }
        });

        // Transactions & Logs button click listener
        view.findViewById(R.id.cardTransactionsLogs).setOnClickListener(v -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), TransactionsLogsActivity.class);
                startActivity(intent);
            }
        });


        // Click listener for LinearLayout to navigate to ManageFragment
        numofListings.setOnClickListener(v -> {
            // 1️⃣ Replace fragment
            ManageFragment manageFragment = ManageFragment.newInstance(userId);
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, manageFragment)
                        .addToBackStack(null)
                        .commit();

                // 2️⃣ Update BottomNavigationView selection
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_manage); // highlight Manage icon
                }
            }
        });

        // Click listener for Total Boarders to navigate to BoardersListActivity
        layoutTotalBoarders.setOnClickListener(v -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), BoardersListActivity.class);
                intent.putExtra("user_id", userId);
                startActivity(intent);
            }
        });



        if (userId != -1) {
            fetchOwnerDashboardData();
        } else {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void fetchOwnerDashboardData() {
        String url = "http://192.168.254.121/BoardEase2/get_owner_dashboard.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("OwnerHomeFragment", "Server Response: " + response);

                    if (response == null || response.trim().isEmpty()) {
                        Toast.makeText(getContext(), "Empty response from server", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject obj = new JSONObject(response);

                        // Validate JSON content
                        if (!obj.has("owner_name")) {
                            Toast.makeText(getContext(), "Invalid server response", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Owner name
                        String ownerName = obj.optString("owner_name", "Owner");
                        tvOwnerName.setText(ownerName);

                        // Listings count
                        int listingsCount = obj.optInt("listings_count", 0);
                        tvListingsCount.setText(String.valueOf(listingsCount));

                        // Boarders count
                        int boardersCount = obj.optInt("boarders_count", 0);
                        tvBoardersCount.setText(String.valueOf(boardersCount));

                        // Views count
                        int viewsCount = obj.optInt("views_count", 0);
                        tvViewsCount.setText(String.valueOf(viewsCount));

                        // Popular listing
                        JSONObject popular = obj.optJSONObject("popular_listing");
                        if (popular != null) {
                            tvPopularTitle.setText(popular.optString("bh_name", "No Listing"));
                            tvPopularVisits.setText(popular.optInt("visits", 0) + " visits");

                            String imageUrl = popular.optString("image_path", "");
                            if (!imageUrl.isEmpty()) {
                                Glide.with(getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.sample_listing)
                                        .error(R.drawable.sample_listing)
                                        .into(imgPopularListing);
                            } else {
                                imgPopularListing.setImageResource(R.drawable.sample_listing);
                            }
                        } else {
                            tvPopularTitle.setText("No Popular Listing");
                            tvPopularVisits.setText("0 visits");
                            imgPopularListing.setImageResource(R.drawable.sample_listing);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "JSON Parsing error", Toast.LENGTH_SHORT).show();
                        Log.e("OwnerHomeFragment", "JSON Parse Error: " + e.getMessage());
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Error fetching dashboard", Toast.LENGTH_SHORT).show();
                    Log.e("OwnerHomeFragment", "Volley Error: " + error.getMessage(), error);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                Log.d("OwnerHomeFragment", "Sending user_id: " + userId);
                return params;
            }
        };

        Volley.newRequestQueue(getContext()).add(request);
    }
}
