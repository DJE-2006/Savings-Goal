package com.example.savingsgoal.Data;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {

    @Insert
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Nullable
    @Query("SELECT * FROM users WHERE id = :id")
    UserEntity findById(long id);

    @Nullable
    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE")
    UserEntity findByEmail(String email);
}
