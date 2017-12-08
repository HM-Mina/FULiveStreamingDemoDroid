package com.netease.livestreamingcapture.widget;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.netease.livestreamingcapture.R;

/** 网络监控信息对话框
 * Created by hzzhujinbo on 2016/7/20.
 */
public class NetWorkInfoDialog extends PopupWindow{

    public static final String NETINFO_ACTION = "com.netease.netInfo";
    private TextView videoFrameRateTV, videoBitRateTV, audioBitRateTV, totalRealBitRateTV, ResolutionTV,networkLevelTV;
    private MsgReceiver msgReceiver;

    private int mVideoFrameRate = 0;
    private int mVideoBitrate = 0;
    private int mAudioBitrate = 0;
    private int mTotalRealBitrate = 0;
    private int mNetworkLevel = 0;

    private String mResolution;
    private Context mContext;

    public NetWorkInfoDialog(final Activity context) {

        mContext = context;
        initDialog(context);
        registerMsgReceiver();
    }

    private void initDialog(Activity context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View contextView = inflater.inflate(R.layout.net_info_layout,null);
        initView(contextView);
        this.setContentView(contextView);
        this.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);
        this.setTouchable(true);
        this.setOutsideTouchable(true);
        // 刷新状态
        this.update();
        // 实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0000000000);
        // 点back键和其他地方使其消失,设置了这个才能触发OnDismisslistener ，设置其他控件变化等操作
        this.setBackgroundDrawable(dw);
        this.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                unRegisterMsgReceiver();
            }
        });
        // 设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.AnimationPreview);

    }

    private void initView(View view) {
        videoFrameRateTV = (TextView)view.findViewById(R.id.VideoFrameRateTV);
        videoBitRateTV = (TextView)view.findViewById(R.id.VideoBitRateTV);
        audioBitRateTV = (TextView)view.findViewById(R.id.AudioBitRateTV);
        totalRealBitRateTV = (TextView)view.findViewById(R.id.TotalRealBitRateTV);
        ResolutionTV = (TextView)view.findViewById(R.id.ResolutionTV);
        networkLevelTV = (TextView) view.findViewById(R.id.networkLevel);

        videoFrameRateTV.setText(String.valueOf(mVideoFrameRate) + " fps");
        videoBitRateTV.setText(String.valueOf(mVideoBitrate) + " kbps");
        audioBitRateTV.setText(String.valueOf(mAudioBitrate) + " kbps");
        totalRealBitRateTV.setText(String.valueOf(mTotalRealBitrate) + " kbps");
        ResolutionTV.setText(mResolution);
    }

    private void registerMsgReceiver() {
        unRegisterMsgReceiver();
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NETINFO_ACTION);
        mContext.registerReceiver(msgReceiver, intentFilter);
    }

    private void unRegisterMsgReceiver() {
        if(msgReceiver != null) {
            mContext.unregisterReceiver(msgReceiver);
            msgReceiver = null;
        }
    }

    public class MsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            int videoFrameRate = intent.getIntExtra("videoFrameRate", 0);
            int videoBitRate = intent.getIntExtra("videoBitRate", 0);
            int audioBitRate = intent.getIntExtra("audioBitRate", 0);
            int totalRealBitRate = intent.getIntExtra("totalRealBitrate", 0);
            int networkLevel = intent.getIntExtra("networkLevel", 0);
            String resolution = intent.getStringExtra("resolution");

            mVideoFrameRate = videoFrameRate;
            mVideoBitrate = videoBitRate;
            mAudioBitrate = audioBitRate;
            mTotalRealBitrate = totalRealBitRate;
            mResolution = resolution;

            videoFrameRateTV.setText(String.valueOf(videoFrameRate) + " fps");
            videoBitRateTV.setText(String.valueOf(videoBitRate) + " kbps");
            audioBitRateTV.setText(String.valueOf(audioBitRate) + " kbps");
            totalRealBitRateTV.setText(String.valueOf(totalRealBitRate) + " kbps");
            networkLevelTV.setText(getNetworkLevel(networkLevel));
            ResolutionTV.setText(mResolution);
        }
    }

    private String getNetworkLevel(int level){
        String str = "未知";
        switch (level){
            case 1:
                str = "好";
                break;
            case 2:
                str = "一般";
                break;
            case 3:
                str = "差";
                break;
            case 4:
                str = "无效";
                break;
            default:
                break;
        }
        return str;
    }

}
