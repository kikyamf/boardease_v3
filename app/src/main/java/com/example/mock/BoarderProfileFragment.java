package com.example.mock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * BoarderProfileFragment - Profile page for boarders
 * Displays user information and provides access to various settings and features
 */
public class BoarderProfileFragment extends Fragment {

    // Views
    private ImageButton btnBack;
    private ImageView ivProfilePic;
    private ImageView ivEditProfile;
    private TextView tvBoarderName;
    private TextView tvBoarderEmail;
    private TextView tvSignOut;

    // Menu Items
    private LinearLayout layoutAccountSettings;
    private LinearLayout layoutMyBookings;
    private LinearLayout layoutMyFavorites;
    private LinearLayout layoutPaymentMethods;
    private LinearLayout layoutNotifications;
    private LinearLayout layoutMessages;
    private LinearLayout layoutHelpSupport;
    private LinearLayout layoutAboutApp;

    public BoarderProfileFragment() {
        // Required empty public constructor
    }

    public static BoarderProfileFragment newInstance() {
        return new BoarderProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boarder_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews(View view) {
        try {
            // Header views
            btnBack = view.findViewById(R.id.btnBack);
            
            // Profile views
            ivProfilePic = view.findViewById(R.id.ivProfilePic);
            ivEditProfile = view.findViewById(R.id.ivEditProfile);
            tvBoarderName = view.findViewById(R.id.tvBoarderName);
            tvBoarderEmail = view.findViewById(R.id.tvBoarderEmail);
            
            // Menu items
            layoutAccountSettings = view.findViewById(R.id.layoutAccountSettings);
            layoutMyBookings = view.findViewById(R.id.layoutMyBookings);
            layoutMyFavorites = view.findViewById(R.id.layoutMyFavorites);
            layoutPaymentMethods = view.findViewById(R.id.layoutPaymentMethods);
            layoutNotifications = view.findViewById(R.id.layoutNotifications);
            layoutMessages = view.findViewById(R.id.layoutMessages);
            layoutHelpSupport = view.findViewById(R.id.layoutHelpSupport);
            layoutAboutApp = view.findViewById(R.id.layoutAboutApp);
            
            // Sign out
            tvSignOut = view.findViewById(R.id.tvSignOut);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupClickListeners() {
        try {
            // Back button
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    try {
                        // Navigate back or close profile
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Edit profile picture
            if (ivEditProfile != null) {
                ivEditProfile.setOnClickListener(v -> {
                    try {
                        Toast.makeText(getContext(), "Edit Profile Picture - Coming Soon!", Toast.LENGTH_SHORT).show();
                        // TODO: Implement profile picture editing
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Account Settings
            if (layoutAccountSettings != null) {
                layoutAccountSettings.setOnClickListener(v -> {
                    try {
                        // Navigate to account settings fragment
                        if (getActivity() != null) {
                            BoarderAccountSettingsFragment accountSettingsFragment = BoarderAccountSettingsFragment.newInstance();
                            getActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, accountSettingsFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // My Bookings
            if (layoutMyBookings != null) {
                layoutMyBookings.setOnClickListener(v -> {
                    try {
                        // Navigate to bookings fragment
                        if (getActivity() instanceof BoarderDashboard) {
                            BoarderDashboard dashboard = (BoarderDashboard) getActivity();
                            dashboard.switchToTab(R.id.nav_activity);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // My Favorites
            if (layoutMyFavorites != null) {
                layoutMyFavorites.setOnClickListener(v -> {
                    try {
                        // Navigate to favorites fragment
                        if (getActivity() instanceof BoarderDashboard) {
                            BoarderDashboard dashboard = (BoarderDashboard) getActivity();
                            dashboard.switchToTab(R.id.nav_manage);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Payment Methods
            if (layoutPaymentMethods != null) {
                layoutPaymentMethods.setOnClickListener(v -> {
                    try {
                        Toast.makeText(getContext(), "Payment Methods - Coming Soon!", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to payment methods
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Notifications
            if (layoutNotifications != null) {
                layoutNotifications.setOnClickListener(v -> {
                    try {
                        Toast.makeText(getContext(), "Notifications - Coming Soon!", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to notifications settings
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Messages
            if (layoutMessages != null) {
                layoutMessages.setOnClickListener(v -> {
                    try {
                        Toast.makeText(getContext(), "Messages - Coming Soon!", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to messages
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Help & Support
            if (layoutHelpSupport != null) {
                layoutHelpSupport.setOnClickListener(v -> {
                    try {
                        Toast.makeText(getContext(), "Help & Support - Coming Soon!", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to help & support
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // About App
            if (layoutAboutApp != null) {
                layoutAboutApp.setOnClickListener(v -> {
                    try {
                        showAboutAppDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Sign Out
            if (tvSignOut != null) {
                tvSignOut.setOnClickListener(v -> {
                    try {
                        showSignOutConfirmationDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserData() {
        try {
            // Load user data from SharedPreferences or API
            // For now, using mock data
            if (tvBoarderName != null) {
                tvBoarderName.setText("John Doe"); // Mock name
            }
            if (tvBoarderEmail != null) {
                tvBoarderEmail.setText("john.doe@email.com"); // Mock email
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAboutAppDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("About BoardEase");
            builder.setMessage("BoardEase v1.0.0\n\n" +
                    "A comprehensive platform for finding and managing boarding house accommodations.\n\n" +
                    "Features:\n" +
                    "• Browse boarding houses\n" +
                    "• Book accommodations\n" +
                    "• Manage favorites\n" +
                    "• Track bookings\n\n" +
                    "© 2024 BoardEase. All rights reserved.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSignOutConfirmationDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Sign Out");
            builder.setMessage("Are you sure you want to sign out?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO: Implement sign out functionality
                    Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
