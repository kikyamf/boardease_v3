package com.example.mock;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TransactionsPagerAdapter extends FragmentStateAdapter {

    private final FragmentActivity fragmentActivity;

    public TransactionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragmentActivity = fragmentActivity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new ReservationsFragment();
                break;
            case 1:
                fragment = new PaymentsFragment();
                break;
            case 2:
                fragment = new RentalsFragment();
                break;
            case 3:
                fragment = new MaintenanceFragment();
                break;
            default:
                fragment = new ReservationsFragment();
        }
        
        // Pass user_id to all fragments if activity is TransactionsLogsActivity
        if (fragmentActivity instanceof TransactionsLogsActivity) {
            Bundle args = new Bundle();
            int userId = ((TransactionsLogsActivity) fragmentActivity).getUserId();
            args.putInt("user_id", userId);
            fragment.setArguments(args);
        }
        
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}


































