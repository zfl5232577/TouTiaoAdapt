package com.mark.toutiaoadapt;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright (C), 2018-2019, 奥昇科技有限公司
 * ClassName    : TouTiaoAdapter
 * Author       : Mark
 * Email        : makun.cai@aorise.org
 * CreateDate   : 2019/4/26 13:43
 * Description  : TODO
 * Version      : 1.0
 */
public class TouTiaoAdapter {

    private static TouTiaoAdapter mInstance;

    private static Map<String, DisplayMetricsInfo> mCache = new ConcurrentHashMap<>();

    /**
     * 用来管理 AndroidAutoSize 支持的所有单位, AndroidAutoSize 支持五种单位 (dp、sp、pt、in、mm)
     */
    private UnitsManager mUnitsManager = new UnitsManager();
    /**
     * 最初的 {@link DisplayMetrics#density}
     */
    private float mInitDensity = -1;
    /**
     * 最初的 {@link DisplayMetrics#densityDpi}
     */
    private int mInitDensityDpi;
    /**
     * 最初的 {@link DisplayMetrics#scaledDensity}
     */
    private float mInitScaledDensity;
    /**
     * 最初的 {@link DisplayMetrics#xdpi}
     */
    private float mInitXdpi;

    /**
     * 此字段表示是否使用设备的实际尺寸做适配
     * {@link #isUseDeviceSize} 为 {@code true} 表示屏幕高度 {@link #mScreenHeight} 包含状态栏的高度
     * {@link #isUseDeviceSize} 为 {@code false} 表示 {@link #mScreenHeight} 会减去状态栏的高度, 默认为 {@code true}
     */
    private boolean isUseDeviceSize = true;

    /**
     * 是否屏蔽系统字体大小对 AndroidAutoSize 的影响, 如果为 {@code true}, App 内的字体的大小将不会跟随系统设置中字体大小的改变
     * 如果为 {@code false}, 则会跟随系统设置中字体大小的改变, 默认为 {@code false}
     */
    private boolean isExcludeFontScale;

    /**
     * 设备的屏幕总宽度, 单位 px
     */
    private int mScreenWidth;
    /**
     * 设备的屏幕总高度, 单位 px, 如果 {@link #isUseDeviceSize} 为 {@code false}, 屏幕总高度会减去状态栏的高度
     */
    private int mScreenHeight;
    /**
     * 状态栏高度, 当 {@link #isUseDeviceSize} 为 {@code false} 时, AndroidAutoSize 会将 {@link #mScreenHeight} 减去状态栏高度
     */
    private int mStatusBarHeight;

    /**
     * 是否是 Miui 系统
     */
    private boolean isMiui;
    /**
     * Miui 系统中的 mTmpMetrics 字段
     */
    private Field mTmpMetricsField;

    private TouTiaoAdapter(Context context) {
        int[] screenSize = ScreenUtils.getScreenSize(context);
        mScreenWidth = screenSize[0];
        mScreenHeight = screenSize[1];
        mStatusBarHeight = ScreenUtils.getStatusBarHeight();
        final DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        mInitDensity = displayMetrics.density;
        mInitDensityDpi = displayMetrics.densityDpi;
        mInitScaledDensity = displayMetrics.scaledDensity;
        mInitXdpi = displayMetrics.xdpi;
        mUnitsManager.setSupportDP(false).setSupportDP(false).setSupportSubunits(Subunits.MM);

        if ("MiuiResources".equals(context.getResources().getClass().getSimpleName()) || "XResources".equals(context.getResources().getClass().getSimpleName())) {
            isMiui = true;
            try {
                mTmpMetricsField = Resources.class.getDeclaredField("mTmpMetrics");
                mTmpMetricsField.setAccessible(true);
            } catch (Exception e) {
                mTmpMetricsField = null;
            }
        }
    }

    public static TouTiaoAdapter getInstance(Context context) {
        if (mInstance == null) {
            synchronized (TouTiaoAdapter.class) {
                if (mInstance == null) {
                    mInstance = new TouTiaoAdapter(context);
                }
            }
        }
        return mInstance;
    }

    /**
     * 这里是今日头条适配方案的核心代码, 核心在于根据当前设备的实际情况做自动计算并转换 {@link DisplayMetrics#density}、
     * {@link DisplayMetrics#scaledDensity}、{@link DisplayMetrics#densityDpi} 这三个值, 额外增加 {@link DisplayMetrics#xdpi}
     * 以支持单位 {@code pt}、{@code in}、{@code mm}
     *
     * @param activity      {@link Activity}
     * @param sizeInDp      设计图上的设计尺寸, 单位 dp, 如果 {@param isBaseOnWidth} 设置为 {@code true},
     *                      {@param sizeInDp} 则应该填写设计图的总宽度, 如果 {@param isBaseOnWidth} 设置为 {@code false},
     *                      {@param sizeInDp} 则应该填写设计图的总高度
     * @param isBaseOnWidth 是否按照宽度进行等比例适配, {@code true} 为以宽度进行等比例适配, {@code false} 为以高度进行等比例适配
     * @see <a href="https://mp.weixin.qq.com/s/d9QCoBP6kV9VSWvVldVVwA">今日头条官方适配方案</a>
     */
    public void autoConvertDensity(Activity activity, float sizeInDp, boolean isBaseOnWidth) {

        float subunitsDesignSize = sizeInDp;
        int screenSize = isBaseOnWidth ? mScreenWidth
                : isUseDeviceSize ? mScreenHeight : mScreenHeight - mStatusBarHeight;
        String key = sizeInDp + "|" + subunitsDesignSize + "|" + isBaseOnWidth + "|"
                + isUseDeviceSize + "|"
                + mInitScaledDensity + "|"
                + screenSize;

        DisplayMetricsInfo displayMetricsInfo = mCache.get(key);

        float targetDensity = 0;
        int targetDensityDpi = 0;
        float targetScaledDensity = 0;
        float targetXdpi = 0;

        if (displayMetricsInfo == null) {
            targetDensity = screenSize * 1.0f / sizeInDp;
            float scale = isExcludeFontScale ? 1 : mInitScaledDensity * 1.0f / mInitDensity;
            targetScaledDensity = targetDensity * scale;
            targetDensityDpi = (int) (targetDensity * 160);
            targetXdpi = screenSize * 1.0f / subunitsDesignSize;
            mCache.put(key, new DisplayMetricsInfo(targetDensity, targetDensityDpi, targetScaledDensity, targetXdpi));
        } else {
            targetDensity = displayMetricsInfo.getDensity();
            targetDensityDpi = displayMetricsInfo.getDensityDpi();
            targetScaledDensity = displayMetricsInfo.getScaledDensity();
            targetXdpi = displayMetricsInfo.getXdpi();
        }
        setDensity(activity, targetDensity, targetDensityDpi, targetScaledDensity, targetXdpi);
    }

