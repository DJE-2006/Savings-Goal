package com.example.savingsgoal.Helpers;

import android.app.Activity;
import android.content.Intent;

import com.example.savingsgoal.Activities.DashboardActivity;
import com.example.savingsgoal.Activities.GoalsListActivity;
import com.example.savingsgoal.Activities.ReportActivity;
import com.example.savingsgoal.Activities.UserProfileActivity;
import com.example.savingsgoal.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void wire(Activity activity, BottomNavigationView nav, int selectedItemId) {
        nav.setSelectedItemId(selectedItemId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return true;

            Class<?> target = null;
            if (id == R.id.nav_home)    target = DashboardActivity.class;
            else if (id == R.id.nav_goals)   target = GoalsListActivity.class;
            else if (id == R.id.nav_report)  target = ReportActivity.class;
            else if (id == R.id.nav_profile) target = UserProfileActivity.class;
            if (target == null) return false;

            Intent intent = new Intent(activity, target);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            return true;
        });
    }
}
