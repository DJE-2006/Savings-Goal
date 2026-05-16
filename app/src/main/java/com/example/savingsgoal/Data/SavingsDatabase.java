package com.example.savingsgoal.Data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {UserEntity.class, GoalEntity.class, ContributionEntity.class},
        version = 1,
        exportSchema = false)
public abstract class SavingsDatabase extends RoomDatabase {

    private static volatile SavingsDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract GoalDao goalDao();
    public abstract ContributionDao contributionDao();

    public static SavingsDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (SavingsDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SavingsDatabase.class,
                            "savings_goal.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
