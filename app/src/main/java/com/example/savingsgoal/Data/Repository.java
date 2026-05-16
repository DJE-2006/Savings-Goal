package com.example.savingsgoal.Data;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.savingsgoal.Models.SavingsGoal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Repository {

    private static volatile Repository INSTANCE;

    private final Context appContext;
    private final UserDao userDao;
    private final GoalDao goalDao;
    private final ContributionDao contributionDao;

    private Repository(Context context) {
        this.appContext = context.getApplicationContext();
        SavingsDatabase db = SavingsDatabase.get(appContext);
        this.userDao = db.userDao();
        this.goalDao = db.goalDao();
        this.contributionDao = db.contributionDao();
    }

    public static Repository get(Context context) {
        if (INSTANCE == null) {
            synchronized (Repository.class) {
                if (INSTANCE == null) INSTANCE = new Repository(context);
            }
        }
        return INSTANCE;
    }

    // -------- Auth --------

    public void register(String name, String email, String password, Callback<UserEntity> cb) {
        AppExecutors.io(() -> {
            try {
                if (userDao.findByEmail(email) != null) {
                    deliverError(cb, "An account with that email already exists.");
                    return;
                }
                UserEntity u = new UserEntity();
                u.name = name;
                u.email = email;
                u.passwordSalt = PasswordHasher.newSalt();
                u.passwordHash = PasswordHasher.hash(password, u.passwordSalt);
                u.accentColor = "#0F766E";
                u.createdAt = System.currentTimeMillis();
                u.id = userDao.insert(u);
                deliverSuccess(cb, u);
            } catch (Exception e) {
                deliverError(cb, "Couldn't create account: " + e.getMessage());
            }
        });
    }

    public void login(String email, String password, Callback<UserEntity> cb) {
        AppExecutors.io(() -> {
            try {
                UserEntity u = userDao.findByEmail(email);
                if (u == null || !PasswordHasher.matches(password, u.passwordSalt, u.passwordHash)) {
                    deliverError(cb, "Incorrect email or password.");
                    return;
                }
                deliverSuccess(cb, u);
            } catch (Exception e) {
                deliverError(cb, "Login failed: " + e.getMessage());
            }
        });
    }

    // -------- Goals --------

    public void getGoals(long userId, Callback<List<SavingsGoal>> cb) {
        AppExecutors.io(() -> {
            try {
                List<GoalWithProgress> rows = goalDao.getAllByUser(userId);
                List<SavingsGoal> list = new ArrayList<>(rows.size());
                for (GoalWithProgress r : rows) list.add(toModel(r));
                deliverSuccess(cb, list);
            } catch (Exception e) {
                deliverError(cb, "Failed to load goals.");
            }
        });
    }

    public void getGoal(long goalId, Callback<SavingsGoal> cb) {
        AppExecutors.io(() -> {
            try {
                GoalWithProgress r = goalDao.getWithProgress(goalId);
                if (r == null) { deliverError(cb, "Goal not found."); return; }
                deliverSuccess(cb, toModel(r));
            } catch (Exception e) {
                deliverError(cb, "Failed to load goal.");
            }
        });
    }

    public void createGoal(long userId, String title, double targetAmount, String deadline,
                           Callback<Long> cb) {
        AppExecutors.io(() -> {
            try {
                GoalEntity g = new GoalEntity();
                g.userId = userId;
                g.title = title;
                g.targetAmount = targetAmount;
                g.deadline = deadline;
                g.status = "Not Started";
                g.createdAt = System.currentTimeMillis();
                long id = goalDao.insert(g);
                deliverSuccess(cb, id);
            } catch (Exception e) {
                deliverError(cb, "Failed to create goal.");
            }
        });
    }

    public void updateGoal(long goalId, String title, double targetAmount, String deadline,
                           Callback<Void> cb) {
        AppExecutors.io(() -> {
            try {
                GoalEntity g = goalDao.getById(goalId);
                if (g == null) { deliverError(cb, "Goal not found."); return; }
                g.title = title;
                g.targetAmount = targetAmount;
                g.deadline = deadline;
                goalDao.update(g);
                deliverSuccess(cb, null);
            } catch (Exception e) {
                deliverError(cb, "Failed to update goal.");
            }
        });
    }

    public void updateGoalStatus(long goalId, String status, Callback<Void> cb) {
        AppExecutors.io(() -> {
            try {
                goalDao.updateStatus(goalId, status);
                deliverSuccess(cb, null);
            } catch (Exception e) {
                deliverError(cb, "Failed to update status.");
            }
        });
    }

    public void deleteGoal(long goalId, Callback<Void> cb) {
        AppExecutors.io(() -> {
            try {
                goalDao.deleteById(goalId);
                deliverSuccess(cb, null);
            } catch (Exception e) {
                deliverError(cb, "Failed to delete goal.");
            }
        });
    }

    // -------- Contributions --------

    public void contribute(long goalId, double amount, String note, Callback<Void> cb) {
        AppExecutors.io(() -> {
            try {
                ContributionEntity c = new ContributionEntity();
                c.goalId = goalId;
                c.amount = amount;
                c.note = (note == null || note.isEmpty()) ? null : note;
                c.dateAdded = System.currentTimeMillis();
                contributionDao.insert(c);

                // Auto-progress a goal off "Not Started" once it has any contribution.
                GoalEntity g = goalDao.getById(goalId);
                if (g != null && "Not Started".equals(g.status)) {
                    goalDao.updateStatus(goalId, "In Progress");
                }
                deliverSuccess(cb, null);
            } catch (Exception e) {
                deliverError(cb, "Failed to add contribution.");
            }
        });
    }

    // -------- Aggregates --------

    public void dashboardSummary(long userId, Callback<DashboardSummary> cb) {
        AppExecutors.io(() -> {
            try {
                DashboardSummary s = new DashboardSummary();
                s.totalGoals  = goalDao.countByUser(userId);
                s.completed   = goalDao.countByStatus(userId, "Completed");
                s.inProgress  = goalDao.countByStatus(userId, "In Progress");
                s.totalTarget = goalDao.sumTarget(userId);
                s.totalSaved  = goalDao.sumSaved(userId);
                deliverSuccess(cb, s);
            } catch (Exception e) {
                deliverError(cb, "Failed to load summary.");
            }
        });
    }

    public void reportSummary(long userId, Callback<ReportSummary> cb) {
        AppExecutors.io(() -> {
            try {
                ReportSummary r = new ReportSummary();
                r.totalGoals  = goalDao.countByUser(userId);
                r.completed   = goalDao.countByStatus(userId, "Completed");
                r.inProgress  = goalDao.countByStatus(userId, "In Progress");
                r.cancelled   = goalDao.countByStatus(userId, "Cancelled");
                r.totalTarget = goalDao.sumTarget(userId);
                r.totalSaved  = goalDao.sumSaved(userId);

                List<GoalWithProgress> rows = goalDao.getCompletedByUser(userId);
                List<SavingsGoal> list = new ArrayList<>(rows.size());
                for (GoalWithProgress row : rows) list.add(toModel(row));
                r.completedGoals = list;

                deliverSuccess(cb, r);
            } catch (Exception e) {
                deliverError(cb, "Failed to load report.");
            }
        });
    }

    // -------- Profile --------

    public void getUser(long userId, Callback<UserEntity> cb) {
        AppExecutors.io(() -> {
            try {
                UserEntity u = userDao.findById(userId);
                if (u == null) { deliverError(cb, "Profile not found."); return; }
                deliverSuccess(cb, u);
            } catch (Exception e) {
                deliverError(cb, "Failed to load profile.");
            }
        });
    }

    public void updateProfile(long userId, String name, String email, String bio,
                              String avatarEmoji, String accentColor,
                              String currentPassword, String newPassword,
                              Callback<UserEntity> cb) {
        AppExecutors.io(() -> {
            try {
                UserEntity u = userDao.findById(userId);
                if (u == null) { deliverError(cb, "Profile not found."); return; }

                if (!u.email.equalsIgnoreCase(email)) {
                    UserEntity other = userDao.findByEmail(email);
                    if (other != null && other.id != userId) {
                        deliverError(cb, "That email is already in use.");
                        return;
                    }
                }

                if (newPassword != null && !newPassword.isEmpty()) {
                    if (currentPassword == null || currentPassword.isEmpty()
                            || !PasswordHasher.matches(currentPassword, u.passwordSalt, u.passwordHash)) {
                        deliverError(cb, "Current password is incorrect.");
                        return;
                    }
                    u.passwordSalt = PasswordHasher.newSalt();
                    u.passwordHash = PasswordHasher.hash(newPassword, u.passwordSalt);
                }

                u.name = name;
                u.email = email;
                u.bio = (bio == null || bio.isEmpty()) ? null : bio;
                u.avatarEmoji = (avatarEmoji == null || avatarEmoji.isEmpty()) ? null : avatarEmoji;
                u.accentColor = (accentColor == null || accentColor.isEmpty()) ? null : accentColor;
                userDao.update(u);
                deliverSuccess(cb, u);
            } catch (Exception e) {
                deliverError(cb, "Failed to save profile.");
            }
        });
    }

    public void saveAvatar(long userId, Bitmap bitmap, Callback<String> cb) {
        AppExecutors.io(() -> {
            try {
                File dir = new File(appContext.getFilesDir(), "avatars");
                if (!dir.exists() && !dir.mkdirs()) throw new IOException("Couldn't create avatars dir");
                File out = new File(dir, userId + ".jpg");
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                }
                String path = out.getAbsolutePath();
                UserEntity u = userDao.findById(userId);
                if (u != null) {
                    u.avatarPath = path;
                    userDao.update(u);
                }
                deliverSuccess(cb, path);
            } catch (Exception e) {
                deliverError(cb, "Couldn't save photo.");
            }
        });
    }

    public void deleteAvatar(long userId, Callback<Void> cb) {
        AppExecutors.io(() -> {
            try {
                UserEntity u = userDao.findById(userId);
                if (u != null && u.avatarPath != null) {
                    new File(u.avatarPath).delete();
                    u.avatarPath = null;
                    userDao.update(u);
                }
                deliverSuccess(cb, null);
            } catch (Exception e) {
                deliverError(cb, "Couldn't remove photo.");
            }
        });
    }

    // -------- Helpers --------

    private static SavingsGoal toModel(GoalWithProgress row) {
        SavingsGoal m = new SavingsGoal();
        m.setId((int) row.goal.id);
        m.setUserId((int) row.goal.userId);
        m.setTitle(row.goal.title);
        m.setTargetAmount(row.goal.targetAmount);
        m.setSavedAmount(row.savedAmount);
        m.setDeadline(row.goal.deadline);
        m.setStatus(row.goal.status);
        m.setCreatedAt(formatTs(row.goal.createdAt));
        return m;
    }

    private static String formatTs(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(ms));
    }

    private static <T> void deliverSuccess(Callback<T> cb, T value) {
        AppExecutors.main(() -> cb.onSuccess(value));
    }

    private static <T> void deliverError(Callback<T> cb, String msg) {
        AppExecutors.main(() -> cb.onError(msg));
    }
}
