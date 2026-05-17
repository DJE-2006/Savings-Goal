package com.example.savingsgoal.Data;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.savingsgoal.Helpers.SessionManager;
import com.example.savingsgoal.Models.SavingsGoal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Network-backed repository. Public surface unchanged from the original Room
 * version, so every existing Activity keeps working without edits.
 *
 * - Auth / goals / contributions / dashboard / report / profile  → REST API
 *   (https://savingsgoal-api.onrender.com)
 * - Avatar image  → stored locally in app filesDir (no upload endpoint yet)
 */
public class Repository {

    private static volatile Repository INSTANCE;

    private final Context appContext;
    private final ApiClient api;
    private final SessionManager session;

    private Repository(Context context) {
        this.appContext = context.getApplicationContext();
        this.api        = ApiClient.get(appContext);
        this.session    = new SessionManager(appContext);
    }

    public static Repository get(Context context) {
        if (INSTANCE == null) {
            synchronized (Repository.class) {
                if (INSTANCE == null) INSTANCE = new Repository(context);
            }
        }
        return INSTANCE;
    }

    // ============================ Auth ============================

    public void register(String name, String email, String password,
                         final Callback<UserEntity> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("email", email);
            body.put("password", password);
        } catch (Exception e) { cb.onError("Couldn't build request."); return; }

        api.post("/auth/register", body, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject data) { handleAuth(data, cb); }
            @Override public void onError(String message)    { cb.onError(message); }
        });
    }

    public void login(String email, String password, final Callback<UserEntity> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (Exception e) { cb.onError("Couldn't build request."); return; }

        api.post("/auth/login", body, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject data) { handleAuth(data, cb); }
            @Override public void onError(String message)    { cb.onError(message); }
        });
    }

    private void handleAuth(JSONObject data, Callback<UserEntity> cb) {
        try {
            String token = data.optString("token", null);
            if (token != null) session.setToken(token);
            UserEntity u = userFromJson(data.getJSONObject("user"));
            cb.onSuccess(u);
        } catch (Exception e) {
            cb.onError("Unexpected response from server.");
        }
    }

    // ============================ Goals ============================

    public void getGoals(long userId, final Callback<List<SavingsGoal>> cb) {
        api.get("/goals", new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject data) {
                try {
                    JSONArray arr = data.getJSONArray("value");
                    List<SavingsGoal> list = new ArrayList<>(arr.length());
                    for (int i = 0; i < arr.length(); i++) list.add(goalFromJson(arr.getJSONObject(i)));
                    cb.onSuccess(list);
                } catch (Exception e) { cb.onError("Failed to load goals."); }
            }
            @Override public void onError(String message) { cb.onError(message); }
        });
    }

    public void getGoal(long goalId, final Callback<SavingsGoal> cb) {
        api.get("/goals/" + goalId, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject data) {
                try { cb.onSuccess(goalFromJson(data)); }
                catch (Exception e) { cb.onError("Failed to load goal."); }
            }
            @Override public void onError(String message) { cb.onError(message); }
        });
    }

    public void createGoal(long userId, String title, double targetAmount, String deadline,
                           final Callback<Long> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("title", title);
            body.put("targetAmount", targetAmount);
            body.put("deadline", deadline);
        } catch (Exception e) { cb.onError("Couldn't build request."); return; }

        api.post("/goals", body, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject data) {
                cb.onSuccess(data.optLong("id", 0L));
            }
            @Override public void onError(String message) { cb.onError(message); }
        });
    }

    public void updateGoal(long goalId, String title, double targetAmount, String deadline,
                           final Callback<Void> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("title", title);
            body.put("targetAmount", targetAmount);
            body.put("deadline", deadline);
        } catch (Exception e) { cb.onError("Couldn't build request."); return; }
        api.put("/goals/" + goalId, body, voidCallback(cb));
    }

    public void updateGoalStatus(long goalId, String status, final Callback<Void> cb) {
        JSONObject body = new JSONObject();
        try { body.put("status", status); }
        catch (Exception e) { cb.onError("Couldn't build request."); return; }
        api.put("/goals/" + goalId, body, voidCallback(cb));
    }

    public void deleteGoal(long goalId, final Callback<Void> cb) {
        api.delete("/goals/" + goalId, voidCallback(cb));
    }

    // ============================ Contributions ============================

    public void contribute(long goalId, double amount, String note, final Callback<Void> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
            if (note != null && !note.isEmpty()) body.put("note", note);
        } catch (Exception e) { cb.onError("Couldn't build request."); return; }
        api.post("/goals/" + goalId + "/contributions", body, voidCallback(cb));
    }

    // ============================ Aggregates ============================

    public void dashboardSummary(long userId, final Callback<DashboardSummary> cb) {
        api.get("/dashboard", new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject data) {
                DashboardSummary s = new DashboardSummary();
                s.totalGoals  = data.optInt("totalGoals");
                s.completed   = data.optInt("completed");
                s.inProgress  = data.optInt("inProgress");
                s.totalTarget = data.optDouble("totalTarget", 0);
                s.totalSaved  = data.optDouble("totalSaved", 0);
                cb.onSuccess(s);
            }
            @Override public void onError(String message) { cb.onError(message); }
        });
    }

    public void reportSummary(long userId, final Callback<ReportSummary> cb) {
        api.get("/report", new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject data) {
                try {
                    ReportSummary r = new ReportSummary();
                    r.totalGoals  = data.optInt("totalGoals");
                    r.completed   = data.optInt("completed");
                    r.inProgress  = data.optInt("inProgress");
                    r.cancelled   = data.optInt("cancelled");
                    r.totalTarget = data.optDouble("totalTarget", 0);
                    r.totalSaved  = data.optDouble("totalSaved", 0);
                    JSONArray arr = data.optJSONArray("completedGoals");
                    List<SavingsGoal> list = new ArrayList<>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) list.add(goalFromJson(arr.getJSONObject(i)));
                    }
                    r.completedGoals = list;
                    cb.onSuccess(r);
                } catch (Exception e) { cb.onError("Failed to load report."); }
            }
            @Override public void onError(String message) { cb.onError(message); }
        });
    }

    // ============================ Profile ============================

    public void getUser(long userId, final Callback<UserEntity> cb) {
        api.get("/profile", new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject data) {
                try { cb.onSuccess(userFromJson(data)); }
                catch (Exception e) { cb.onError("Failed to load profile."); }
            }
            @Override public void onError(String message) { cb.onError(message); }
        });
    }

    public void updateProfile(long userId, String name, String email, String bio,
                              String avatarEmoji, String accentColor,
                              String currentPassword, String newPassword,
                              final Callback<UserEntity> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("name",        name);
            body.put("email",       email);
            body.put("bio",         bio        == null ? "" : bio);
            body.put("avatarEmoji", avatarEmoji == null ? "" : avatarEmoji);
            body.put("accentColor", accentColor == null ? "" : accentColor);
            if (newPassword != null && !newPassword.isEmpty()) {
                body.put("currentPassword", currentPassword == null ? "" : currentPassword);
                body.put("newPassword",     newPassword);
            }
        } catch (Exception e) { cb.onError("Couldn't build request."); return; }

        api.put("/profile", body, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject data) {
                try { cb.onSuccess(userFromJson(data)); }
                catch (Exception e) { cb.onError("Failed to parse profile."); }
            }
            @Override public void onError(String message) { cb.onError(message); }
        });
    }

    // ============================ Avatar (local-only) ============================

    public void saveAvatar(long userId, final Bitmap bitmap, final Callback<String> cb) {
        AppExecutors.io(() -> {
            try {
                File dir = new File(appContext.getFilesDir(), "avatars");
                if (!dir.exists() && !dir.mkdirs()) throw new IOException("Couldn't create avatars dir");
                File out = new File(dir, userId + ".jpg");
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                }
                final String path = out.getAbsolutePath();
                AppExecutors.main(() -> cb.onSuccess(path));
            } catch (Exception e) {
                AppExecutors.main(() -> cb.onError("Couldn't save photo."));
            }
        });
    }

    public void deleteAvatar(long userId, final Callback<Void> cb) {
        AppExecutors.io(() -> {
            try {
                File f = new File(new File(appContext.getFilesDir(), "avatars"), userId + ".jpg");
                if (f.exists()) //noinspection ResultOfMethodCallIgnored
                    f.delete();
                AppExecutors.main(() -> cb.onSuccess(null));
            } catch (Exception e) {
                AppExecutors.main(() -> cb.onError("Couldn't remove photo."));
            }
        });
    }

    // ============================ Helpers ============================

    private static Callback<JSONObject> voidCallback(final Callback<Void> cb) {
        return new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject _ignored) { cb.onSuccess(null); }
            @Override public void onError(String message)        { cb.onError(message); }
        };
    }

    private static SavingsGoal goalFromJson(JSONObject j) {
        SavingsGoal g = new SavingsGoal();
        g.setId((int) j.optLong("id"));
        g.setUserId((int) j.optLong("userId"));
        g.setTitle(j.optString("title"));
        g.setTargetAmount(j.optDouble("targetAmount", 0));
        g.setSavedAmount(j.optDouble("savedAmount", 0));
        g.setDeadline(j.optString("deadline"));
        g.setStatus(j.optString("status"));
        g.setCreatedAt(String.valueOf(j.optLong("createdAt")));
        return g;
    }

    private static UserEntity userFromJson(JSONObject j) {
        UserEntity u = new UserEntity();
        u.id          = j.optLong("id");
        u.name        = j.optString("name", "");
        u.email       = j.optString("email", "");
        u.bio         = j.isNull("bio")         ? null : j.optString("bio", null);
        u.avatarEmoji = j.isNull("avatarEmoji") ? null : j.optString("avatarEmoji", null);
        u.accentColor = j.isNull("accentColor") ? null : j.optString("accentColor", null);
        u.createdAt   = j.optLong("createdAt");
        // Network responses don't carry these — leave default.
        u.passwordHash = "";
        u.passwordSalt = "";
        u.avatarPath   = null;
        return u;
    }
}
