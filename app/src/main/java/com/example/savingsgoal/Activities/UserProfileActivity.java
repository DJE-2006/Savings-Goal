package com.example.savingsgoal.Activities;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.savingsgoal.Data.Callback;
import com.example.savingsgoal.Data.Repository;
import com.example.savingsgoal.Data.UserEntity;
import com.example.savingsgoal.Helpers.BottomNavHelper;
import com.example.savingsgoal.Helpers.SessionManager;
import com.example.savingsgoal.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.InputStream;

public class UserProfileActivity extends AppCompatActivity {

    private static final String[] EMOJI_CHOICES = {
            "😀", "😎", "🦊", "🐱", "🐶", "🐼", "🦁", "🐯",
            "🌸", "🌟", "🚀", "💎", "🔥", "🏆", "🎯", "💰"
    };

    private static final String[] ACCENT_HEX = {
            "#0F766E", "#4F46E5", "#E11D48", "#D97706", "#059669", "#7C3AED"
    };

    private TextInputLayout tilName, tilEmail, tilBio, tilCurrentPassword, tilNewPassword;
    private TextInputEditText etName, etEmail, etBio, etCurrentPassword, etNewPassword;
    private MaterialButton btnUpdate, btnPickPhoto, btnRemovePhoto;
    private ProgressBar progressBar;
    private View avatarFrame;
    private ShapeableImageView ivAvatarPhoto;
    private TextView tvAvatarEmoji, tvProfileName, tvProfileEmail;
    private LinearLayout emojiRow, accentRow;

    private SessionManager sessionManager;
    private Repository repo;

    private String pickedEmoji;
    private String pickedAccent;
    private String currentAvatarPath;
    private Bitmap pendingPhotoBitmap;
    private boolean photoMarkedForRemoval = false;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(null);

        sessionManager = new SessionManager(this);
        repo = Repository.get(this);
        initViews();

        etName.setText(sessionManager.getUserName());
        etEmail.setText(sessionManager.getUserEmail());
        etBio.setText(sessionManager.getBio());

        pickedEmoji  = sessionManager.getAvatarEmoji();
        pickedAccent = sessionManager.getAccentColor();
        currentAvatarPath = sessionManager.getAvatarPath();

        tvProfileName.setText(safeOr(sessionManager.getUserName(), "My Profile"));
        tvProfileEmail.setText(safeOr(sessionManager.getUserEmail(), ""));

        buildEmojiPicker();
        buildAccentPicker();
        refreshAvatarPreview();

