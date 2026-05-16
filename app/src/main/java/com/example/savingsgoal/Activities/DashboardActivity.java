package com.example.savingsgoal.Activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.savingsgoal.Data.Callback;
import com.example.savingsgoal.Data.DashboardSummary;
import com.example.savingsgoal.Data.Repository;
import com.example.savingsgoal.Helpers.BottomNavHelper;
import com.example.savingsgoal.Helpers.SessionManager;
import com.example.savingsgoal.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Calendar;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private TextView    tvWelcome, tvGreeting, tvAvatarInitial, tvTotalGoals, tvCompleted, tvInProgress,
                        tvTotalSaved, tvTotalTarget, tvHeroBadge, tvHeroPercent, tvMotivation;
    private LinearProgressIndicator heroProgress;
    private ProgressBar progressBar;
    private MaterialButton btnViewGoals, btnProfile, btnReport, btnLogout;
    private FloatingActionButton fabAddGoal;
    private FrameLayout avatarFrame;
    private SessionManager sessionManager;
    private Repository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);
        repo = Repository.get(this);

        initViews();
        applyProfileToHeader();

        btnViewGoals.setOnClickListener(v -> startActivity(new Intent(this, GoalsListActivity.class)));
        btnProfile.setOnClickListener(v ->   startActivity(new Intent(this, UserProfileActivity.class)));
        btnReport.setOnClickListener(v ->    startActivity(new Intent(this, ReportActivity.class)));
        fabAddGoal.setOnClickListener(v ->   startActivity(new Intent(this, AddEditGoalActivity.class)));
        btnLogout.setOnClickListener(v ->    logout());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        BottomNavHelper.wire(this, bottomNav, R.id.nav_home);
    }

    private void initViews() {
        tvWelcome       = findViewById(R.id.tvWelcome);
        tvGreeting      = findViewById(R.id.tvGreeting);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvTotalGoals    = findViewById(R.id.tvTotalGoals);
        tvCompleted     = findViewById(R.id.tvCompleted);
        tvInProgress    = findViewById(R.id.tvInProgress);
        tvTotalSaved    = findViewById(R.id.tvTotalSaved);
        tvTotalTarget   = findViewById(R.id.tvTotalTarget);
        tvHeroBadge     = findViewById(R.id.tvHeroBadge);
        tvHeroPercent   = findViewById(R.id.tvHeroPercent);
        tvMotivation    = findViewById(R.id.tvMotivation);
        heroProgress    = findViewById(R.id.heroProgress);
        progressBar     = findViewById(R.id.progressBar);
        btnViewGoals    = findViewById(R.id.btnViewGoals);
        btnProfile      = findViewById(R.id.btnProfile);
        btnReport       = findViewById(R.id.btnReport);
        btnLogout       = findViewById(R.id.btnLogout);
        fabAddGoal      = findViewById(R.id.fabAddGoal);
        avatarFrame     = (FrameLayout) tvAvatarInitial.getParent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyProfileToHeader();
        loadSummary();
        fabAddGoal.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
    }

    private void applyProfileToHeader() {
        String name = sessionManager.getUserName();
        tvWelcome.setText(name == null || name.isEmpty() ? "Welcome!" : name);
        tvGreeting.setText(timeBasedGreeting());

        String emoji = sessionManager.getAvatarEmoji();
        if (emoji != null && !emoji.isEmpty()) {
            tvAvatarInitial.setText(emoji);
            tvAvatarInitial.setTextSize(20f);
        } else {
            tvAvatarInitial.setText(initialFor(name));
            tvAvatarInitial.setTextSize(18f);
        }

        if (avatarFrame != null) {
            avatarFrame.setBackgroundTintList(ColorStateList.valueOf(sessionManager.getAccentColorInt()));
        }
    }

    private void loadSummary() {
        long userId = sessionManager.getUserId();
        if (userId == -1L) return;
        progressBar.setVisibility(View.VISIBLE);

        repo.dashboardSummary(userId, new Callback<DashboardSummary>() {
            @Override public void onSuccess(DashboardSummary s) {
                progressBar.setVisibility(View.GONE);
                tvTotalGoals.setText(String.valueOf(s.totalGoals));
                tvCompleted.setText(String.valueOf(s.completed));
                tvInProgress.setText(String.format(Locale.getDefault(),
                        "%d active goal%s", s.inProgress, s.inProgress == 1 ? "" : "s"));

                tvTotalSaved.setText(String.format(Locale.getDefault(), "₱%,.2f", s.totalSaved));
                tvTotalTarget.setText(String.format(Locale.getDefault(), "₱%,.2f", s.totalTarget));

                int percent = s.totalTarget > 0
                        ? (int) Math.min(100, Math.round(s.totalSaved / s.totalTarget * 100.0))
                        : 0;
                heroProgress.setProgress(percent, true);
                tvHeroPercent.setText(String.format(Locale.getDefault(), "%d%% completed", percent));
                tvHeroBadge.setText(badgeFor(percent));
                tvMotivation.setText(motivationFor(percent, s.totalGoals));
            }
            @Override public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String timeBasedGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 5)  return "Good night";
        if (h < 12) return "Good morning";
        if (h < 17) return "Good afternoon";
        if (h < 21) return "Good evening";
        return "Good night";
    }

    private String initialFor(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        return name.trim().substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private String badgeFor(int percent) {
        if (percent >= 100) return "🏆 Achieved";
        if (percent >= 75)  return "🚀 Almost there";
        if (percent >= 40)  return "🔥 On track";
        if (percent > 0)    return "🌱 Building";
        return "✨ Start saving";
    }

    private String motivationFor(int percent, int totalGoals) {
        if (totalGoals == 0)  return "Add your first goal";
        if (percent >= 100)   return "Amazing work 🎉";
        if (percent >= 75)    return "So close ✨";
        if (percent >= 40)    return "Keep going 💪";
        return "One step at a time";
    }

    private void logout() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
