package com.example.mock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrivateRoomsFragment extends Fragment {

    private static final String ARG_BH_ID = "bh_id";
    private static final String GET_ROOMS_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_rooms.php";
    private static final String DELETE_ROOM_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/delete_room.php";
    private static final String GET_ROOM_UNITS_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_room_units.php";

    private int bhId;
    private RecyclerView recyclerView;
    private View emptyState;
    private PrivateRoomsAdapter adapter;
    private List<RoomData> roomList;

    public static PrivateRoomsFragment newInstance(int bhId) {
        PrivateRoomsFragment fragment = new PrivateRoomsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BH_ID, bhId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bhId = getArguments().getInt(ARG_BH_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_private_rooms, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        roomList = new ArrayList<>();
        adapter = new PrivateRoomsAdapter(roomList, this::onRoomClick, this::onEditRoom, this::onDeleteRoom);
        recyclerView.setAdapter(adapter);
        
        // Fetch rooms data
        fetchRooms();
        
        return view;
    }

    public void refreshData() {
        fetchRooms();
    }

    private void fetchRooms() {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        StringRequest request = new StringRequest(Request.Method.POST, GET_ROOMS_URL,
                response -> {
                    try {
                        System.out.println("DEBUG: Raw response from get_rooms.php: " + response);
                        System.out.println("DEBUG: Response length: " + response.length());
                        
                        // Check if response is empty or contains HTML
                        if (response.trim().isEmpty()) {
                            Toast.makeText(getContext(), "Empty response from server", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        if (response.trim().startsWith("<")) {
                            Toast.makeText(getContext(), "Server returned HTML instead of JSON. Check PHP errors.", Toast.LENGTH_LONG).show();
                            System.out.println("HTML Response: " + response);
                            return;
                        }
                        
                        JSONObject jsonResponse = new JSONObject(response);
                        System.out.println("DEBUG: Successfully parsed JSON");
                        
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray roomsArray = jsonResponse.getJSONArray("rooms");
                            System.out.println("DEBUG: Found " + roomsArray.length() + " rooms");
                            roomList.clear();
                            
                            for (int i = 0; i < roomsArray.length(); i++) {
                                JSONObject roomObj = roomsArray.getJSONObject(i);
                                
                                // Only add private rooms
                                String category = roomObj.getString("category");
                                System.out.println("DEBUG: Found room with category: '" + category + "'");
                                
                                if ("Private Room".equals(category)) {
                                    RoomData room = new RoomData();
                                    room.roomId = roomObj.getInt("bhr_id");
                                    room.bhId = roomObj.getInt("bh_id");
                                    room.category = category;
                                    room.title = roomObj.getString("title");
                                    // Debug the raw JSON object first
                                    System.out.println("DEBUG: Raw room JSON object: " + roomObj.toString());
                                    System.out.println("DEBUG: Has room_description key? " + roomObj.has("room_description"));
                                    System.out.println("DEBUG: Has description key? " + roomObj.has("description"));
                                    
                                    // Try both possible keys
                                    if (roomObj.has("room_description")) {
                                        room.description = roomObj.optString("room_description", "");
                                        System.out.println("DEBUG: Using room_description: '" + room.description + "'");
                                    } else if (roomObj.has("description")) {
                                        room.description = roomObj.optString("description", "");
                                        System.out.println("DEBUG: Using description: '" + room.description + "'");
                                    } else {
                                        room.description = "";
                                        System.out.println("DEBUG: No description field found, using empty string");
                                    }
                                    
                                    // Debug logging
                                    System.out.println("DEBUG: Parsed room data:");
                                    System.out.println("DEBUG: roomId = " + room.roomId);
                                    System.out.println("DEBUG: title = " + room.title);
                                    System.out.println("DEBUG: final description = '" + room.description + "'");
                                    
                                    // Handle different data types for price, capacity, total_rooms
                                    if (roomObj.has("price")) {
                                        room.price = String.valueOf(roomObj.getDouble("price"));
                                    } else {
                                        room.price = "0";
                                    }
                                    
                                    if (roomObj.has("capacity")) {
                                        room.capacity = String.valueOf(roomObj.getInt("capacity"));
                                    } else {
                                        room.capacity = "0";
                                    }
                                    
                                    if (roomObj.has("total_rooms")) {
                                        room.totalRooms = String.valueOf(roomObj.getInt("total_rooms"));
                                    } else {
                                        room.totalRooms = "0";
                                    }
                                    
                                    // Get images
                                    if (roomObj.has("images") && !roomObj.isNull("images")) {
                                        JSONArray imagesArray = roomObj.getJSONArray("images");
                                        room.imagePaths = new ArrayList<>();
                                        for (int j = 0; j < imagesArray.length(); j++) {
                                            room.imagePaths.add(imagesArray.getString(j));
                                        }
                                    }
                                    
                                    roomList.add(room);
                                    System.out.println("DEBUG: Added room: " + room.title);
                                }
                            }
                            
                            adapter.notifyDataSetChanged();
                            System.out.println("DEBUG: Total private rooms loaded: " + roomList.size());
                            
                            // Show/hide empty state
                            updateEmptyState();
                        } else {
                            String errorMsg = jsonResponse.optString("error", "Unknown error");
                            Toast.makeText(getContext(), "Failed to load rooms: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        System.out.println("DEBUG: JSON parsing error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error parsing room data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    System.out.println("DEBUG: Network error: " + error.getMessage());
                    Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bh_id", String.valueOf(bhId));
                System.out.println("DEBUG: Sending bh_id: " + bhId);
                return params;
            }
        };
        
        queue.add(request);
    }

    private void updateEmptyState() {
        if (roomList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void onRoomClick(RoomData room) {
        // Show room details dialog
        showRoomDetailsDialog(room);
    }

    private void onEditRoom(RoomData room) {
        // Debug logging
        System.out.println("DEBUG: onEditRoom called with:");
        System.out.println("DEBUG: roomId = " + room.roomId);
        System.out.println("DEBUG: title = " + room.title);
        System.out.println("DEBUG: description = '" + room.description + "'");
        System.out.println("DEBUG: price = " + room.price);
        System.out.println("DEBUG: capacity = " + room.capacity);
        System.out.println("DEBUG: total_rooms = " + room.totalRooms);
        
        // Navigate to edit room activity
        Intent intent = new Intent(getContext(), EditRoomActivity.class);
        intent.putExtra("room_id", room.roomId);
        intent.putExtra("bh_id", room.bhId);
        intent.putExtra("category", room.category);
        intent.putExtra("title", room.title);
        intent.putExtra("description", room.description);
        intent.putExtra("price", room.price);
        intent.putExtra("capacity", room.capacity);
        intent.putExtra("total_rooms", room.totalRooms);
        startActivity(intent);
    }

    private void onDeleteRoom(RoomData room) {
        // Show confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Room")
                .setMessage("Are you sure you want to delete this room?\n\n" + room.title)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteRoom(room.roomId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRoom(int roomId) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        StringRequest request = new StringRequest(Request.Method.POST, DELETE_ROOM_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(getContext(), "Room deleted successfully", Toast.LENGTH_SHORT).show();
                            fetchRooms(); // Refresh the list
                        } else {
                            Toast.makeText(getContext(), "Failed to delete room: " + jsonResponse.getString("error"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing delete response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("room_id", String.valueOf(roomId));
                return params;
            }
        };
        
        queue.add(request);
    }

    private void showRoomDetailsDialog(RoomData room) {
        // Create custom dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_room_units, null);
        
        TextView tvRoomTitle = dialogView.findViewById(R.id.tvRoomTitle);
        TextView tvRoomInfo = dialogView.findViewById(R.id.tvRoomInfo);
        RecyclerView recyclerViewUnits = dialogView.findViewById(R.id.recyclerViewUnits);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView tvNoUnits = dialogView.findViewById(R.id.tvNoUnits);
        
        // Set room title and info
        tvRoomTitle.setText(room.title);
        tvRoomInfo.setText("Price: ₱" + room.price + " • Capacity: " + room.capacity + " person(s)");
        
        // Setup RecyclerView
        recyclerViewUnits.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewUnits.setVisibility(View.GONE);
        tvNoUnits.setVisibility(View.GONE);
        
        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .create();
        
        // Fetch room units
        fetchRoomUnits(room.roomId, recyclerViewUnits, progressBar, tvNoUnits);
        
        dialog.show();
    }
    
    private void fetchRoomUnits(int roomId, RecyclerView recyclerView, ProgressBar progressBar, TextView tvNoUnits) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        StringRequest request = new StringRequest(Request.Method.POST, GET_ROOM_UNITS_URL,
                response -> {
                    try {
                        System.out.println("DEBUG: Room units response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        
                        progressBar.setVisibility(View.GONE);
                        
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray unitsArray = jsonResponse.getJSONArray("units");
                            List<RoomUnitsAdapter.RoomUnit> unitList = new ArrayList<>();
                            
                            for (int i = 0; i < unitsArray.length(); i++) {
                                JSONObject unitObj = unitsArray.getJSONObject(i);
                                RoomUnitsAdapter.RoomUnit unit = new RoomUnitsAdapter.RoomUnit();
                                unit.roomNumber = unitObj.getString("room_number");
                                unit.status = unitObj.getString("status");
                                unitList.add(unit);
                            }
                            
                            if (unitList.isEmpty()) {
                                tvNoUnits.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                RoomUnitsAdapter adapter = new RoomUnitsAdapter(unitList);
                                recyclerView.setAdapter(adapter);
                                recyclerView.setVisibility(View.VISIBLE);
                                tvNoUnits.setVisibility(View.GONE);
                            }
                        } else {
                            String errorMsg = jsonResponse.optString("error", "Failed to load room units");
                            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                            tvNoUnits.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error parsing room units data", Toast.LENGTH_SHORT).show();
                        tvNoUnits.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                },
                error -> {
                    System.out.println("DEBUG: Network error fetching room units: " + error.getMessage());
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoUnits.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bhr_id", String.valueOf(roomId));
                System.out.println("DEBUG: Sending bhr_id for units: " + roomId);
                return params;
            }
        };
        
        queue.add(request);
    }

    // Data class for room information
    public static class RoomData {
        public int roomId;
        public int bhId;
        public String category;
        public String title;
        public String description;
        public String price;
        public String capacity;
        public String totalRooms;
        public List<String> imagePaths;
    }
}










