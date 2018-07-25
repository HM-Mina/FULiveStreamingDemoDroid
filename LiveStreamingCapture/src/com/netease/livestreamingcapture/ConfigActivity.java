package com.netease.livestreamingcapture;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.LSMediaCapture.lsMediaCapture;
import com.netease.LSMediaCapture.util.string.StringUtil;
import com.netease.vcloud.video.effect.VideoEffect;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.netease.LSMediaCapture.lsMediaCapture.FormatType.RTMP;
import static com.netease.LSMediaCapture.lsMediaCapture.StreamType.AV;

public class ConfigActivity extends Activity implements View.OnClickListener{
    private PublishParam publishParam = null;
    private EditText editMainPushUrl  = null;
    private RadioGroup filterGroup = null;
    private RadioGroup qosGroup = null;
    private TextView txtUsingFilter = null, txtWatermark = null, txtQos = null, txtGraffiti = null,txtFrontCamera = null,txtUpload = null;
    private Button mainStartBtn = null;
    private ImageView imageScan = null;
    private DateFormat formatter_file_name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());

    /**   6.0权限处理     **/
    private boolean bPermission = false;
    private final int WRITE_PERMISSION_REQ_CODE = 100;
    private boolean checkPublishPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(ConfigActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(ConfigActivity.this, Manifest.permission.CAMERA)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(ConfigActivity.this, Manifest.permission.RECORD_AUDIO)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(ConfigActivity.this, Manifest.permission.READ_PHONE_STATE)) {
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(ConfigActivity.this,
                        (String[]) permissions.toArray(new String[0]),
                        WRITE_PERMISSION_REQ_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WRITE_PERMISSION_REQ_CODE:
                for (int ret : grantResults) {
                    if (ret != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                bPermission = true;
                break;
            default:
                break;
        }
    }

    private ConfigActivity.MsgReceiver msgReceiver = null;

    public static class PublishParam implements Serializable {
        String pushUrl = null; //推流地址
        lsMediaCapture.StreamType streamType = AV;  // 推流类型
        lsMediaCapture.FormatType formatType = RTMP; // 推流格式
        String recordPath; //文件录制地址，当formatType 为 MP4 或 RTMP_AND_MP4 时有效
        lsMediaCapture.VideoQuality videoQuality = lsMediaCapture.VideoQuality.SUPER; //清晰度
        boolean isScale_16x9 = false; //是否强制16:9
        boolean useFilter = true; //是否使用滤镜
        VideoEffect.FilterType filterType = VideoEffect.FilterType.clean; //滤镜类型
        boolean frontCamera = true; //是否默认前置摄像头
        boolean watermark = false; //是否添加水印
        boolean qosEnable = true;  //是否开启QOS
        int qosEncodeMode = 1; // 1:流畅优先, 2:清晰优先
        boolean graffitiOn = false; //是否添加涂鸦
        boolean uploadLog = true; //是否上传SDK运行日志

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_config);
        bPermission = checkPublishPermission();
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("LiveStreamingStopFinished");
        registerReceiver(msgReceiver, intentFilter);
        publishParam = new PublishParam();
        initButtons();

        ((EditText)findViewById(R.id.main_push_url)).setText(".live.126.net");
        onClick(findViewById(R.id.main_use_filter));
    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(msgReceiver);
        msgReceiver = null;
        super.onDestroy();
    }

    private void switchTo(TextView view, boolean on){
        view.setTag(((Boolean)on).toString());
        if(on){
            view.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.switch_on, 0);
        }
        else{
            view.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.switch_off, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data ==null || data.getExtras() == null || TextUtils.isEmpty(data.getExtras().getString("result"))) {
            return;
        }
        String result = data.getExtras().getString("result");
        if(editMainPushUrl != null){
            editMainPushUrl.setText(result);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.main_water:
                if(Boolean.parseBoolean(txtWatermark.getTag().toString())){
                    switchTo(txtWatermark, false);
                    publishParam.watermark = false;
                }
                else{
                    switchTo(txtWatermark, true);
                    publishParam.watermark = true;
                }
                break;

            case R.id.main_qos:
                if(Boolean.parseBoolean(txtQos.getTag().toString())){
                    switchTo(txtQos, false);
                    publishParam.qosEnable = false;
//                    qosGroup.setVisibility(View.GONE);
                }
                else{
                    switchTo(txtQos, true);
                    publishParam.qosEnable = true;
//                    qosGroup.setVisibility(View.VISIBLE);
                }
                break;

            case R.id.main_graffiti:
                if(Boolean.parseBoolean(txtGraffiti.getTag().toString())){
                    switchTo(txtGraffiti, false);
                    publishParam.graffitiOn = false;
                }
                else{
                    switchTo(txtGraffiti, true);
                    publishParam.graffitiOn = true;
                }
                break;

            case R.id.main_use_filter:
                if(Boolean.parseBoolean(txtUsingFilter.getTag().toString())){
                    switchTo(txtUsingFilter, false);
                    filterGroup.setVisibility(View.GONE);
                    publishParam.useFilter = false;
                }
                else{
                    switchTo(txtUsingFilter, true);
                    filterGroup.setVisibility(View.VISIBLE);
                    publishParam.useFilter = true;
                }
                break;

            case R.id.main_front_camera:
                if(Boolean.parseBoolean(txtFrontCamera.getTag().toString())){
                    switchTo(txtFrontCamera, false);
                    publishParam.frontCamera = false;
                }
                else{
                    switchTo(txtFrontCamera, true);
                    publishParam.frontCamera = true;
                }
                break;

            case R.id.main_log_upload:
                if(Boolean.parseBoolean(txtUpload.getTag().toString())){
                    switchTo(txtUpload, false);
                    publishParam.uploadLog = false;
                }
                else{
                    switchTo(txtUpload, true);
                    publishParam.uploadLog = true;
                }
                break;

            case R.id.main_start:
                publishParam.pushUrl = ((EditText)findViewById(R.id.main_push_url)).getText().toString();
                publishParam.recordPath = ((EditText)findViewById(R.id.main_format_path)).getText().toString();

                if(!bPermission){
                    Toast.makeText(getApplication(),"请先允许app所需要的权限",Toast.LENGTH_LONG).show();
                    bPermission = checkPublishPermission();
                    return;
                }

                if(StringUtil.isEmpty(publishParam.pushUrl) || !publishParam.pushUrl.contains(".live.126.net")){
                    Toast.makeText(getApplication(),"请先设置正确的推流地址", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Intent intent = new Intent(ConfigActivity.this, LiveStreamingActivity.class);
                    intent.putExtra("data", publishParam);
                    startActivity(intent);
                }
                break;

            case R.id.main_scan_image:
                Intent intent = new Intent(ConfigActivity.this, QRCodeScanActivity.class);
                startActivityForResult(intent, 100);
                break;

            default:
                break;

        }
    }

    private void initButtons() {
        (imageScan = (ImageView)findViewById(R.id.main_scan_image)).setOnClickListener(this);
        editMainPushUrl = (EditText)findViewById(R.id.main_push_url);
        RadioGroup streamGroup = (RadioGroup)findViewById(R.id.main_stream_type);
        streamGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId)
                {
                    case R.id.main_stream_av:
                        publishParam.streamType = lsMediaCapture.StreamType.AV;
                        break;
                    case R.id.main_stream_video:
                        publishParam.streamType = lsMediaCapture.StreamType.VIDEO;
                        break;
                    case R.id.main_stream_audio:
                        publishParam.streamType = lsMediaCapture.StreamType.AUDIO;
                        break;
                    default:
                        publishParam.streamType = lsMediaCapture.StreamType.AV;
                        break;
                }
            }
        });

        final EditText recordPath = (EditText) findViewById(R.id.main_format_path);
        recordPath.setText("/sdcard/111/" + formatter_file_name.format(new Date()) + ".mp4");

        RadioGroup formatGroup = (RadioGroup)findViewById(R.id.main_format_type);
        formatGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId)
                {
                    case R.id.main_format_rtmp:
                        publishParam.formatType = lsMediaCapture.FormatType.RTMP;
                        recordPath.setVisibility(View.GONE);
                        break;
                    case R.id.main_format_mp4:
                        publishParam.formatType = lsMediaCapture.FormatType.MP4;
                        recordPath.setVisibility(View.VISIBLE);
                        break;
                    case R.id.main_format_rtmp_and_mp4:
                        publishParam.formatType = lsMediaCapture.FormatType.RTMP_AND_MP4;
                        recordPath.setVisibility(View.VISIBLE);
                        break;
                    default:
                        publishParam.formatType = lsMediaCapture.FormatType.RTMP;
                        break;
                }
            }
        });

        RadioGroup resolutionGroup = (RadioGroup)findViewById(R.id.main_radiogroup_resolution);
        resolutionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId)
                {
                    case R.id.main_resolution_super:
                        publishParam.videoQuality = lsMediaCapture.VideoQuality.SUPER;
                        break;
                    case R.id.main_resolution_high:
                        publishParam.videoQuality = lsMediaCapture.VideoQuality.HIGH;
                        break;
                    case R.id.main_resolution_medium:
                        publishParam.videoQuality = lsMediaCapture.VideoQuality.MEDIUM;
                        break;
                    default:
                        publishParam.videoQuality = lsMediaCapture.VideoQuality.HIGH;
                        break;
                }
            }
        });

        RadioGroup scaleGroup = (RadioGroup) findViewById(R.id.main_screen_scale);
        scaleGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId){
                    case R.id.main_scale_16x9:
                        publishParam.isScale_16x9 = true;
                        break;
                    case R.id.main_scale_normal:
                        publishParam.isScale_16x9 = false;
                        break;
                    default:
                        break;
                }
            }
        });

        (txtUsingFilter = (TextView)findViewById(R.id.main_use_filter)).setOnClickListener(this);
        filterGroup = (RadioGroup)findViewById(R.id.main_filter_radiogroup);
        filterGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId)
                {
                    case R.id.main_filter_clean:
                        publishParam.filterType = VideoEffect.FilterType.clean;
                        break;
                    case R.id.main_filter_nature:
                        publishParam.filterType = VideoEffect.FilterType.nature;
                        break;
                    case R.id.main_filter_pixar:
                        publishParam.filterType = VideoEffect.FilterType.pixar;
                        break;
                    case R.id.main_filter_tender:
                        publishParam.filterType = VideoEffect.FilterType.tender;
                        break;
                    default:
                        publishParam.filterType = VideoEffect.FilterType.clean;
                        break;
                }
            }
        });


        (txtWatermark = (TextView)findViewById(R.id.main_water)).setOnClickListener(this);
        (txtQos = (TextView)findViewById(R.id.main_qos)).setOnClickListener(this);
        qosGroup = (RadioGroup) findViewById(R.id.main_qos_radiogroup);
        qosGroup.setVisibility(View.GONE);
        qosGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId)
                {
                    case R.id.main_qos_fast:
                        publishParam.qosEncodeMode = 1;
                        break;
                    case R.id.main_qos_clear:
                        publishParam.qosEncodeMode = 2;
                        break;
                    default:
                        publishParam.qosEncodeMode = 1;
                        break;
                }
            }
        });
        (txtGraffiti = (TextView)findViewById(R.id.main_graffiti)).setOnClickListener(this);
        (txtFrontCamera = (TextView) findViewById(R.id.main_front_camera)).setOnClickListener(this);
        (txtUpload = (TextView) findViewById(R.id.main_log_upload)).setOnClickListener(this);
        (mainStartBtn = (Button)findViewById(R.id.main_start)).setOnClickListener(this);


    }

    //用于接收Service发送的消息
    public class MsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            int value = intent.getIntExtra("LiveStreamingStopFinished", 0);
            if(value == 1)//finished
            {
                mainStartBtn.setEnabled(true);
                mainStartBtn.setText("进入直播");
            }
            else//not yet finished
            {
                mainStartBtn.setEnabled(false);
                mainStartBtn.setText("直播停止中...");
            }
        }
    }


}
