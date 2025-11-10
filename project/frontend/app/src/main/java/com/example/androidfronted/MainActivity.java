package com.example.androidfronted;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 主页面，包含底部导航栏
 * - 点击“我的”跳转到 ProfileFragment
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 先只加载一个空 Fragment，避免 HomeFragment 不存在
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new Fragment()) // 临时空 Fragment
                    .commit();
        }
    }
}