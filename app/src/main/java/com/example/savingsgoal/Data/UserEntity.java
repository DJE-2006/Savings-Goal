package com.example.savingsgoal.Data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String email;
    public String passwordHash;
    public String passwordSalt;

    @Nullable public String avatarEmoji;
    @Nullable public String avatarPath;
    @Nullable public String accentColor;
    @Nullable public String bio;

    public long createdAt;
}
