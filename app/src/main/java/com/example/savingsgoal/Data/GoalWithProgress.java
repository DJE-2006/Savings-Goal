package com.example.savingsgoal.Data;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;

public class GoalWithProgress {

    @Embedded
    public GoalEntity goal;

    @ColumnInfo(name = "savedAmount")
    public double savedAmount;
}
