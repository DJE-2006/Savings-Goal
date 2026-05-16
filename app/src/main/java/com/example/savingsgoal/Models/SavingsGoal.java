package com.example.savingsgoal.Models;

public class SavingsGoal {

    private int    id;
    private int    userId;
    private String title;
    private double targetAmount;
    private double savedAmount;
    private String deadline;
    private String status;
    private String createdAt;

    public SavingsGoal() {}

    public int getProgressPercent() {
        if (targetAmount == 0) return 0;
        return Math.min((int) ((savedAmount / targetAmount) * 100), 100);
    }

    public int    getId()               { return id; }
    public void   setId(int id)         { this.id = id; }

    public int    getUserId()                { return userId; }
    public void   setUserId(int userId)      { this.userId = userId; }

    public String getTitle()                 { return title; }
    public void   setTitle(String title)     { this.title = title; }

    public double getTargetAmount()                        { return targetAmount; }
    public void   setTargetAmount(double targetAmount)     { this.targetAmount = targetAmount; }

    public double getSavedAmount()                         { return savedAmount; }
    public void   setSavedAmount(double savedAmount)       { this.savedAmount = savedAmount; }

    public String getDeadline()                  { return deadline; }
    public void   setDeadline(String deadline)   { this.deadline = deadline; }

    public String getStatus()                { return status; }
    public void   setStatus(String status)   { this.status = status; }

    public String getCreatedAt()                   { return createdAt; }
    public void   setCreatedAt(String createdAt)   { this.createdAt = createdAt; }
}