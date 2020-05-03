package com.github.ui.adapters;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.github.R;
import com.github.ui.fragments.RecyclerViewFragment;

public class MainTabsPagerAdapter extends FragmentStatePagerAdapter {
    private static final int[] TAB_TITLES = {R.string.tab_chats, R.string.tab_contacts};
    private Context context;

    public MainTabsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        RecyclerViewFragment rvf = new RecyclerViewFragment(TAB_TITLES[position]);
        return rvf;
    }

    @Override
    public int getCount() {
        return TAB_TITLES.length;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return context.getResources().getString(TAB_TITLES[position]);
    }
}
