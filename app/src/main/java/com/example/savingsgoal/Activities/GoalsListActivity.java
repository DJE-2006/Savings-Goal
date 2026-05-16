package com.example.savingsgoal.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.savingsgoal.Adapter.GoalAdapter;
import com.example.savingsgoal.Data.Callback;
import com.example.savingsgoal.Data.Repository;
import com.example.savingsgoal.Helpers.BottomNavHelper;
import com.example.savingsgoal.Helpers.SessionManager;
import com.example.savingsgoal.Models.SavingsGoal;
import com.example.savingsgoal.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class GoalsListActivity extends AppCompatActivity {

    private RecyclerView      recyclerView;
    private GoalAdapter       adapter;
    private List<SavingsGoal> goalList;
    private ProgressBar       progressBar;
    private SearchView        searchView;
    private FloatingActionButton fabAdd;
    private SessionManager    sessionManager;
    private Repository        repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(null);

        sessionManager = new SessionManager(this);
        repo           = Repository.get(this);
        goalList       = new ArrayList<>();

        initViews();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        BottomNavHelper.wire(this, bottomNav, R.id.nav_goals);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GoalAdapter(this, goalList);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditGoalActivity.class)));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query); return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText); return true;
            }
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar  = findViewById(R.id.progressBar);
        searchView   = findViewById(R.id.searchView);
        fabAdd       = findViewById(R.id.fabAdd);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGoals();
    }

    private void loadGoals() {
        long userId = sessionManager.getUserId();
        if (userId == -1L) return;
        progressBar.setVisibility(View.VISIBLE);

        repo.getGoals(userId, new Callback<List<SavingsGoal>>() {
            @Override public void onSuccess(List<SavingsGoal> list) {
                progressBar.setVisibility(View.GONE);
                adapter.updateList(list);
                recyclerView.scheduleLayoutAnimation();
            }
            @Override public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GoalsListActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
