package com.example.savingsgoal.Models;

public class Contribution {

    private int    id;
    private int    goalId;
    private double amount;
    private String note;
    private String dateAdded;

    public Contribution() {}

    public int    getId()               { return id; }
    public void   setId(int id)         { this.id = id; }

    public int    getGoalId()               { return goalId; }
    public void   setGoalId(int goalId)     { this.goalId = goalId; }

    public double getAmount()               { return amount; }
    public void   setAmount(double amount)  { this.amount = amount; }

    public String getNote()               { return note; }
    public void   setNote(String note)    { this.note = note; }

    public String getDateAdded()                   { return dateAdded; }
    public void   setDateAdded(String dateAdded)   { this.dateAdded = dateAdded; }
}