package com.example.androidfronted;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 注册第二步：输入手机号和验证码
 * - 点击“上一步”返回 RegisterStep1Activity
 * - 点击“立即注册”跳转到 MainActivity
 */
public class RegisterStep2Activity extends AppCompatActivity {

    private EditText etPhone, etVerifyCode;
    private TextView tvBackStep;
    private Button btnRegister;
    private TextView tvRegisterHint; // 新增提示文本

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_register_step2);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etPhone = findViewById(R.id.etPhone);
        etVerifyCode = findViewById(R.id.etVerifyCode);
        tvBackStep = findViewById(R.id.tvBackStep);
        btnRegister = findViewById(R.id.btnRegister);
        tvRegisterHint = findViewById(R.id.tvRegisterHint); // 新增
    }

    private void setupClickListeners() {
        // 上一步
        tvBackStep.setOnClickListener(v -> {
            finish(); // 返回上一页
        });

        // 立即注册
        btnRegister.setOnClickListener(v -> completeRegistration());
    }

    /**
     * 完成注册：手机号和验证码非空即成功
     */
    private void completeRegistration() {
        String phone = etPhone.getText().toString().trim();
        String code = etVerifyCode.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            tvRegisterHint.setText("请输入手机号");
            return;
        }

        if (TextUtils.isEmpty(code)) {
            tvRegisterHint.setText("请输入验证码");
            return;
        }

        tvRegisterHint.setText(""); // 清空提示

        String username = getIntent().getStringExtra("USERNAME");
        Toast.makeText(this, "注册成功！欢迎，" + username, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}