    /**
     * 给几大 {@link DisplayMetrics} 赋值
     *
     * @param activity      {@link Activity}
     * @param density       {@link DisplayMetrics#density}
     * @param densityDpi    {@link DisplayMetrics#densityDpi}
     * @param scaledDensity {@link DisplayMetrics#scaledDensity}
     * @param xdpi          {@link DisplayMetrics#xdpi}
     */
    private void setDensity(Activity activity, float density, int densityDpi, float scaledDensity, float xdpi) {
        //兼容 MIUI
        DisplayMetrics activityDisplayMetricsOnMIUI = getMetricsOnMiui(activity.getResources());
        DisplayMetrics appDisplayMetricsOnMIUI = getMetricsOnMiui(activity.getApplication().getResources());

        if (activityDisplayMetricsOnMIUI != null) {
            setDensity(activityDisplayMetricsOnMIUI, density, densityDpi, scaledDensity, xdpi);
        } else {
            DisplayMetrics activityDisplayMetrics = activity.getResources().getDisplayMetrics();
            setDensity(activityDisplayMetrics, density, densityDpi, scaledDensity, xdpi);
        }

        if (appDisplayMetricsOnMIUI != null) {
            setDensity(appDisplayMetricsOnMIUI, density, densityDpi, scaledDensity, xdpi);
        } else {
            DisplayMetrics appDisplayMetrics = activity.getApplication().getResources().getDisplayMetrics();
            setDensity(appDisplayMetrics, density, densityDpi, scaledDensity, xdpi);
        }
    }

    /**
     * 赋值
     *
     * @param displayMetrics {@link DisplayMetrics}
     * @param density        {@link DisplayMetrics#density}
     * @param densityDpi     {@link DisplayMetrics#densityDpi}
     * @param scaledDensity  {@link DisplayMetrics#scaledDensity}
     * @param xdpi           {@link DisplayMetrics#xdpi}
     */
    private void setDensity(DisplayMetrics displayMetrics, float density, int densityDpi, float scaledDensity, float xdpi) {
        if (mUnitsManager.isSupportDP()) {
            displayMetrics.density = density;
            displayMetrics.densityDpi = densityDpi;
        }
        if (mUnitsManager.isSupportSP()) {
            displayMetrics.scaledDensity = scaledDensity;
        }
        switch (mUnitsManager.getSupportSubunits()) {
            case NONE:
                break;
            case PT:
                displayMetrics.xdpi = xdpi * 72f;
                break;
            case IN:
                displayMetrics.xdpi = xdpi;
                break;
            case MM:
                displayMetrics.xdpi = xdpi * 25.4f;
                break;
            default:
        }
    }

    public UnitsManager getmUnitsManager() {
        return mUnitsManager;
    }

    /**
     * 解决 MIUI 更改框架导致的 MIUI7 + Android5.1.1 上出现的失效问题 (以及极少数基于这部分 MIUI 去掉 ART 然后置入 XPosed 的手机)
     * 来源于: https://github.com/Firedamp/Rudeness/blob/master/rudeness-sdk/src/main/java/com/bulong/rudeness/RudenessScreenHelper.java#L61:5
     *
     * @param resources {@link Resources}
     * @return {@link DisplayMetrics}, 可能为 {@code null}
     */
    private  DisplayMetrics getMetricsOnMiui(Resources resources) {
        if (isMiui && mTmpMetricsField != null) {
            try {
                return (DisplayMetrics) mTmpMetricsField.get(resources);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public boolean isUseDeviceSize() {
        return isUseDeviceSize;
    }

    public void setUseDeviceSize(boolean useDeviceSize) {
        isUseDeviceSize = useDeviceSize;
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public void setScreenWidth(int mScreenWidth) {
        this.mScreenWidth = mScreenWidth;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    public void setScreenHeight(int mScreenHeight) {
        this.mScreenHeight = mScreenHeight;
    }

    public int getStatusBarHeight() {
        return mStatusBarHeight;
    }

    public boolean isExcludeFontScale() {
        return isExcludeFontScale;
    }

    public void setExcludeFontScale(boolean excludeFontScale) {
        isExcludeFontScale = excludeFontScale;
    }

}
