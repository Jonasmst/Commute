package com.androiddev.jonas.commute;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Window;

public class CommuteActivity extends FragmentActivity {

    // Tabs stuff
    ViewPager mPager;
    SlidingTabLayout mTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.slidingtabs_test);


        // Tabs
        mPager = (ViewPager) findViewById(R.id.tabsPager);
        mPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
        mTabs = (SlidingTabLayout) findViewById(R.id.tabsLayout);
        mTabs.setViewPager(mPager);

        // Do stuff
        loadFragments();

    }

    // Load up all fragments
    public void loadFragments() {
        FragmentManager fragManager = getSupportFragmentManager();

        // Weather fragment
        FragmentTransaction fragTrans = fragManager.beginTransaction();
        WeatherFragment weather = new WeatherFragment();
        fragTrans.add(R.id.Main_WeatherContainer, weather, "WEATHER");
        fragTrans.commit();
    }


    /* Adapter for tabs */
    public static class MyPagerAdapter extends FragmentPagerAdapter {

        String[] tabs;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
            tabs = new String[]{"Forskningsparken", "Radiumhospitalet"};
        }

        @Override
        public Fragment getItem(int position) {

            TransportFragment frag = TransportFragment.getInstance(position);
            return frag;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabs[position];
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

}