        btnUpdate.setOnClickListener(v -> updateProfile());
        btnPickPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnRemovePhoto.setOnClickListener(v -> {
            pendingPhotoBitmap = null;
            photoMarkedForRemoval = true;
            currentAvatarPath = null;
            refreshAvatarPreview();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        BottomNavHelper.wire(this, bottomNav, R.id.nav_profile);
    }

    private void initViews() {
        tilName            = findViewById(R.id.tilName);
        tilEmail           = findViewById(R.id.tilEmail);
        tilBio             = findViewById(R.id.tilBio);
        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword     = findViewById(R.id.tilNewPassword);
        etName             = findViewById(R.id.etName);
        etEmail            = findViewById(R.id.etEmail);
        etBio              = findViewById(R.id.etBio);
        etCurrentPassword  = findViewById(R.id.etCurrentPassword);
        etNewPassword      = findViewById(R.id.etNewPassword);
        btnUpdate          = findViewById(R.id.btnUpdate);
        btnPickPhoto       = findViewById(R.id.btnPickPhoto);
        btnRemovePhoto     = findViewById(R.id.btnRemovePhoto);
        progressBar        = findViewById(R.id.progressBar);
        avatarFrame        = findViewById(R.id.avatarFrame);
        ivAvatarPhoto      = findViewById(R.id.ivAvatarPhoto);
        tvAvatarEmoji      = findViewById(R.id.tvAvatarEmoji);
        tvProfileName      = findViewById(R.id.tvProfileName);
        tvProfileEmail     = findViewById(R.id.tvProfileEmail);
        emojiRow           = findViewById(R.id.emojiRow);
        accentRow          = findViewById(R.id.accentRow);
    }

    private void buildEmojiPicker() {
        emojiRow.removeAllViews();
        int chip = dp(48);
        int margin = dp(6);
        for (String emoji : EMOJI_CHOICES) {
            TextView t = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(chip, chip);
            lp.setMargins(0, 0, margin, 0);
            t.setLayoutParams(lp);
            t.setGravity(android.view.Gravity.CENTER);
            t.setText(emoji);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            t.setBackground(circleBackground(emoji.equals(pickedEmoji)));
            t.setOnClickListener(v -> {
                pickedEmoji = emoji.equals(pickedEmoji) ? null : emoji;
                refreshAvatarPreview();
                refreshEmojiChips();
            });
            emojiRow.addView(t);
        }
    }

    private void refreshEmojiChips() {
        for (int i = 0; i < emojiRow.getChildCount(); i++) {
            View v = emojiRow.getChildAt(i);
            v.setBackground(circleBackground(EMOJI_CHOICES[i].equals(pickedEmoji)));
        }
    }

    private android.graphics.drawable.Drawable circleBackground(boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(getColor(R.color.surface_alt));
        if (selected) {
            d.setStroke(dp(2), accentInt());
        } else {
            d.setStroke(dp(1), getColor(R.color.divider));
        }
        return d;
    }

    private void buildAccentPicker() {
        accentRow.removeAllViews();
        int swatch = dp(40);
        int margin = dp(8);
        for (String hex : ACCENT_HEX) {
            View v = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatch, swatch);
            lp.setMargins(0, 0, margin, 0);
            v.setLayoutParams(lp);
            v.setBackground(accentSwatch(hex, hex.equalsIgnoreCase(pickedAccent)));
            v.setOnClickListener(view -> {
                pickedAccent = hex;
                refreshAvatarPreview();
                refreshAccentSwatches();
            });
            accentRow.addView(v);
        }
    }

    private void refreshAccentSwatches() {
        for (int i = 0; i < accentRow.getChildCount(); i++) {
            View v = accentRow.getChildAt(i);
            v.setBackground(accentSwatch(ACCENT_HEX[i], ACCENT_HEX[i].equalsIgnoreCase(pickedAccent)));
        }
    }

    private android.graphics.drawable.Drawable accentSwatch(String hex, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(Color.parseColor(hex));
        d.setStroke(dp(selected ? 3 : 1),
                selected ? getColor(R.color.text_primary) : getColor(R.color.divider));
        return d;
    }

