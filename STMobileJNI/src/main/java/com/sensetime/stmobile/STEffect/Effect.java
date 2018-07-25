package com.sensetime.stmobile.STEffect;

import android.content.Context;
import android.opengl.GLES20;

import com.sensetime.stmobile.STBeautifyNative;
import com.sensetime.stmobile.STBeautyParamsType;
import com.sensetime.stmobile.STCommon;
import com.sensetime.stmobile.STFilterParamsType;
import com.sensetime.stmobile.STHumanActionParamsType;
import com.sensetime.stmobile.STMobileFaceAttributeNative;
import com.sensetime.stmobile.STMobileHumanActionNative;
import com.sensetime.stmobile.STMobileStickerNative;
import com.sensetime.stmobile.STMobileStreamFilterNative;
import com.sensetime.stmobile.STRotateType;
import com.sensetime.stmobile.model.STHumanAction;

import javax.microedition.khronos.opengles.GL10;

/** 商汤滤镜逻辑处理类
 * Created by hzzhujinbo on 2017/7/11.
 */

public class Effect {

    private String TAG = "Effect";
    private Context mContext;
    private String mCurrentSticker;
    private String mCurrentFilterStyle;
    private float mCurrentFilterStrength = 0.5f;//阈值为[0,1]
    private float mFilterStrength = 0.5f;
    private String mFilterStyle;


    private STMobileStickerNative mStStickerNative = new STMobileStickerNative();
    private STBeautifyNative mStBeautifyNative = new STBeautifyNative(); //美颜参数，用户可以根据需要自己调节
    private STMobileHumanActionNative mSTHumanActionNative = new STMobileHumanActionNative();
    private STHumanAction mHumanActionBeautyOutput = new STHumanAction();
    private STMobileStreamFilterNative mSTMobileStreamFilterNative = new STMobileStreamFilterNative(); //滤镜参数，用户可以根据需要自己调节
    private STMobileFaceAttributeNative mSTFaceAttributeNative = new STMobileFaceAttributeNative();
//    private STMobileObjectTrackNative mSTMobileObjectTrackNative = new STMobileObjectTrackNative();

    private float[] mBeautifyParams = {0.36f, 0.74f, 0.30f, 0.13f, 0.11f, 0.1f};
    private static int[] beautyTypes = {
            STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH,
            STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH,
            STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH,
            STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO,
            STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO,
            STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO
    };

    private boolean mIsCreateHumanActionHandleSucceeded = false;
    private final Object mHumanActionHandleLock = new Object();

    private int mImageWidth;
    private int mImageHeight;
    private boolean mNeedBeautify = false;
    private boolean mNeedFilter = false;
    private boolean mNeedFaceAttribute = false;
    private boolean mNeedSticker;
    private long mDetectConfig = 0;
//    private STGLRender mGLRender;
    private Accelerometer mAccelerometer;

    private int[] mStickerTextureId;
    private int[] mBeautifyTextureId;
    private int[] mFilterTextureOutId;

    private boolean mInited = false;


    public Effect(Context context){
        FileUtils.copyModelFiles(context);
        mContext = context;

        //初始化非OpengGL相关的句柄，包括人脸检测及人脸属性
        initHumanAction(); //因为人脸模型加载较慢，建议异步调用
        initFaceAttribute();
        initObjectTrack();
        mAccelerometer = new Accelerometer(context);
        mAccelerometer.start();
    }

    public void release(){
        destory();
    }

    public void enableBeautify(boolean needBeautify) {
        mNeedBeautify = needBeautify;
        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
    }

    public void enableFaceAttribute(boolean needFaceAttribute) {
        mNeedFaceAttribute = needFaceAttribute;
        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
    }

    public void enableFilter(boolean needFilter){
        mNeedFilter = needFilter;
    }

    public void setFilterStyle(String modelPath) {
        mFilterStyle = modelPath;
    }

    public void setFilterStrength(float strength){
        mFilterStrength = strength;
    }


    public void setBeautifyParam(int type, float value) {
        mStBeautifyNative.setParam(type,value);
    }

