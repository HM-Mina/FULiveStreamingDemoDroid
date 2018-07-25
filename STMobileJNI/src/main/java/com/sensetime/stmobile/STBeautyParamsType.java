package com.sensetime.stmobile;

/**
 * 定义可以美颜的类型
 */
public class STBeautyParamsType {
    public final static int ST_BEAUTIFY_REDDEN_STRENGTH = 1; // 红润强度, [0,1.0], 0.0不做红润
    public final static int ST_BEAUTIFY_SMOOTH_STRENGTH = 3; // 磨皮强度, [0,1.0], 0.0不做磨皮
    public final static int ST_BEAUTIFY_WHITEN_STRENGTH = 4;    /// 美白强度, [0,1.0], 0.0不做美白
    public final static int ST_BEAUTIFY_ENLARGE_EYE_RATIO = 5;    /// 大眼比例, [0,1.0], 0.0不做大眼效果
    public final static int ST_BEAUTIFY_SHRINK_FACE_RATIO = 6;    /// 瘦脸比例, [0,1.0], 0.0不做瘦脸效果
    public final static int ST_BEAUTIFY_SHRINK_JAW_RATIO = 7;     /// 小脸比例, [0,1.0], 0.0不做小脸效果
}
