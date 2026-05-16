package com.example.savingsgoal.Data;

public interface Callback<T> {
    void onSuccess(T result);
    void onError(String message);
}
