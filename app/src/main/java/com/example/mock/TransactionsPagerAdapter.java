package com.example.mock;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TransactionsPagerAdapter extends FragmentStateAdapter {

    public TransactionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ReservationsFragment();
            case 1:
                return new PaymentsFragment();
            case 2:
                return new RentalsFragment();
            case 3:
                return new MaintenanceFragment();
            default:
                return new ReservationsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}

































