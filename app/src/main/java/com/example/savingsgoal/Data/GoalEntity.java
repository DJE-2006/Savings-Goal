package com.example.savingsgoal.Data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "goals",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("userId")})
public class GoalEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;
    public String title;
    public double targetAmount;
    @Nullable public String deadline;
    public String status;
    public long createdAt;
}
