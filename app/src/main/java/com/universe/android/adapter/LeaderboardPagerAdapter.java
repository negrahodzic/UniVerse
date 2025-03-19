package com.universe.android.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.universe.android.fragment.FriendsLeaderboardFragment;
import com.universe.android.fragment.GlobalLeaderboardFragment;

/**
 * Adapter for LeaderboardActivity ViewPager
 */
public class LeaderboardPagerAdapter extends FragmentStateAdapter {

    private String displayMode;
    private GlobalLeaderboardFragment globalFragment;
    private FriendsLeaderboardFragment friendsFragment;

    public LeaderboardPagerAdapter(@NonNull FragmentActivity fragmentActivity, String displayMode) {
        super(fragmentActivity);
        this.displayMode = displayMode;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            // Global leaderboard
            globalFragment = GlobalLeaderboardFragment.newInstance(displayMode);
            return globalFragment;
        } else {
            // Friends leaderboard
            friendsFragment = FriendsLeaderboardFragment.newInstance(displayMode);
            return friendsFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Two tabs: Global and Friends
    }

    /**
     * Update display mode for both fragments
     */
    public void updateDisplayMode(String mode) {
        this.displayMode = mode;

        if (globalFragment != null) {
            globalFragment.updateDisplayMode(mode);
        }

        if (friendsFragment != null) {
            friendsFragment.updateDisplayMode(mode);
        }
    }
}