package com.example.androidfronted;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 注册第一步：输入用户名和密码
 * - 点击“下一步”跳转到 RegisterStep2Activity
 * - 点击“去登录”跳转回 LoginActivity
 */
public class RegisterStep1Activity extends AppCompatActivity {

    private EditText etUsername, etPassword, etConfirmPassword;
    private ImageView ivTogglePassword, ivToggleConfirm;
    private Button btnNext;
    private boolean isPasswordVisible = false;
    private boolean isConfirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_register_step1);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirm = findViewById(R.id.ivToggleConfirmPassword);
        btnNext = findViewById(R.id.btnNext);
    }

    private void setupClickListeners() {
        // 眼睛图标切换密码可见性
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        ivToggleConfirm.setOnClickListener(v -> toggleConfirmPasswordVisibility());

        // 下一步按钮
        btnNext.setOnClickListener(v -> goToStep2());

        // “去登录”链接（LinearLayout）
        findViewById(R.id.loginLink).setOnClickListener(v -> {
            finish(); // 直接返回登录页
        });
    }

    private void togglePasswordVisibility() {
        toggleVisibility(etPassword, ivTogglePassword, () -> isPasswordVisible = !isPasswordVisible, () -> isPasswordVisible);
    }

    private void toggleConfirmPasswordVisibility() {
        toggleVisibility(etConfirmPassword, ivToggleConfirm, () -> isConfirmVisible = !isConfirmVisible, () -> isConfirmVisible);
    }

    private void toggleVisibility(EditText et, ImageView iv, Runnable toggleFlag, java.util.concurrent.Callable<Boolean> getFlag) {
        try {
            if (getFlag.call()) {
                et.setTransformationMethod(PasswordTransformationMethod.getInstance());
                iv.setImageResource(R.drawable.ic_eye_off);
            } else {
                et.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                iv.setImageResource(R.drawable.ic_eye_on);
            }
            toggleFlag.run();
            et.setSelection(et.getText().length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 验证并跳转到第二步
     */
    private void goToStep2() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, RegisterStep2Activity.class);
        intent.putExtra("USERNAME", username);
        intent.putExtra("PASSWORD", password);
        startActivity(intent);
    }
}