    private void refreshAvatarPreview() {
        avatarFrame.setBackgroundTintList(ColorStateList.valueOf(accentInt()));

        if (pendingPhotoBitmap != null) {
            ivAvatarPhoto.setImageBitmap(pendingPhotoBitmap);
            ivAvatarPhoto.setVisibility(View.VISIBLE);
            tvAvatarEmoji.setVisibility(View.GONE);
            return;
        }
        if (currentAvatarPath != null && !currentAvatarPath.isEmpty() && !photoMarkedForRemoval) {
            Bitmap bmp = BitmapFactory.decodeFile(currentAvatarPath);
            if (bmp != null) {
                ivAvatarPhoto.setImageBitmap(bmp);
                ivAvatarPhoto.setVisibility(View.VISIBLE);
                tvAvatarEmoji.setVisibility(View.GONE);
                return;
            }
        }
        ivAvatarPhoto.setVisibility(View.GONE);
        tvAvatarEmoji.setVisibility(View.VISIBLE);
        tvAvatarEmoji.setText(pickedEmoji != null && !pickedEmoji.isEmpty()
                ? pickedEmoji
                : initialFor(sessionManager.getUserName()));
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) return;
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            if (in != null) in.close();
            if (bmp == null) {
                Toast.makeText(this, "Couldn't read that image", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap scaled = scaleToMax(bmp, 512);
            pendingPhotoBitmap = scaled;
            photoMarkedForRemoval = false;
            refreshAvatarPreview();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't read that image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap scaleToMax(Bitmap src, int max) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= max && h <= max) return src;
        float r = (float) max / Math.max(w, h);
        return Bitmap.createScaledBitmap(src, Math.round(w * r), Math.round(h * r), true);
    }

    private void updateProfile() {
        String name            = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email           = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String bio             = etBio.getText() != null ? etBio.getText().toString().trim() : "";
        String currentPassword = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString().trim() : "";
        String newPassword     = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";

        tilName.setError(null); tilEmail.setError(null); tilBio.setError(null);
        tilCurrentPassword.setError(null); tilNewPassword.setError(null);

        if (name.isEmpty())  { tilName.setError(getString(R.string.error_name_required)); etName.requestFocus(); return; }
        if (email.isEmpty()) { tilEmail.setError(getString(R.string.error_email_required)); etEmail.requestFocus(); return; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email)); etEmail.requestFocus(); return;
        }
        if (!newPassword.isEmpty() && currentPassword.isEmpty()) {
            tilCurrentPassword.setError("Current password is required to change password");
            etCurrentPassword.requestFocus(); return;
        }
        if (!newPassword.isEmpty() && newPassword.length() < 6) {
            tilNewPassword.setError(getString(R.string.error_password_short));
            etNewPassword.requestFocus(); return;
        }

        showLoading(true);

        long userId = sessionManager.getUserId();

        Runnable doProfileUpdate = () -> repo.updateProfile(userId, name, email, bio,
                pickedEmoji, pickedAccent, currentPassword, newPassword,
                new Callback<UserEntity>() {
                    @Override public void onSuccess(UserEntity u) {
                        showLoading(false);
                        Toast.makeText(UserProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                        sessionManager.updateProfile(u.name, u.email,
                                u.avatarEmoji, u.avatarPath, u.accentColor, u.bio);
                        tvProfileName.setText(u.name);
                        tvProfileEmail.setText(u.email);
                        etCurrentPassword.setText("");
                        etNewPassword.setText("");
                    }
                    @Override public void onError(String message) {
                        showLoading(false);
                        Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });

        if (pendingPhotoBitmap != null) {
            repo.saveAvatar(userId, pendingPhotoBitmap, new Callback<String>() {
                @Override public void onSuccess(String path) {
                    currentAvatarPath = path;
                    sessionManager.setAvatarPath(path);
                    pendingPhotoBitmap = null;
                    doProfileUpdate.run();
                }
                @Override public void onError(String message) {
                    showLoading(false);
                    Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else if (photoMarkedForRemoval && sessionManager.getAvatarPath() != null) {
            repo.deleteAvatar(userId, new Callback<Void>() {
                @Override public void onSuccess(Void result) {
                    currentAvatarPath = null;
                    sessionManager.setAvatarPath(null);
                    photoMarkedForRemoval = false;
                    doProfileUpdate.run();
                }
                @Override public void onError(String message) {
                    showLoading(false);
                    Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            doProfileUpdate.run();
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnUpdate.setEnabled(!isLoading);
        btnPickPhoto.setEnabled(!isLoading);
        btnRemovePhoto.setEnabled(!isLoading);
    }

    private int accentInt() {
        if (pickedAccent != null && !pickedAccent.isEmpty()) {
            try { return Color.parseColor(pickedAccent); } catch (IllegalArgumentException ignore) {}
        }
        return sessionManager.getAccentColorInt();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private String initialFor(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        return name.trim().substring(0, 1).toUpperCase();
    }

    private String safeOr(String s, String fallback) {
        return (s == null || s.isEmpty()) ? fallback : s;
    }
}
