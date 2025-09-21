package com.example.mock;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddingBhFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddingBhFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AddingBhFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AddingBhFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddingBhFragment newInstance(String param1, String param2) {
        AddingBhFragment fragment = new AddingBhFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adding_bh, container, false);

        // Get reference to button
        Button btnCreate = view.findViewById(R.id.btnCreate);

        btnCreate.setOnClickListener(v -> {
            // Replace current fragment with AddRoomsFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AddingRoomsFragment()) // fragment_container is the FrameLayout in activity_main.xml
                    .addToBackStack(null) // so that back button works
                    .commit();
        });

        return view;
    }

}