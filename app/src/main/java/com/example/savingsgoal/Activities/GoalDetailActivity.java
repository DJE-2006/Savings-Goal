package com.example.savingsgoal.Activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.savingsgoal.Data.Callback;
import com.example.savingsgoal.Data.Repository;
import com.example.savingsgoal.Models.SavingsGoal;
import com.example.savingsgoal.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class GoalDetailActivity extends AppCompatActivity {

    private TextView    tvTitle, tvSaved, tvTarget, tvDeadline, tvStatus, tvPercent;
    private ProgressBar progressBar;
    private LinearProgressIndicator pbGoal;
    private MaterialButton btnEdit, btnDelete, btnAddContribution, btnUpdateStatus;
    private TextInputLayout tilContribution, tilNote;
    private TextInputEditText etContribution, etNote;
    private TextView chipAmt100, chipAmt500, chipAmt1000, chipAmt5000;
    private long       goalId;
    private Repository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        goalId = getIntent().getLongExtra("goal_id", -1);
        if (goalId == -1L) {
            finish();
            return;
        }

        repo = Repository.get(this);
        initViews();

        btnEdit.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, AddEditGoalActivity.class);
            intent.putExtra("goal_id", goalId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> confirmDelete());
        btnAddContribution.setOnClickListener(v -> addContribution());
        btnUpdateStatus.setOnClickListener(v -> showStatusDialog());

        chipAmt100.setOnClickListener(v -> setAmount("100"));
        chipAmt500.setOnClickListener(v -> setAmount("500"));
        chipAmt1000.setOnClickListener(v -> setAmount("1000"));
        chipAmt5000.setOnClickListener(v -> setAmount("5000"));
    }

    private void setAmount(String amount) {
        etContribution.setText(amount);
        etContribution.setSelection(amount.length());
    }

    private void initViews() {
        tvTitle            = findViewById(R.id.tvTitle);
        tvSaved            = findViewById(R.id.tvSaved);
        tvTarget           = findViewById(R.id.tvTarget);
        tvDeadline         = findViewById(R.id.tvDeadline);
        tvStatus           = findViewById(R.id.tvStatus);
        tvPercent          = findViewById(R.id.tvPercent);
        progressBar        = findViewById(R.id.progressBar);
        pbGoal             = findViewById(R.id.pbGoal);
        btnEdit            = findViewById(R.id.btnEdit);
        btnDelete          = findViewById(R.id.btnDelete);
        btnAddContribution = findViewById(R.id.btnAddContribution);
        btnUpdateStatus    = findViewById(R.id.btnUpdateStatus);
        tilContribution    = findViewById(R.id.tilContribution);
        tilNote            = findViewById(R.id.tilNote);
        etContribution     = findViewById(R.id.etContribution);
        etNote             = findViewById(R.id.etNote);
        chipAmt100         = findViewById(R.id.chipAmt100);
        chipAmt500         = findViewById(R.id.chipAmt500);
        chipAmt1000        = findViewById(R.id.chipAmt1000);
        chipAmt5000        = findViewById(R.id.chipAmt5000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGoalDetail();
    }

    private void loadGoalDetail() {
        showLoading(true);
        repo.getGoal(goalId, new Callback<SavingsGoal>() {
            @Override public void onSuccess(SavingsGoal goal) {
                showLoading(false);
                tvTitle.setText(goal.getTitle());
                tvSaved.setText(String.format(Locale.getDefault(), "₱%,.2f", goal.getSavedAmount()));
                tvTarget.setText(String.format(Locale.getDefault(), "Target: ₱%,.2f", goal.getTargetAmount()));
                tvDeadline.setText(String.format("📅 %s", goal.getDeadline() == null ? "" : goal.getDeadline()));
                tvStatus.setText(goal.getStatus());
                tvPercent.setText(String.format(Locale.getDefault(), "%d%%", goal.getProgressPercent()));
                pbGoal.setProgress(goal.getProgressPercent(), true);
                updateStatusUI(goal.getStatus());
            }
            @Override public void onError(String message) {
                showLoading(false);
                Toast.makeText(GoalDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatusUI(String status) {
        int statusColor;
        int bgColor;
        switch (status) {
            case "Completed":
                statusColor = Color.parseColor("#10B981");
                bgColor = Color.parseColor("#D1FAE5");
                break;
            case "In Progress":
                statusColor = Color.parseColor("#3B82F6");
                bgColor = Color.parseColor("#DBEAFE");
                break;
            case "Cancelled":
                statusColor = Color.parseColor("#EF4444");
                bgColor = Color.parseColor("#FEE2E2");
                break;
            default:
                statusColor = Color.parseColor("#F59E0B");
                bgColor = Color.parseColor("#FEF3C7");
                break;
        }
        tvStatus.setTextColor(statusColor);
        tvStatus.setBackgroundTintList(ColorStateList.valueOf(bgColor));
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to permanently delete this goal? This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteGoal())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_menu_delete)
                .show();
    }

    private void deleteGoal() {
        showLoading(true);
        repo.deleteGoal(goalId, new Callback<Void>() {
            @Override public void onSuccess(Void result) {
                showLoading(false);
                Toast.makeText(GoalDetailActivity.this, "Goal deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onError(String message) {
                showLoading(false);
                Toast.makeText(GoalDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addContribution() {
        String amountStr = etContribution.getText() != null ? etContribution.getText().toString().trim() : "";
        String note      = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        tilContribution.setError(null);

        if (amountStr.isEmpty()) {
            tilContribution.setError("Amount is required");
            etContribution.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                tilContribution.setError("Must be greater than zero");
                return;
            }
        } catch (NumberFormatException e) {
            tilContribution.setError("Enter a valid number");
            return;
        }

        showLoading(true);
        repo.contribute(goalId, amount, note, new Callback<Void>() {
            @Override public void onSuccess(Void result) {
                showLoading(false);
                Toast.makeText(GoalDetailActivity.this, "Contribution added", Toast.LENGTH_SHORT).show();
                etContribution.setText("");
                etNote.setText("");
                loadGoalDetail();
            }
            @Override public void onError(String message) {
                showLoading(false);
                Toast.makeText(GoalDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showStatusDialog() {
        String[] statuses = {"Not Started", "In Progress", "Completed", "Cancelled"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Status")
                .setItems(statuses, (d, which) -> updateStatus(statuses[which]))
                .show();
    }

    private void updateStatus(String status) {
        showLoading(true);
        repo.updateGoalStatus(goalId, status, new Callback<Void>() {
            @Override public void onSuccess(Void result) {
                showLoading(false);
                Toast.makeText(GoalDetailActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                loadGoalDetail();
            }
            @Override public void onError(String message) {
                showLoading(false);
                Toast.makeText(GoalDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnAddContribution.setEnabled(!isLoading);
        btnEdit.setEnabled(!isLoading);
        btnDelete.setEnabled(!isLoading);
    }
}
