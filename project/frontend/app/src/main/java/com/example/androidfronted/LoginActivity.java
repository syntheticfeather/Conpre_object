package com.example.androidfronted;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录主页面
 * - 默认显示验证码登录（fragment_verify_code_login）
 * - 点击“密码登录”切换到密码登录界面
 */
public class LoginActivity extends AppCompatActivity {

    private List<Fragment> fragments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 添加两个 Fragment
        fragments.add(new VerifyCodeLoginFragment());
        fragments.add(new PasswordLoginFragment());

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.viewPager, fragments.get(0), "verify")
                .add(R.id.viewPager, fragments.get(1), "password")
                .hide(fragments.get(1))
                .commit();

        setupTabs();
    }

    private void setupTabs() {
        findViewById(R.id.tabVerifyCode).setOnClickListener(v -> switchTab(0));
        findViewById(R.id.tabPassword).setOnClickListener(v -> switchTab(1));
        selectTab(0);
    }

    private void switchTab(int index) {
        getSupportFragmentManager().beginTransaction()
                .hide(fragments.get(0))
                .hide(fragments.get(1))
                .show(fragments.get(index))
                .commit();
        selectTab(index);
    }

    private void selectTab(int index) {
        findViewById(R.id.tabVerifyCode).setSelected(index == 0);
        findViewById(R.id.tabPassword).setSelected(index == 1);
    }
}