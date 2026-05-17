package com.example.savingsgoal.Activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.savingsgoal.Data.Callback;
import com.example.savingsgoal.Data.Repository;
import com.example.savingsgoal.Helpers.SessionManager;
import com.example.savingsgoal.Models.SavingsGoal;
import com.example.savingsgoal.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

public class AddEditGoalActivity extends AppCompatActivity {

    private TextInputLayout tilTitle, tilTargetAmount, tilDeadline;
    private TextInputEditText etTitle, etTargetAmount, etDeadline;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private Repository repo;
    private long goalId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_goal);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        sessionManager = new SessionManager(this);
        repo = Repository.get(this);
        initViews();

        goalId = getIntent().getLongExtra("goal_id", -1);

        if (goalId != -1L) {
            toolbar.setTitle(R.string.title_edit_goal);
            loadGoalData();
        } else {
            toolbar.setTitle(R.string.title_new_goal);
        }

        etDeadline.setOnClickListener(v -> showDatePicker());
        tilDeadline.setEndIconOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveGoal());
    }

    private void initViews() {
        tilTitle        = findViewById(R.id.tilTitle);
        tilTargetAmount = findViewById(R.id.tilTargetAmount);
        tilDeadline     = findViewById(R.id.tilDeadline);
        etTitle         = findViewById(R.id.etTitle);
        etTargetAmount  = findViewById(R.id.etTargetAmount);
        etDeadline      = findViewById(R.id.etDeadline);
        btnSave         = findViewById(R.id.btnSave);
        progressBar     = findViewById(R.id.progressBar);
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, day);
            etDeadline.setText(date);
            tilDeadline.setError(null);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadGoalData() {
        showLoading(true);
        repo.getGoal(goalId, new Callback<SavingsGoal>() {
            @Override public void onSuccess(SavingsGoal g) {
                showLoading(false);
                etTitle.setText(g.getTitle());
                etTargetAmount.setText(String.valueOf(g.getTargetAmount()));
                etDeadline.setText(g.getDeadline() == null ? "" : g.getDeadline());
            }
            @Override public void onError(String message) {
                showLoading(false);
                Toast.makeText(AddEditGoalActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGoal() {
        String title           = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String targetAmountStr = etTargetAmount.getText() != null ? etTargetAmount.getText().toString().trim() : "";
        String deadline        = etDeadline.getText() != null ? etDeadline.getText().toString().trim() : "";

        tilTitle.setError(null);
        tilTargetAmount.setError(null);
        tilDeadline.setError(null);

        if (title.isEmpty()) {
            tilTitle.setError(getString(R.string.error_title_required));
            etTitle.requestFocus();
            return;
        }
        if (targetAmountStr.isEmpty()) {
            tilTargetAmount.setError(getString(R.string.error_target_amount_invalid));
            etTargetAmount.requestFocus();
            return;
        }

        double targetAmount;
        try {
            targetAmount = Double.parseDouble(targetAmountStr);
            if (targetAmount <= 0) {
                tilTargetAmount.setError(getString(R.string.error_target_amount_invalid));
                return;
            }
        } catch (NumberFormatException e) {
            tilTargetAmount.setError(getString(R.string.error_target_amount_invalid));
            return;
        }

        if (deadline.isEmpty()) {
            tilDeadline.setError(getString(R.string.error_deadline_required));
            showDatePicker();
            return;
        }

        showLoading(true);

        if (goalId != -1L) {
            repo.updateGoal(goalId, title, targetAmount, deadline, new Callback<Void>() {
                @Override public void onSuccess(Void result) {
                    showLoading(false);
                    Toast.makeText(AddEditGoalActivity.this, "Goal updated", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override public void onError(String message) {
                    showLoading(false);
                    Toast.makeText(AddEditGoalActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            long userId = sessionManager.getUserId();
            repo.createGoal(userId, title, targetAmount, deadline, new Callback<Long>() {
                @Override public void onSuccess(Long id) {
                    showLoading(false);
                    Toast.makeText(AddEditGoalActivity.this, "Goal added", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override public void onError(String message) {
                    showLoading(false);
                    Toast.makeText(AddEditGoalActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
    }
}
