package com.vasilis.gymtracker.ui.program;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter for the ViewPager2: 7 pages, one per day of the week.
 */
public class DayPagerAdapter extends FragmentStateAdapter {

    private static final int DAYS_IN_WEEK = 7;

    public DayPagerAdapter(@NonNull Fragment parent) {
        super(parent);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Position 0 = Monday, 6 = Sunday.
        return DayFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return DAYS_IN_WEEK;
    }
}
