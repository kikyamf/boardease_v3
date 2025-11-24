package com.example.mock;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

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
            // Get all fragments from the fragment manager and refresh matching ones
            List<Fragment> fragments = fragmentActivity.getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isAdded()) {
                    if (fragment instanceof PrivateRoomsFragment) {
                        ((PrivateRoomsFragment) fragment).refreshData();
                    } else if (fragment instanceof BedSpacersFragment) {
                        ((BedSpacersFragment) fragment).refreshData();
                    }
                }
            }
            
            // Also try to find fragments by ViewPager2's tag format as fallback
            // ViewPager2 uses tag format: "f" + getItemId(position)
            for (int i = 0; i < getItemCount(); i++) {
                long itemId = getItemId(i);
                Fragment fragment = fragmentActivity.getSupportFragmentManager()
                        .findFragmentByTag("f" + itemId);
                
                if (fragment != null && fragment.isAdded()) {
                    if (fragment instanceof PrivateRoomsFragment) {
                        ((PrivateRoomsFragment) fragment).refreshData();
                    } else if (fragment instanceof BedSpacersFragment) {
                        ((BedSpacersFragment) fragment).refreshData();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: recreate fragments
            notifyDataSetChanged();
        }
    }
}








