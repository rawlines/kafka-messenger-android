package com.github.ui.adapters;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.github.R;
import com.github.activities.MainActivity;
import com.github.fragments.RecyclerViewFragment;

public class MainTabsPagerAdapter extends FragmentStatePagerAdapter {
    private static final int[] TAB_TITLES = {R.string.tab_chats, R.string.tab_contacts};
    private MainActivity mainActivity;

    public MainTabsPagerAdapter(MainActivity mainActivity, FragmentManager fm) {
        super(fm);
        this.mainActivity = mainActivity;
    }

    @Override
    public Fragment getItem(int position) {
        return new RecyclerViewFragment(TAB_TITLES[position], mainActivity);
    }

    @Override
    public int getCount() {
        return TAB_TITLES.length;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mainActivity.getResources().getString(TAB_TITLES[position]);
    }
}
