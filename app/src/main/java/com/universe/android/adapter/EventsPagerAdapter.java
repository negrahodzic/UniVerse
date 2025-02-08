package com.universe.android.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.universe.android.fragment.MyTicketsFragment;
import com.universe.android.fragment.UpcomingEventsFragment;

public class EventsPagerAdapter extends FragmentStateAdapter {

    public EventsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return position == 0 ? new UpcomingEventsFragment() : new MyTicketsFragment();
    }

    @Override
    public int getItemCount() {
        return 2; // Two tabs: Upcoming and My Tickets
    }

}