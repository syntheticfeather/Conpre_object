package com.example.androidfronted;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 验证码登录 Fragment
 * - 点击“去注册”跳转到 RegisterStep1Activity
 * - 输入非空后点击“立即登录”跳转到 MainActivity
 */
public class VerifyCodeLoginFragment extends Fragment {

    private EditText etPhone, etVerifyCode;
    private Button btnLogin;
    private LinearLayout registerLink;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verify_code_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // 初始化视图
        etPhone = view.findViewById(R.id.etPhone);
        etVerifyCode = view.findViewById(R.id.etVerifyCode);
        btnLogin = view.findViewById(R.id.btnLogin);
        registerLink = view.findViewById(R.id.registerLink);

        // 设置“去注册”点击事件
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RegisterStep1Activity.class);
            startActivity(intent);
        });

        // 设置“立即登录”点击事件
        btnLogin.setOnClickListener(v -> handleLogin());
    }

    /**
     * 处理登录逻辑：手机号和验证码非空即跳转主页
     */
    private void handleLogin() {
        String phone = etPhone.getText().toString().trim();
        String code = etVerifyCode.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            setError(etPhone, true);
            Toast.makeText(getContext(), "请输入手机号", Toast.LENGTH_SHORT).show();
            return;
        } else {
            setError(etPhone, false);
        }

        if (TextUtils.isEmpty(code)) {
            setError(etVerifyCode, true);
            Toast.makeText(getContext(), "请输入验证码", Toast.LENGTH_SHORT).show();
            return;
        } else {
            setError(etVerifyCode, false);
        }

        // 登录成功，跳转主页
        Toast.makeText(getContext(), "登录成功！", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getActivity(), MainActivity.class));
        getActivity().finish();
    }

    /**
     * 设置输入框错误状态样式
     */
    private void setError(EditText et, boolean error) {
        if (error) {
            et.setBackgroundResource(R.drawable.edittext_bg_error);
            et.setHintTextColor(Color.RED);
        } else {
            et.setBackgroundResource(R.drawable.edittext_bg);
            et.setHintTextColor(Color.GRAY);
        }
    }
}