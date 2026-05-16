package com.example.savingsgoal.Data;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GoalDao {

    @Insert
    long insert(GoalEntity goal);

    @Update
    void update(GoalEntity goal);

    @Query("DELETE FROM goals WHERE id = :id")
    void deleteById(long id);

    @Query("UPDATE goals SET status = :status WHERE id = :id")
    void updateStatus(long id, String status);

    @Nullable
    @Query("SELECT * FROM goals WHERE id = :id")
    GoalEntity getById(long id);

    @Nullable
    @Query("SELECT g.*, " +
            "  COALESCE((SELECT SUM(amount) FROM contributions c WHERE c.goalId = g.id), 0) AS savedAmount " +
            "FROM goals g WHERE g.id = :id")
    GoalWithProgress getWithProgress(long id);

    @Query("SELECT g.*, " +
            "  COALESCE((SELECT SUM(amount) FROM contributions c WHERE c.goalId = g.id), 0) AS savedAmount " +
            "FROM goals g WHERE g.userId = :userId ORDER BY g.createdAt DESC")
    List<GoalWithProgress> getAllByUser(long userId);

    @Query("SELECT g.*, " +
            "  COALESCE((SELECT SUM(amount) FROM contributions c WHERE c.goalId = g.id), 0) AS savedAmount " +
            "FROM goals g WHERE g.userId = :userId AND g.status = 'Completed' ORDER BY g.createdAt DESC")
    List<GoalWithProgress> getCompletedByUser(long userId);

    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId")
    int countByUser(long userId);

    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND status = :status")
    int countByStatus(long userId, String status);

    @Query("SELECT COALESCE(SUM(targetAmount), 0) FROM goals WHERE userId = :userId")
    double sumTarget(long userId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM contributions c " +
            "JOIN goals g ON g.id = c.goalId WHERE g.userId = :userId")
    double sumSaved(long userId);
}
