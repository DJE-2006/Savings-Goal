package com.example.savingsgoal.Data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "contributions",
        foreignKeys = @ForeignKey(
                entity = GoalEntity.class,
                parentColumns = "id",
                childColumns = "goalId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("goalId")})
public class ContributionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long goalId;
    public double amount;
    @Nullable public String note;
    public long dateAdded;
}