    private NeteaseGLUtil mNeteaseGLUtil;
    public int effect(int cameraTextureId,byte[] data, final int width,final int height,int orientation){


        if((mImageWidth != 0 && mImageWidth != width) || (mImageHeight != 0 && mImageHeight != height)){
            if (mNeteaseGLUtil != null) {
                mNeteaseGLUtil.release();
                mNeteaseGLUtil = null;
            }
            mBeautifyTextureId = null;
            mStickerTextureId = null;
            mFilterTextureOutId = null;
        }
        mImageWidth = width;
        mImageHeight = height;

        if(mNeteaseGLUtil == null){
            mNeteaseGLUtil = new NeteaseGLUtil();
            mNeteaseGLUtil.init(width, height);
        }

        if(!mInited){
            initGLEffect();
        }

        if(mNeteaseGLUtil == null){
            return -1;
        }

        if (mBeautifyTextureId == null) {
            mBeautifyTextureId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mBeautifyTextureId,
                    GLES20.GL_TEXTURE_2D);
        }

        if (mStickerTextureId == null) {
            mStickerTextureId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mStickerTextureId,
                    GLES20.GL_TEXTURE_2D);
        }

        if(mFilterTextureOutId == null){
            mFilterTextureOutId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mFilterTextureOutId, GLES20.GL_TEXTURE_2D);
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        int textureId = mNeteaseGLUtil.drawOesTexture2Texture2D(cameraTextureId,width,height);
        int result;
        if ((mNeedBeautify || mNeedSticker || mNeedFilter) && mIsCreateHumanActionHandleSucceeded) {
            int currentOrientation = getHumanActionOrientation(orientation);
            STHumanAction humanAction = mSTHumanActionNative.humanActionDetect(data,
                    STCommon.ST_PIX_FMT_NV21, mDetectConfig, currentOrientation, mImageWidth,
                    mImageHeight);

            // 磨皮
            if (mNeedBeautify) {
                result = mStBeautifyNative.processTexture(textureId, mImageWidth, mImageHeight,
                        humanAction, mBeautifyTextureId[0], mHumanActionBeautyOutput);
                if (result == 0) {
                    textureId = mBeautifyTextureId[0];
                }
            }

            // 贴纸
            if (mNeedSticker) {
                result = mStStickerNative.processTexture(textureId, humanAction, currentOrientation,
                        mImageWidth, mImageHeight,currentOrientation, false, mStickerTextureId[0]);
                if (result == 0) {
                    textureId = mStickerTextureId[0];
                }
            }

            //滤镜
            if(mCurrentFilterStyle != mFilterStyle){
                mCurrentFilterStyle = mFilterStyle;
                mSTMobileStreamFilterNative.setStyle(mCurrentFilterStyle);
            }
            if(mCurrentFilterStrength != mFilterStrength){
                mCurrentFilterStrength = mFilterStrength;
                mSTMobileStreamFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
            }

            if(mNeedFilter){
                //如果需要输出buffer推流或其他，设置该开关为true
                result = mSTMobileStreamFilterNative.processTexture(textureId, mImageWidth, mImageHeight, mFilterTextureOutId[0]);
                if(result == 0){
                    textureId = mFilterTextureOutId[0];
                }
            }
        }

        GLES20.glViewport(0, 0, width, height);
        return textureId;
    }

    //初始化GL相关的句柄，包括美颜，贴纸，滤镜
    private void initGLEffect(){
        GLES20.glEnable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);
        initBeauty();
        initSticker();
        initFilter();
        mInited = true;
    }

    private void destory(){
        //必须释放非openGL句柄资源,否则内存泄漏
        deleteInternalTextures();

        synchronized (mHumanActionHandleLock)
        {
            mSTHumanActionNative.destroyInstance();
        }
        mSTFaceAttributeNative.destroyInstance();
        mSTMobileStreamFilterNative.destroyInstance();
//        mSTMobileObjectTrackNative.destroyInstance();

        //openGL资源释放
        mStBeautifyNative.destroyBeautify();
        mStStickerNative.destroyInstance();

        if (mNeteaseGLUtil != null) {
            mNeteaseGLUtil.release();
            mNeteaseGLUtil = null;
        }
//
    }

    private void deleteInternalTextures() {
        if (mBeautifyTextureId != null) {
            GLES20.glDeleteTextures(1, mBeautifyTextureId, 0);
            mBeautifyTextureId = null;
        }

        if (mStickerTextureId != null) {
            GLES20.glDeleteTextures(1, mStickerTextureId, 0);
            mStickerTextureId = null;
        }

    }


    public void enableSticker(boolean needSticker) {
        mNeedSticker = needSticker;
    }

    public void setSticker(String sticker) {
        enableSticker(true);
        mCurrentSticker = sticker;
        int result = mStStickerNative.changeSticker(mCurrentSticker);
        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
    }


    private void initFaceAttribute() {
        int result = mSTFaceAttributeNative.createInstanceFromAssetFile(FileUtils.MODEL_NAME_FACE_ATTRIBUTE, mContext.getAssets());
    }

    private void initHumanAction() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mHumanActionHandleLock) {
                    int result = mSTHumanActionNative.createInstanceFromAssetFile(FileUtils.getActionModelName(), STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO, mContext.getAssets());
                    if (result == 0) {
                        mIsCreateHumanActionHandleSucceeded = true;
                        mSTHumanActionNative.setParam(STHumanActionParamsType.ST_HUMAN_ACTION_PARAM_BACKGROUND_BLUR_STRENGTH, 0.35f);
                    }
                }
            }
        }).start();
    }

    private void initSticker() {
        int result = mStStickerNative.createInstance(mContext,null);
        if(mNeedSticker){
            mStStickerNative.changeSticker(mCurrentSticker);
        }
        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
    }

    private void initBeauty() {
        // 初始化beautify,preview的宽高
        int result = mStBeautifyNative.createInstance();
        if (result == 0) {
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH, mBeautifyParams[0]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH, mBeautifyParams[1]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH, mBeautifyParams[2]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO, mBeautifyParams[3]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO, mBeautifyParams[4]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO, mBeautifyParams[5]);
        }
    }


    /**
     * human action detect的配置选项,根据Sticker的TriggerAction和是否需要美颜配置
     *
     * @param needFaceDetect  是否需要开启face detect
     * @param config  sticker的TriggerAction
     */
    private void setHumanActionDetectConfig(boolean needFaceDetect, long config){
        if(needFaceDetect){
            mDetectConfig = config | STMobileHumanActionNative.ST_MOBILE_FACE_DETECT;
        }else{
            mDetectConfig = config;
        }
    }

    private void initFilter(){
        int result = mSTMobileStreamFilterNative.createInstance();
        LogUtils.i(TAG, "filter create instance result %d", result);

        mSTMobileStreamFilterNative.setStyle(mCurrentFilterStyle);
        mCurrentFilterStrength = mFilterStrength;
        mSTMobileStreamFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
    }

    private void initObjectTrack(){
//        int result = mSTMobileObjectTrackNative.createInstance();
    }

    private int getCurrentOrientation() {
        int dir = Accelerometer.getDirection();
        int orientation = dir - 1;
        if (orientation < 0) {
            orientation = dir ^ 3;
        }

        return orientation;
    }

    private int getHumanActionOrientation(int cameraOrientation){
        int deviceOrientation = getCurrentOrientation();
        int humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_270;
        if(deviceOrientation == 0){ //竖屏
            switch (cameraOrientation){
                case 0:
                    humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_0;
                    break;
                case 90:
                    humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_90;
                    break;
                case 180:
                    humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_180;
                    break;
                case 270:
                    humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_270;
                    break;
                default:
                    break;
            }
        }else if(deviceOrientation == 3){ //横屏
            humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_0;
        }else if(deviceOrientation == 1){ //反向横屏
            humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_180;
        }else if(deviceOrientation == 2){ //反向竖屏
            switch (cameraOrientation){
                case 0:
                    humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_180;
                    break;
                case 90:
                    humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_270;
                    break;
                case 180:
                    humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_0;
                    break;
                case 270:
                    humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_90;
                    break;
                default:
                    break;
            }
        }
        return humanOrientation;
    }
}
