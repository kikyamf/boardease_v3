package com.example.mock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupChat extends AppCompatActivity {

    private ImageView backButton;
    private EditText groupNameEditText;
    private Button createGroupButton;
    private RecyclerView membersRecyclerView;
    private LinearLayout layoutEmptyMembers;
    private TextView selectedCountText;
    private RequestQueue requestQueue;
    private int currentUserId;
    private String currentUserType;
    private List<ProfileModel> availableMembers = new ArrayList<>();
    private List<ProfileModel> selectedMembers = new ArrayList<>();
    private MemberSelectionAdapter membersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_group_chat);

        // Adjust for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);

        // Get current user info from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        
        // Handle String type for user_id (stored as String in Login.java)
        String userIdString = sharedPreferences.getString("user_id", "1");
        try {
            currentUserId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            currentUserId = 1; // Default fallback
        }
        
        // Get user type from session
        currentUserType = sharedPreferences.getString("user_role", "Boarder");
        Log.d("CreateGroupChat", "Current user type: " + currentUserType);
        
        // Check if user is authorized to create group chats
        if (!"BH Owner".equals(currentUserType) && !"Owner".equals(currentUserType)) {
            Log.d("CreateGroupChat", "Access denied for boarder. Role: " + currentUserType);
            Toast.makeText(this, "Only boarding house owners can create group chats", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        Log.d("CreateGroupChat", "Access granted for owner. Role: " + currentUserType);

        // Initialize views
        backButton = findViewById(R.id.backButton);
        groupNameEditText = findViewById(R.id.groupNameEditText);
        createGroupButton = findViewById(R.id.createGroupButton);
        membersRecyclerView = findViewById(R.id.membersRecyclerView);
        layoutEmptyMembers = findViewById(R.id.layoutEmptyMembers);
        selectedCountText = findViewById(R.id.selectedCountText);

        // Back button action
        backButton.setOnClickListener(v -> finish());

        // Create group button action
        createGroupButton.setOnClickListener(v -> createGroupChat());

        // Add text change listener to group name EditText
        groupNameEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateCreateButtonState();
            }
        });

        // Setup RecyclerView
        setupRecyclerView();
        
        // Load real members from API
        loadAvailableMembers();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        membersRecyclerView.setLayoutManager(layoutManager);
        
        membersAdapter = new MemberSelectionAdapter(this, availableMembers);
        
        // Handle member selection
        membersAdapter.setOnItemClickListener(profile -> {
            if (selectedMembers.contains(profile)) {
                selectedMembers.remove(profile);
                profile.setSelected(false);
            } else {
                selectedMembers.add(profile);
                profile.setSelected(true);
            }
            membersAdapter.notifyDataSetChanged();
            updateCreateButtonState();
        });
        
        membersRecyclerView.setAdapter(membersAdapter);
    }

    private void loadAvailableMembers() {
        String url = "http://192.168.101.6/BoardEase2/get_users_for_messaging.php?current_user_id=" + currentUserId;
        Log.d("CreateGroupChat", "Loading members from: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("CreateGroupChat", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray usersArray = data.getJSONArray("users");
                            availableMembers.clear();
                            
                            if (usersArray.length() > 0) {
                                for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObj = usersArray.getJSONObject(i);
                                
                                // Get boarding house info if available
                                String boardingHouseName = "";
                                String boardingHouseAddress = "";
                                
                                if (userObj.has("boarding_house_name") && !userObj.isNull("boarding_house_name")) {
                                    boardingHouseName = userObj.getString("boarding_house_name");
                                }
                                if (userObj.has("boarding_house_address") && !userObj.isNull("boarding_house_address")) {
                                    boardingHouseAddress = userObj.getString("boarding_house_address");
                                }
                                
                                ProfileModel profile = new ProfileModel(
                                    userObj.getInt("user_id"),
                                    userObj.getString("full_name"),
                                    userObj.getString("user_type"),
                                    userObj.getString("email"),
                                    userObj.getString("phone"),
                                    R.drawable.ic_profile,
                                    userObj.getBoolean("has_device_token"),
                                    userObj.getBoolean("has_device_token") ? "Online" : "Offline",
                                    boardingHouseName,
                                    boardingHouseAddress
                                );
                                availableMembers.add(profile);
                                }
                            } else {
                                // Show empty state - no members found
                                Log.d("CreateGroupChat", "No members found for group chat creation");
                                Toast.makeText(this, "No members available for group chat creation", Toast.LENGTH_SHORT).show();
                            }
                            
                            membersAdapter.notifyDataSetChanged();
                            updateMembersEmptyState();
                        } else {
                            Toast.makeText(this, "Error loading members: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing members data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("CreateGroupChat", "Network error loading members", error);
                    Log.e("CreateGroupChat", "Error message: " + error.getMessage());
                    if (error.networkResponse != null) {
                        Log.e("CreateGroupChat", "Network response code: " + error.networkResponse.statusCode);
                        Log.e("CreateGroupChat", "Network response data: " + new String(error.networkResponse.data));
                    }
                    Toast.makeText(this, "Error loading members: " + (error.getMessage() != null ? error.getMessage() : "Network error"), Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void loadDummyMembers() {
        // Create dummy members for testing - with boarding house information
        availableMembers.clear();
        
        // Boarders with boarding house information
        availableMembers.add(new ProfileModel(2, "John Doe", "boarder", "john@example.com", "123-456-7890", R.drawable.ic_profile, true, "Online", "Sunset Boarding House", "123 Main St, Cebu City"));
        availableMembers.add(new ProfileModel(3, "Jane Smith", "boarder", "jane@example.com", "123-456-7891", R.drawable.ic_profile, false, "2 hours ago", "Mountain View Lodge", "456 Oak Ave, Cebu City"));
        availableMembers.add(new ProfileModel(5, "Sarah Wilson", "boarder", "sarah@example.com", "123-456-7893", R.drawable.ic_profile, false, "1 day ago", "Sunset Boarding House", "123 Main St, Cebu City"));
        availableMembers.add(new ProfileModel(7, "Lisa Garcia", "boarder", "lisa@example.com", "123-456-7895", R.drawable.ic_profile, true, "Online", "Ocean Breeze Inn", "789 Beach Rd, Cebu City"));
        availableMembers.add(new ProfileModel(9, "Maria Santos", "boarder", "maria@example.com", "123-456-7897", R.drawable.ic_profile, true, "Online", "Mountain View Lodge", "456 Oak Ave, Cebu City"));
        availableMembers.add(new ProfileModel(11, "Anna Lee", "boarder", "anna@example.com", "123-456-7899", R.drawable.ic_profile, true, "Online", "Sunset Boarding House", "123 Main St, Cebu City"));
        availableMembers.add(new ProfileModel(13, "Emily Davis", "boarder", "emily@example.com", "123-456-7901", R.drawable.ic_profile, true, "Online", "Ocean Breeze Inn", "789 Beach Rd, Cebu City"));
        availableMembers.add(new ProfileModel(15, "Jennifer White", "boarder", "jennifer@example.com", "123-456-7903", R.drawable.ic_profile, true, "Online", "Mountain View Lodge", "456 Oak Ave, Cebu City"));
        availableMembers.add(new ProfileModel(17, "Amanda Martinez", "boarder", "amanda@example.com", "123-456-7905", R.drawable.ic_profile, true, "Online", "Sunset Boarding House", "123 Main St, Cebu City"));
        availableMembers.add(new ProfileModel(19, "Jessica Thompson", "boarder", "jessica@example.com", "123-456-7907", R.drawable.ic_profile, true, "Online", "Ocean Breeze Inn", "789 Beach Rd, Cebu City"));
        
        // Boarding House Owners
        availableMembers.add(new ProfileModel(4, "Mike Johnson", "owner", "mike@example.com", "123-456-7892", R.drawable.ic_profile, true, "Online"));
        availableMembers.add(new ProfileModel(6, "David Brown", "owner", "david@example.com", "123-456-7894", R.drawable.ic_profile, true, "Online"));
        availableMembers.add(new ProfileModel(8, "Tom Wilson", "owner", "tom@example.com", "123-456-7896", R.drawable.ic_profile, false, "30 minutes ago"));
        availableMembers.add(new ProfileModel(10, "Carlos Rodriguez", "owner", "carlos@example.com", "123-456-7898", R.drawable.ic_profile, false, "1 hour ago"));
        availableMembers.add(new ProfileModel(12, "James Miller", "owner", "james@example.com", "123-456-7900", R.drawable.ic_profile, false, "3 hours ago"));
        availableMembers.add(new ProfileModel(14, "Robert Taylor", "owner", "robert@example.com", "123-456-7902", R.drawable.ic_profile, false, "2 days ago"));
        availableMembers.add(new ProfileModel(16, "Michael Clark", "owner", "michael@example.com", "123-456-7904", R.drawable.ic_profile, false, "4 hours ago"));
        availableMembers.add(new ProfileModel(18, "Christopher Lee", "owner", "chris@example.com", "123-456-7906", R.drawable.ic_profile, false, "1 hour ago"));
        availableMembers.add(new ProfileModel(20, "Daniel Anderson", "owner", "daniel@example.com", "123-456-7908", R.drawable.ic_profile, false, "5 hours ago"));
        
        membersAdapter.notifyDataSetChanged();
    }

    private void updateCreateButtonState() {
        boolean canCreate = !groupNameEditText.getText().toString().trim().isEmpty() && 
                           selectedMembers.size() >= 2; // At least 2 members + creator = 3 total
        
        createGroupButton.setEnabled(canCreate);
        createGroupButton.setAlpha(canCreate ? 1.0f : 0.5f);
        
        // Update selected count text
        if (selectedCountText != null) {
            selectedCountText.setText(selectedMembers.size() + " selected");
        }
    }

    private void createGroupChat() {
        String groupName = groupNameEditText.getText().toString().trim();
        
        if (groupName.isEmpty()) {
            Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedMembers.size() < 2) {
            Toast.makeText(this, "Please select at least 2 members", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        createGroupButton.setText("Creating...");
        createGroupButton.setEnabled(false);

        // Use create_group_chat.php endpoint
        String url = "http://192.168.101.6/BoardEase2/create_group_chat.php";
        
        // Prepare member IDs
        JSONArray memberIds = new JSONArray();
        for (ProfileModel member : selectedMembers) {
            memberIds.put(member.getUserId());
        }
        
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("group_name", groupName);
            jsonObject.put("created_by", currentUserId);
            jsonObject.put("member_ids", memberIds);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparing group data", Toast.LENGTH_SHORT).show();
            resetCreateButton();
            return;
        }

        android.util.Log.d("CreateGroup", "=== CREATING GROUP CHAT ===");
        android.util.Log.d("CreateGroup", "URL: " + url);
        android.util.Log.d("CreateGroup", "Group Name: " + groupName);
        android.util.Log.d("CreateGroup", "Created By: " + currentUserId);
        android.util.Log.d("CreateGroup", "Member IDs: " + memberIds.toString());
        android.util.Log.d("CreateGroup", "Request JSON: " + jsonObject.toString());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                response -> {
                    try {
                        android.util.Log.d("CreateGroup", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            int groupId = data.optInt("group_id", 0);
                            android.util.Log.d("CreateGroup", "Group created successfully with ID: " + groupId);
                            
                            Toast.makeText(this, "Group chat '" + groupName + "' created successfully!", Toast.LENGTH_SHORT).show();
                            
                            // Show selected members info
                            StringBuilder membersInfo = new StringBuilder("Members: ");
                            for (ProfileModel member : selectedMembers) {
                                membersInfo.append(member.getName()).append(", ");
                            }
                            membersInfo.append("You (Creator)");
                            
                            Toast.makeText(this, membersInfo.toString(), Toast.LENGTH_LONG).show();
                            
                            // Return to Messages activity with success result
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("group_created", true);
                            resultIntent.putExtra("group_name", groupName);
                            resultIntent.putExtra("group_id", groupId);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            android.util.Log.e("CreateGroup", "Failed to create group: " + response.getString("message"));
                            Toast.makeText(this, "Failed to create group: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                            resetCreateButton();
                        }
                    } catch (JSONException e) {
                        android.util.Log.e("CreateGroup", "JSON parsing error", e);
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetCreateButton();
                    }
                },
                error -> {
                    android.util.Log.e("CreateGroup", "Network error", error);
                    android.util.Log.e("CreateGroup", "Error details: " + error.getMessage());
                    if (error.networkResponse != null) {
                        android.util.Log.e("CreateGroup", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("CreateGroup", "Network response data: " + new String(error.networkResponse.data));
                    }
                    Toast.makeText(this, "Error creating group: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    resetCreateButton();
                });

        requestQueue.add(request);
    }
    
    private void resetCreateButton() {
        createGroupButton.setText("Create Group");
        createGroupButton.setEnabled(true);
    }
    
    private void updateMembersEmptyState() {
        if (availableMembers.isEmpty()) {
            membersRecyclerView.setVisibility(View.GONE);
            layoutEmptyMembers.setVisibility(View.VISIBLE);
        } else {
            membersRecyclerView.setVisibility(View.VISIBLE);
            layoutEmptyMembers.setVisibility(View.GONE);
        }
    }
}

































