package com.example.savingsgoal.Data;

import com.example.savingsgoal.Models.SavingsGoal;

import java.util.List;

public class ReportSummary {
    public int totalGoals;
    public double totalSaved;
    public double totalTarget;
    public int completed;
    public int inProgress;
    public int cancelled;
    public List<SavingsGoal> completedGoals;
}
