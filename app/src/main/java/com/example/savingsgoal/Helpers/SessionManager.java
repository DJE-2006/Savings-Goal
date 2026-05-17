package com.example.savingsgoal.Helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class SessionManager {

    private static final String PREF_NAME    = "SavingsGoalSession";
    private static final String KEY_LOGIN    = "isLoggedIn";
    private static final String KEY_ID       = "userId";
    private static final String KEY_NAME     = "userName";
    private static final String KEY_EMAIL    = "userEmail";
    private static final String KEY_EMOJI    = "avatarEmoji";
    private static final String KEY_AVATAR   = "avatarPath";
    private static final String KEY_ACCENT   = "accentColor";
    private static final String KEY_BIO      = "bio";
    private static final String KEY_TOKEN    = "authToken";

    public static final String DEFAULT_ACCENT_HEX = "#0F766E";

    private final SharedPreferences pref;

    public SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createSession(long userId, String name, String email) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_LOGIN, true);
        editor.putLong(KEY_ID, userId);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public void updateProfile(String name, String email,
                              String avatarEmoji, String avatarPath,
                              String accentColor, String bio) {
        SharedPreferences.Editor editor = pref.edit();
        if (name != null)  editor.putString(KEY_NAME, name);
        if (email != null) editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_EMOJI, avatarEmoji);
        editor.putString(KEY_AVATAR, avatarPath);
        editor.putString(KEY_ACCENT, accentColor);
        editor.putString(KEY_BIO, bio);
        editor.apply();
    }

    public void setAvatarPath(String avatarPath) {
        pref.edit().putString(KEY_AVATAR, avatarPath).apply();
    }

    public void setToken(String token) {
        pref.edit().putString(KEY_TOKEN, token).apply();
    }
    public String getToken() { return pref.getString(KEY_TOKEN, null); }

    public boolean isLoggedIn()   { return pref.getBoolean(KEY_LOGIN, false); }
    public long    getUserId()    { return pref.getLong(KEY_ID, -1L); }
    public String  getUserName()  { return pref.getString(KEY_NAME, ""); }
    public String  getUserEmail() { return pref.getString(KEY_EMAIL, ""); }
    public String  getAvatarEmoji() { return pref.getString(KEY_EMOJI, null); }
    public String  getAvatarPath()  { return pref.getString(KEY_AVATAR, null); }
    public String  getAccentColor() { return pref.getString(KEY_ACCENT, null); }
    public String  getBio()         { return pref.getString(KEY_BIO, null); }

    public int getAccentColorInt() {
        String hex = getAccentColor();
        if (hex == null || hex.isEmpty()) hex = DEFAULT_ACCENT_HEX;
        try { return Color.parseColor(hex); }
        catch (IllegalArgumentException e) { return Color.parseColor(DEFAULT_ACCENT_HEX); }
    }

    public void logout() {
        pref.edit().clear().apply();
    }
}
