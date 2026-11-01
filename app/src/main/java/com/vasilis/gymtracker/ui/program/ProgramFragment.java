package com.vasilis.gymtracker.ui.program;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.vasilis.gymtracker.R;
import com.vasilis.gymtracker.util.DateUtils;

/**
 * "Program" section: Monday–Sunday tabs, with a ViewPager2 below
 * that shows the DayFragment of the selected day.
 */
public class ProgramFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_program, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager);

        // "this": the adapter belongs to this fragment (it uses the child fragment manager).
        viewPager.setAdapter(new DayPagerAdapter(this));

        String[] dayNames = getResources().getStringArray(R.array.day_names);
        // TabLayoutMediator links the tabs to the pages:
        // tapping a tab changes the page, swiping a page changes the tab.
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(dayNames[position])
        ).attach();

        // Start on today's day.
        if (savedInstanceState == null) {
            viewPager.setCurrentItem(DateUtils.todayIndex(), false);
        }
    }
}
