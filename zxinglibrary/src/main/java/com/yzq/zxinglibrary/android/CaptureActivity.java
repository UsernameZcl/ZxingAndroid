package com.yzq.zxinglibrary.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Html;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.yzq.zxinglibrary.R;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.camera.CameraManager;
import com.yzq.zxinglibrary.common.Constant;
import com.yzq.zxinglibrary.decode.DecodeImgCallback;
import com.yzq.zxinglibrary.decode.DecodeImgThread;
import com.yzq.zxinglibrary.decode.ImageUtil;
import com.yzq.zxinglibrary.view.ViewfinderView;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * @author: yzq
 * @date: 2017/10/26 15:22
 * @declare :扫一扫
 */

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();
    public ZxingConfig config;
    private SurfaceView previewView;
    private ViewfinderView viewfinderView;
    private ImageView flashLightIv;
    //private TextView flashLightTv;
    private ImageView backIv;
    //  private LinearLayoutCompat flashLightLayout;
    //private LinearLayoutCompat albumLayout;
//    private LinearLayoutCompat bottomLayout;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private SurfaceHolder surfaceHolder;
    private TextView number;
    private List<String> caveLists = new ArrayList<>();

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    private TextView scan;

    private SensorManager mSensorManager;
    private float mLux;
    private Sensor mSensor;


    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 保持Activity处于唤醒状态
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.BLACK);
        }

        /*先获取配置信息*/
        try {
            config = (ZxingConfig) getIntent().getExtras().get(Constant.INTENT_ZXING_CONFIG);
        } catch (Exception e) {

            Log.i("config", e.toString());
        }

        if (config == null) {
            config = new ZxingConfig();
        }


        setContentView(R.layout.activity_capture);

// 获取服务
        mSensorManager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);
