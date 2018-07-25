package com.sensetime.stmobile;

import android.content.Context;

import com.sensetime.stmobile.model.STHumanAction;

/**
 * 贴纸渲染JNI类定义
 */
public class STMobileStickerNative {

    //定义trigger action
    public final static int ST_MOBILE_EYE_BLINK = 0x00000002;    ///<  眨眼
    public final static int ST_MOBILE_MOUTH_AH = 0x00000004;    ///<  嘴巴大张
    public final static int ST_MOBILE_HEAD_YAW = 0x00000008;    ///<  摇头
    public final static int ST_MOBILE_HEAD_PITCH = 0x00000010;    ///<  点头
    public final static int ST_MOBILE_BROW_JUMP = 0x00000020;    ///<  眉毛挑动

    enum RenderStatus {
        ST_MATERIAL_BEGIN_RENDER(0), // 开始渲染子素材
        ST_MATERIAL_RENDERING(1),  // 子素材渲染中
        ST_MATERIAL_NO_RENDERING(2); // 子素材不再渲染

        private final int status;

        private RenderStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public static RenderStatus fromStatus(int status) {
            for (RenderStatus type : RenderStatus.values()) {
                if (type.getStatus() == status) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 定义素材处理callback
     */
    public interface ItemCallback {
        /**
         * @param materialName 子素材名
         * @param status       当前子素材渲染的状态 RenderStatus
         */
        void processTextureCallback(String materialName, RenderStatus status);
    }

    private final static String TAG = STMobileStickerNative.class.getSimpleName();
    private static ItemCallback mCallback;

    /**
     * 设置要监听的素材处理callback
     *
     * @param callback 素材处理callback
     */
    public static void setCallback(ItemCallback callback) {
        mCallback = callback;
    }

    /**
     * JNI处理素材时，会回调该函数。
     */
    public static void item_callback(String materialName, int status) {
        if (mCallback != null) {
            mCallback.processTextureCallback(materialName, RenderStatus.fromStatus(status));
        }
    }

    static {
        System.loadLibrary("st_mobile");
        System.loadLibrary("stmobile_jni");
    }

    private long nativeStickerHandle;

    private STSoundPlay mSoundPlay;

    /**
     * 创建贴纸实例
     *
     * @param context 上下文环境，可为null，null时无法使用贴纸声音功能
     * @param zipPath 输入的素材包路径,可以为空
     * @return 成功返回0，错误返回其他，参考STCommon.ResultCode
     */
    public int createInstance(Context context, String zipPath) {
        if(context != null){
            mSoundPlay = new STSoundPlay(context);
        }

        int ret = createInstanceNative(zipPath);

        if(0 == ret && mSoundPlay != null) {
            mSoundPlay.setStickHandle(this);
        }
        return ret;
    }

    /**
     * 销毁实例，必须在opengl环境中运行
     */
    public void destroyInstance() {
        destroyInstanceNative();
        if(mSoundPlay != null){
            mSoundPlay.release();
            mSoundPlay = null;
        }
    }

    /**
     * 创建贴纸实例
     *
     * @param zipPath 输入的素材包路径,可以为空
     * @return 成功返回0，错误返回其他，参考STCommon.ResultCode
     */
    private native int createInstanceNative(String zipPath);


    /**
     * 对OpenGL ES 中的纹理进行贴纸处理，必须在opengl环境中运行，仅支持RGBA图像格式
     *
     * @param textureIn      输入textureid
     * @param humanAction    输入检测到的人脸信息，由STMobileHumanActionNative相关的API获得
     * @param rotate         为使人脸正向，pInputImage需要旋转的角度。比如STRotateType.ST_CLOCKWISE_ROTATE_90
     * @param imageWidth     图像宽度（以像素为单位）
     * @param imageHeight    图像高度（以像素为单位）
     * @param frontStickerRotate 前景贴纸朝向, 0代表106/107点在上, 90代表106/107点在左, 180代表106/107点在下, 270代表106/107点在右
     * @param needsMirroring 传入图像与显示图像是否是镜像关系
     * @param textureOut     处理后的纹理ID，用来做渲染
     * @return 成功返回0，错误返回其他，参考STCommon.ResultCode
     */
    public native int processTexture(int textureIn, STHumanAction humanAction, int rotate, int imageWidth, int imageHeight, int frontStickerRotate, boolean needsMirroring, int textureOut);

    /**
     * 对OpenGL ES 中的纹理进行贴纸处理，必须在opengl环境中运行，仅支持RGBA图像格式.支持buffer输出
     *
     * @param textureIn      输入textureid
     * @param humanAction    输入检测到的人脸信息，由STMobileHumanActionNative相关的API获得
     * @param rotate         为使人脸正向，pInputImage需要旋转的角度。比如STRotateType.ST_CLOCKWISE_ROTATE_90
     * @param imageWidth     图像宽度（以像素为单位）
     * @param imageHeight    图像高度（以像素为单位）
     * @param frontStickerRotate 前景贴纸朝向, 0代表106/107点在上, 90代表106/107点在左, 180代表106/107点在下, 270代表106/107点在右
     * @param needsMirroring 传入图像与显示图像是否是镜像关系
     * @param textureOut     处理后的纹理ID，用来做渲染
     * @param outFmt         输出图像的格式，支持NV21,BGR,BGRA,NV12,RGBA等,比如STCommon.ST_PIX_FMT_NV12。
     * @param imageOut       输出图像的buffer，需要从外部创建
     * @return 成功返回0，错误返回其他，参考STCommon.ResultCode
     */
    public native int processTextureAndOutputBuffer(int textureIn, STHumanAction humanAction, int rotate, int imageWidth, int imageHeight, int frontStickerRotate, boolean needsMirroring, int textureOut, int outFmt, byte[] imageOut);

    /**
     * 切换贴纸路径
     *
     * @param path 贴纸路径。如果输入null，为无贴纸模式
     * @return 成功返回0，错误返回其他，参考STCommon.ResultCode
     */
    public native int changeSticker(String path);

    /**
     * 获取当前贴纸的触发动作
     *
     * @return 触发动作，比如STMobileHumanActionNative.ST_MOBILE_EYE_BLINK,
     * 比如STMobileHumanActionNative.ST_MOBILE_BROW_JUMP等
     */
    public native long getTriggerAction();

    /**
     * 等待素材加载完毕后再渲染，因为会导致切换素材包时画面卡顿，仅建议用于希望等待模型加载完毕再渲染的场景，比如单帧或较短视频的3D绘制等
     * @param needWait 是否等待素材加载完毕后再渲染
     * @return   成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
     */
    public native int setWaitingMaterialLoaded(boolean needWait);

    /**
     * 设置贴纸素材图像所占用的最大内存
     * @param value   贴纸素材图像所占用的最大内存（MB）,默认150MB,素材过大时,循环加载,降低内存； 贴纸较小时,全部加载,降低cpu
     * @return        成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
     */
    public native int setMaxMemory(int value);

    /**
     * 调整最小帧处理间隔
     * @param value  贴纸前后两个序列帧切换的最小时间间隔，单位为毫秒。当两个相机帧处理的间隔小于这个值的时候，当前显示的贴纸序列帧会继续显示，直到显示的时间大于该设定值贴纸才会切换到下一阵，相机帧不受影响。
     * @return  成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
     */
    public native int setMinInterval(float value);

    /**
     * 通知声音停止函数
     * @param name   结束播放的声音文件名（MB）,默认150MB,素材过大时,循环加载,降低内存； 贴纸较小时,全部加载,降低cpu
     * @return        成功返回ST_OK,失败返回其他错误码,错误码定义在st_mobile_common.h中,如ST_E_FAIL等
     */
    public native int setSoundPlayDone(String name);

    /**
     * 销毁实例，必须在opengl环境中运行
     */
    private native void destroyInstanceNative();
}
