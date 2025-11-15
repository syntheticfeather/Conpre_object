package com.example.androidfronted;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 主页 Fragment
 * - 加载 fragment_home.xml 布局
 * - 监听“了解详情”按钮点击事件，跳转至产品详情页
 */
public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View learnMoreButton = view.findViewById(R.id.btn_learn_more);
        if (learnMoreButton != null) {
            learnMoreButton.setOnClickListener(v -> {
                // 启动 ProductDetailActivity
                Intent intent = new Intent(getActivity(), ProductDetailActivity.class);
                // 确保 getActivity() 不为 null（Fragment 已 attached）
                if (getActivity() != null) {
                    startActivity(intent);
                }
            });
        }
    }
}