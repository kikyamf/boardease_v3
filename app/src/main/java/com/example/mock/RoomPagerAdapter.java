package com.example.mock;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class RoomPagerAdapter extends FragmentStateAdapter {

    private int bhId;
    private FragmentActivity fragmentActivity;

    public RoomPagerAdapter(@NonNull FragmentActivity fragmentActivity, int bhId) {
        super(fragmentActivity);
        this.bhId = bhId;
        this.fragmentActivity = fragmentActivity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return PrivateRoomsFragment.newInstance(bhId);
            case 1:
                return BedSpacersFragment.newInstance(bhId);
            default:
                return PrivateRoomsFragment.newInstance(bhId);
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public void refreshData() {
        // Refresh both fragments by calling their refreshData methods
        try {
            // Get the current fragments and call their refreshData methods
            Fragment privateRoomsFragment = fragmentActivity.getSupportFragmentManager()
                    .findFragmentByTag("f" + 0);
            Fragment bedSpacersFragment = fragmentActivity.getSupportFragmentManager()
                    .findFragmentByTag("f" + 1);
            
            if (privateRoomsFragment instanceof PrivateRoomsFragment) {
                ((PrivateRoomsFragment) privateRoomsFragment).refreshData();
            }
            if (bedSpacersFragment instanceof BedSpacersFragment) {
                ((BedSpacersFragment) bedSpacersFragment).refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: recreate fragments
            notifyDataSetChanged();
        }
    }
}
