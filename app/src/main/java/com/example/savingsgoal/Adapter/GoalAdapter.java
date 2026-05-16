package com.example.savingsgoal.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.savingsgoal.Activities.GoalDetailActivity;
import com.example.savingsgoal.Models.SavingsGoal;
import com.example.savingsgoal.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    private final Context context;
    private final List<SavingsGoal> goalList;
    private List<SavingsGoal> goalListFull;

    public GoalAdapter(Context context, List<SavingsGoal> goalList) {
        this.context       = context;
        this.goalList      = goalList;
        this.goalListFull  = new ArrayList<>(goalList);
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        SavingsGoal goal = goalList.get(position);

        holder.tvTitle.setText(goal.getTitle());
        holder.tvSaved.setText(String.format(Locale.getDefault(), "₱%,.2f of ₱%,.2f",
                goal.getSavedAmount(), goal.getTargetAmount()));
        holder.tvDeadline.setText(String.format("📅 %s", goal.getDeadline()));
        holder.tvStatus.setText(goal.getStatus());
        holder.tvPercent.setText(String.format(Locale.getDefault(), "%d%%", goal.getProgressPercent()));
        holder.progressBar.setProgress(goal.getProgressPercent(), true);

        int statusColor;
        int bgColor;
        int progressColor;
        switch (goal.getStatus()) {
            case "Completed":
                statusColor   = Color.parseColor("#10B981");
                bgColor       = Color.parseColor("#D1FAE5");
                progressColor = Color.parseColor("#10B981");
                break;
            case "In Progress":
                statusColor   = Color.parseColor("#3B82F6");
                bgColor       = Color.parseColor("#DBEAFE");
                progressColor = Color.parseColor("#6D5DFC");
                break;
            case "Cancelled":
                statusColor   = Color.parseColor("#EF4444");
                bgColor       = Color.parseColor("#FEE2E2");
                progressColor = Color.parseColor("#EF4444");
                break;
            default:
                statusColor   = Color.parseColor("#F59E0B");
                bgColor       = Color.parseColor("#FEF3C7");
                progressColor = Color.parseColor("#F59E0B");
                break;
        }

        holder.tvStatus.setTextColor(statusColor);
        holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        holder.tvPercent.setTextColor(progressColor);
        holder.progressBar.setIndicatorColor(progressColor);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GoalDetailActivity.class);
            intent.putExtra("goal_id", goal.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return goalList.size(); }

    public void filter(String query) {
        List<SavingsGoal> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(goalListFull);
        } else {
            String lower = query.toLowerCase().trim();
            for (SavingsGoal goal : goalListFull) {
                String title  = goal.getTitle()  != null ? goal.getTitle().toLowerCase()  : "";
                String status = goal.getStatus() != null ? goal.getStatus().toLowerCase() : "";
                if (title.contains(lower) || status.contains(lower)) {
                    filteredList.add(goal);
                }
            }
        }
        updateListInternal(filteredList);
    }

    public void updateList(List<SavingsGoal> newList) {
        goalListFull = new ArrayList<>(newList);
        updateListInternal(newList);
    }

    private void updateListInternal(List<SavingsGoal> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new GoalDiffCallback(this.goalList, newList));
        this.goalList.clear();
        this.goalList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView    tvTitle, tvSaved, tvDeadline, tvStatus, tvPercent;
        LinearProgressIndicator progressBar;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle     = itemView.findViewById(R.id.tvTitle);
            tvSaved     = itemView.findViewById(R.id.tvSaved);
            tvDeadline  = itemView.findViewById(R.id.tvDeadline);
            tvStatus    = itemView.findViewById(R.id.tvStatus);
            tvPercent   = itemView.findViewById(R.id.tvPercent);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    private static class GoalDiffCallback extends DiffUtil.Callback {
        private final List<SavingsGoal> oldList;
        private final List<SavingsGoal> newList;

        GoalDiffCallback(List<SavingsGoal> oldList, List<SavingsGoal> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }
        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            SavingsGoal oldGoal = oldList.get(oldItemPosition);
            SavingsGoal newGoal = newList.get(newItemPosition);
            String oldTitle  = oldGoal.getTitle()  != null ? oldGoal.getTitle()  : "";
            String newTitle  = newGoal.getTitle()  != null ? newGoal.getTitle()  : "";
            String oldStatus = oldGoal.getStatus() != null ? oldGoal.getStatus() : "";
            String newStatus = newGoal.getStatus() != null ? newGoal.getStatus() : "";
            return oldTitle.equals(newTitle) &&
                    oldGoal.getSavedAmount() == newGoal.getSavedAmount() &&
                    oldGoal.getTargetAmount() == newGoal.getTargetAmount() &&
                    oldStatus.equals(newStatus);
        }
    }
}