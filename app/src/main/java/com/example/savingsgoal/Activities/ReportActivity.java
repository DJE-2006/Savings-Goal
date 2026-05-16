package com.example.savingsgoal.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.savingsgoal.Adapter.GoalAdapter;
import com.example.savingsgoal.Data.Callback;
import com.example.savingsgoal.Data.Repository;
import com.example.savingsgoal.Data.ReportSummary;
import com.example.savingsgoal.Helpers.BottomNavHelper;
import com.example.savingsgoal.Helpers.SessionManager;
import com.example.savingsgoal.Models.SavingsGoal;
import com.example.savingsgoal.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private TextView    tvTotalGoals, tvTotalSaved, tvTotalTarget,
            tvCompletedCount, tvInProgressCount, tvCancelledCount;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private GoalAdapter adapter;
    private List<SavingsGoal> completedGoals;
    private SessionManager sessionManager;
    private Repository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(null);

        sessionManager = new SessionManager(this);
        repo = Repository.get(this);
        completedGoals = new ArrayList<>();

        initViews();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        BottomNavHelper.wire(this, bottomNav, R.id.nav_report);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GoalAdapter(this, completedGoals);
        recyclerView.setAdapter(adapter);

        loadReport();
    }

    private void initViews() {
        tvTotalGoals      = findViewById(R.id.tvTotalGoals);
        tvTotalSaved      = findViewById(R.id.tvTotalSaved);
        tvTotalTarget     = findViewById(R.id.tvTotalTarget);
        tvCompletedCount  = findViewById(R.id.tvCompletedCount);
        tvInProgressCount = findViewById(R.id.tvInProgressCount);
        tvCancelledCount  = findViewById(R.id.tvCancelledCount);
        progressBar       = findViewById(R.id.progressBar);
        recyclerView      = findViewById(R.id.recyclerView);
    }

    private void loadReport() {
        long userId = sessionManager.getUserId();
        if (userId == -1L) return;
        progressBar.setVisibility(View.VISIBLE);

        repo.reportSummary(userId, new Callback<ReportSummary>() {
            @Override public void onSuccess(ReportSummary r) {
                progressBar.setVisibility(View.GONE);
                tvTotalGoals.setText(String.valueOf(r.totalGoals));
                tvTotalSaved.setText(String.format(Locale.getDefault(), "₱%,.2f", r.totalSaved));
                tvTotalTarget.setText(String.format(Locale.getDefault(), "₱%,.2f", r.totalTarget));
                tvCompletedCount.setText(String.valueOf(r.completed));
                tvInProgressCount.setText(String.valueOf(r.inProgress));
                tvCancelledCount.setText(String.valueOf(r.cancelled));
                adapter.updateList(r.completedGoals);
                recyclerView.scheduleLayoutAnimation();
            }
            @Override public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ReportActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
