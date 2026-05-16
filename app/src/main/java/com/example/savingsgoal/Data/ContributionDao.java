package com.example.savingsgoal.Data;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface ContributionDao {

    @Insert
    long insert(ContributionEntity contribution);
}
