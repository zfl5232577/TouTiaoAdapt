package com.mark.toutiaoadapt;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 跳转到 {@link FragmentHostActivity}, 展示项目内部的 {@link Fragment} 自定义适配参数的用法
     *
     * @param view {@link View}
     */
    public void goCustomAdaptFragment(View view) {
        startActivity(new Intent(getApplicationContext(), FragmentHostActivity.class));
    }
}
