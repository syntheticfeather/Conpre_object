package com.example.androidfronted;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

/**
 * 产品详情页面 Activity
 * - 加载 product_detail.xml 布局
 * - 点击左上角返回按钮，关闭当前页面，返回到 MainActivity（主页）
 */
public class ProductDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置布局为产品详情页
        setContentView(R.layout.product_detail);

        View backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                finish();
            });
        }
    }
}