// 传感器种类-光照传感器
        mSensor = mSensorManager
                .getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(listener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        initView();

        hasSurface = false;

        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        beepManager.setPlayBeep(config.isPlayBeep());
        beepManager.setVibrate(config.isShake());


    }

    private SensorEventListener listener =
            new SensorEventListener() {
                public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
// 获取光线强度
                        mLux = event.values[0];
                        Log.e("TAG", "mLux:" + mLux);
                        changeWindowBrightness((int) mLux);
                    }
                }

                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };

    public void changeWindowBrightness(int brightness) {
        Window window = this.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        if (brightness == -1) {
            layoutParams.screenBrightness =
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            layoutParams.screenBrightness =
                    (brightness <= 0 ? 1 : brightness) / 255f;
        }
        window.setAttributes(layoutParams);
    }


    private void initView() {
        previewView = (SurfaceView) findViewById(R.id.preview_view);
        previewView.setOnClickListener(this);

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView.setZxingConfig(config);

        scan = (TextView) findViewById(R.id.scan);
        scan.setText(config.getTitle());

        backIv = (ImageView) findViewById(R.id.backIv);
        backIv.setOnClickListener(this);

        flashLightIv = (ImageView) findViewById(R.id.flashLightIv);
        flashLightIv.setOnClickListener(this);
        // flashLightTv =(TextView) findViewById(R.id.flashLightTv);

//        flashLightLayout =(LinearLayoutCompat) findViewById(R.id.flashLightLayout);
//        flashLightLayout.setOnClickListener(this);
        // albumLayout =(LinearLayoutCompat) findViewById(R.id.albumLayout);
        //albumLayout.setOnClickListener(this);
        //  bottomLayout =(LinearLayoutCompat) findViewById(R.id.bottomLayout);

        number = (TextView) findViewById(R.id.number);
        boolean showText = config.isShowText();

        if(showText==false){
            number.setVisibility(View.GONE);
        }else {
            number.setVisibility(View.VISIBLE);
        }
        String str = "";
        str += "已扫描" + "<font color='#599DDD'> " + 0 + " </font>" + "个记录仪,请继续扫描";

        CharSequence charSequence_code = Html.fromHtml(str);

        number.setText(charSequence_code);
//        switchVisibility(bottomLayout, config.isShowbottomLayout());
        switchVisibility(flashLightIv, config.isShowFlashLight());
        //  switchVisibility(albumLayout, config.isShowAlbum());


//        /*有闪光灯就显示手电筒按钮  否则不显示*/
        if (isSupportCameraLedFlash(getPackageManager())) {
            flashLightIv.setVisibility(View.VISIBLE);
        } else {
            flashLightIv.setVisibility(View.GONE);
        }

    }


    /**
     * @param pm
     * @return 是否有闪光灯
     */
    public static boolean isSupportCameraLedFlash(PackageManager pm) {
        if (pm != null) {
            FeatureInfo[] features = pm.getSystemAvailableFeatures();
            if (features != null) {
                for (FeatureInfo f : features) {
                    if (f != null && PackageManager.FEATURE_CAMERA_FLASH.equals(f.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param flashState 切换闪光灯图片
     */
    public void switchFlashImg(int flashState) {

        if (flashState == Constant.FLASH_OPEN) {
            flashLightIv.setImageResource(R.mipmap.light_open);
            //   flashLightTv.setText("轻触关闭");
        } else {
            flashLightIv.setImageResource(R.mipmap.light_open);
            //   flashLightTv.setText("轻触照亮");
        }

    }

    /**
     * @param rawResult 返回的扫描结果
     */
    public void handleDecode(Result rawResult) {
        //返回扫描的结果
        String text = rawResult.getText();
//       Toast.makeText(CaptureActivity.this, "text"+text, Toast.LENGTH_SHORT).show();
        Log.e("TAG", "text:" + text);
//        if (text.equals("")) {
//            Toast.makeText(CaptureActivity.this, "抱歉,解析失败,重新试试.", Toast.LENGTH_SHORT).show();
//        } else {
        inactivityTimer.onActivity();

        beepManager.playBeepSoundAndVibrate();
//            //判断是否连续扫描
        boolean continuity = config.isContinuity();
//            //是否对扫描的条码进行检查
        boolean pipelinedCode = config.isPipelinedCode();
        //扫描到的条码的长度
        int codeLength = config.getCodeLength();
        //对扫描到的条码进行判断   如:是否为纯数字
        boolean rule = config.isRule();
        int scanNumber = config.getScanNumber();

        Log.e("TAG", pipelinedCode + "");
        Log.e("TAG", codeLength + "");
        Log.e("TAG", rule + "");

        //纯数字的检验
        String regex = "^\\d+$";
//        if (rule == true) {
//
//            if (pipelinedCode == true) {
//                boolean matches = text.matches(regex);
//                if (matches == false) {
//                    Toast.makeText(CaptureActivity.this, "条码:" + text + "必须为纯流水码", Toast.LENGTH_LONG).show();
//                    continuePreview();
//                    return;
//                }
//
//            }
//            if (text.length() > codeLength || text.length() < codeLength) {
//                Toast.makeText(CaptureActivity.this, "条码:" + text + "必须为" + codeLength + "位", Toast.LENGTH_LONG).show();
//                continuePreview();
//                return;
//            }
//            Toast.makeText(CaptureActivity.this, "条码:" + text, Toast.LENGTH_SHORT).show();
//            //判断集合是否包含扫描到的条码
//            boolean contains = caveLists.contains(text);
//            if (contains == false) {
//                caveLists.add(text);
//            }
//            Intent intent = getIntent();
//            intent.putExtra(Constant.CODED_CONTENT, (Serializable) caveLists);
//            setResult(RESULT_OK, intent);
//            continuePreview();
//        } else {
        if (continuity == false) {
            caveLists.add(text);
            Log.e("TAG", text);
            Intent intent = getIntent();
            intent.putExtra(Constant.CODED_CONTENT, (Serializable) caveLists);
            setResult(RESULT_OK, intent);
            this.finish();
        } else {

            //  Toast.makeText(CaptureActivity.this, "条码:" + text, Toast.LENGTH_SHORT).show();
            //判断集合是否包含扫描到的条码
            boolean contains = caveLists.contains(text);
            if (contains == false) {
                caveLists.add(text);
                String str = "";
                str += "已扫描" + "<font color='#599DDD'> " + caveLists.size() + " </font>" + "个记录仪,请继续扫描";

                CharSequence charSequence_code = Html.fromHtml(str);

                number.setText(charSequence_code);
            }
//                else {
//                    number.setText("记录仪编码:"+text+"已包含,请继续扫描");
//                }


            if (scanNumber == caveLists.size()) {
                String str = "";
                str += "已扫描" + "<font color='#599DDD'> " + caveLists.size() + " </font>" + "个记录仪,扫描完成";

                CharSequence charSequence_code = Html.fromHtml(str);

                number.setText(charSequence_code);
                Intent intent = getIntent();
                intent.putExtra(Constant.CODED_CONTENT, (Serializable) caveLists);
                setResult(RESULT_OK, intent);
                this.finish();
            } else {
                Intent intent = getIntent();
                intent.putExtra(Constant.CODED_CONTENT, (Serializable) caveLists);
                setResult(RESULT_OK, intent);
                continuePreview();
            }


        }
    }
    //}
//    }


    private void switchVisibility(View view, boolean b) {
        if (b) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        cameraManager = new CameraManager(getApplication(), config);

        viewfinderView.setCameraManager(cameraManager);
        handler = null;

        surfaceHolder = previewView.getHolder();
        if (hasSurface) {

            initCamera(surfaceHolder);
        } else {
            // 重置callback，等待surfaceCreated()来初始化camera
            surfaceHolder.addCallback(this);
        }

        beepManager.updatePrefs();
        inactivityTimer.onResume();

    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            // 打开Camera硬件设备
            cameraManager.openDriver(surfaceHolder);
            // 创建一个handler来打开预览，并抛出一个运行时异常
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("扫一扫");
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    @Override
    protected void onPause() {

        Log.i("CaptureActivity", "onPause");
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();

        if (!hasSurface) {

            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(listener);
        }
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void onClick(View view) {

        int id = view.getId();
        if (id == R.id.flashLightIv) {
            /*切换闪光灯*/
            cameraManager.switchFlashLight(handler);
        }
//        else if (id == R.id.albumLayout) {
//            /*打开相册*/
//            Intent intent = new Intent();
//            intent.setAction(Intent.ACTION_PICK);
//            intent.setType("image/*");
//            startActivityForResult(intent, Constant.REQUEST_IMAGE);
//        }
        else if (id == R.id.backIv) {
            finish();
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constant.REQUEST_IMAGE && resultCode == RESULT_OK) {
            String path = ImageUtil.getImageAbsolutePath(this, data.getData());

            new DecodeImgThread(path, new DecodeImgCallback() {
                @Override
                public void onImageDecodeSuccess(Result result) {
                    handleDecode(result);
                }

                @Override
                public void onImageDecodeFailed() {
                    Toast.makeText(CaptureActivity.this, "抱歉，解析失败,换个图片试试.", Toast.LENGTH_SHORT).show();
                }
            }).run();


        }
    }

    /**
     * 使Zxing能够继续扫描
     */
    public void continuePreview() {
        if (handler != null) {
            handler.restartPreviewAndDecode();
        }
    }

}
