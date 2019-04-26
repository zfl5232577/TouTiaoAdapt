package com.mark.toutiaoadapt;

import android.support.v7.app.AppCompatActivity;

import com.mark.toutiaoadapt.screenadapt.TouTiaoAdapter;

/**
 * Copyright (C), 2018-2019, 奥昇科技有限公司
 * ClassName    : BaseActivity
 * Author       : Mark
 * Email        : makun.cai@aorise.org
 * CreateDate   : 2019/4/26 13:43
 * Description  : TODO
 * Version      : 1.0
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void setContentView(int layoutResID) {
        TouTiaoAdapter.getInstance(getApplication()).autoConvertDensity(this,640,true);
        super.setContentView(layoutResID);
    }
}
