package com.example.nextalkapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Định nghĩa đúng thứ tự các Fragment tương ứng với các tab từ trái qua phải
        switch (position) {
            case 0:
                return new ChatFragment();
            case 1:
                return new FriendFragment();
            case 2:
                return new SettingFragment();
            default:
                return new ChatFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Số lượng tab của bạn
    }
}