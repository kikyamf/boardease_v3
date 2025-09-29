package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mock.adapters.CarouselAdapter;

//import me.relex.circleindicator.CircleIndicator3;

public class WelcomeActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button nextButton;
//    private CircleIndicator3 indicator; // Instance variable

    private int[] images = {R.drawable.carousel3, R.drawable.carousel2, R.drawable.carousel1};
    private String[] titles = {"Welcome!", "Explore Features", "Get Started"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        viewPager = findViewById(R.id.viewPager);
        nextButton = findViewById(R.id.nextButton);

        CarouselAdapter adapter = new CarouselAdapter(images, titles);
        viewPager.setAdapter(adapter);


        nextButton.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            // Using adapter.getItemCount() is safer than images.length
            // if the adapter is the source of truth for page count.
            if (adapter != null && current < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                startActivity(new Intent(WelcomeActivity.this, Login.class));
                finish();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Using adapter.getItemCount() is safer
                if (adapter != null && position == adapter.getItemCount() - 1) {
                    nextButton.setText("Get Started");
                } else {
                    nextButton.setText("Next");
                }
            }
        });
    }
}
