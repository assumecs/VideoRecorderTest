package com.n22.videorecordertest;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class RecordActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "MainActivity";
    private SurfaceView mSurfaceView;
    private ImageButton mBtnStartStop;
    private ImageButton mBtnSet;
    private ImageButton mBtnShowFile;
    private Chronometer mTimer;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private Camera.Parameters mParameters;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWindowFeature();
        setContentView(R.layout.activity_record);
        initView();
        initCameraAndSurfaceViewHolder();
//        prepareMediaRecorder();
    }

    private void initCameraAndSurfaceViewHolder() {
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mCamera=Camera.open();
    }

    private void initListeners() {
        mBtnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"录像");
                if (isRecording()) {
                    Log.d(TAG,"停止录像");
                    stopRecording();
                    mTimer.stop();
                    mBtnStartStop.setBackgroundResource(R.drawable.rec_start);
                } else {
                    if (startRecording()) {
                        Log.d(TAG,"开始录像");
                        mTimer.setBase(SystemClock.elapsedRealtime());
                        mTimer.start();
                        mBtnStartStop.setBackgroundResource(R.drawable.rec_stop);
                    }
                }
            }
        });
        mBtnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "暂停");
//                mTimer.
//                Log.d(TAG,"设置");
//                Toast.makeText(MainActivity.this,"设置待开发...",Toast.LENGTH_SHORT).show();
            }
        });
        mBtnShowFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(RecordActivity.this,ShowVideoActivity.class);
                startActivity(intent);
            }
        });

    }

    private boolean prepareMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));
        mMediaRecorder.setPreviewDisplay(mHolder.getSurface());
        String path = getSDPath();
        if (path != null) {

            File dir = new File(path + "/VideoRecorderTest");
            if (!dir.exists()) {
                dir.mkdir();
            }
            path = dir + "/" + getDate() + ".mp4";
            mMediaRecorder.setOutputFile(path);
            try {
                mMediaRecorder.prepare();
            } catch (IOException e) {
                releaseMediaRecorder();
                e.printStackTrace();
            }
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }
    public boolean startRecording() {
        if (prepareMediaRecorder()) {
            mMediaRecorder.start();
            return true;
        } else {
            releaseMediaRecorder();
        }
        return false;
    }

    public void stopRecording() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
        }
        releaseMediaRecorder();
    }

    private void initView() {
        mSurfaceView  = (SurfaceView)findViewById(R.id.capture_surfaceview);
        mBtnStartStop = (ImageButton) findViewById(R.id.ib_stop);
        mBtnSet= (ImageButton) findViewById(R.id.capture_imagebutton_setting);
        mBtnShowFile= (ImageButton) findViewById(R.id.capture_imagebutton_showfiles);
        mTimer= (Chronometer) findViewById(R.id.crm_count_time);
    }

    public boolean isRecording() {
        return mMediaRecorder != null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_main);
        initView();
        initCameraAndSurfaceViewHolder();
        initListeners();
    }

    private void initWindowFeature() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏

        // 设置横屏显示
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // 选择支持半透明模式,在有SurfaceView的activity中使用。
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
    }

    /**
     * 获取系统时间
     */
    public static String getDate(){
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);           // 获取年份
        int month = ca.get(Calendar.MONTH);         // 获取月份
        int day = ca.get(Calendar.DATE);            // 获取日
        int minute = ca.get(Calendar.MINUTE);       // 分
        int hour = ca.get(Calendar.HOUR);           // 小时
        int second = ca.get(Calendar.SECOND);       // 秒

        String date = "" + year + (month + 1 )+ day + hour + minute + second;
        Log.d(TAG, "date:" + date);

        return date;
    }

    /**
     * 获取SD path
     */
    public String getSDPath(){
        File sdDir;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist)
        {
            sdDir = Environment.getExternalStorageDirectory();// 获取外部存储的根目录
            return sdDir.toString();
        }

        return null;
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mParameters=mCamera.getParameters();
        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        mCamera.setParameters(mParameters);
//        mCamera.autoFocus(new Camera.AutoFocusCallback() {
//            @Override
//            public void onAutoFocus(boolean success, Camera camera) {
//                if(success){
//                    Log.d(TAG,"自动对焦成功");
//                }
//            }
//        });
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

            //下面这个方法能帮我们获取到相机预览帧，我们可以在这里实时地处理每一帧
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Log.i(TAG, "获取预览帧...");
                    new ProcessFrameAsyncTask().execute(data);
                    Log.d(TAG,"预览帧大小："+String.valueOf(data.length));
                }
            });
        } catch (IOException e) {
            Log.d(TAG,"设置相机预览失败",e);
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private class ProcessFrameAsyncTask extends AsyncTask<byte[],Void,String> {

        @Override
        protected String doInBackground(byte[]... params) {
            processFrame(params[0]);
            return null;
        }

        private void processFrame(byte[] frameData) {

            Log.i(TAG, "正在处理预览帧...");
            Log.i(TAG, "预览帧大小"+String.valueOf(frameData.length));
            Log.i(TAG, "预览帧处理完毕...");
            //下面这段注释掉的代码是把预览帧数据输出到sd卡中，以.yuv格式保存
//            String path = getSDPath();
//            File dir = new File(path + "/FrameTest");
//            if (!dir.exists()) {
//                dir.mkdir();
//            }
//            path = dir + "/" + "testFrame"+".yuv";
//            File file =new File(path);
//            try {
//                FileOutputStream fileOutputStream=new FileOutputStream(file);
//                BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(fileOutputStream);
//                bufferedOutputStream.write(frameData);
//                Log.i(TAG, "预览帧处理完毕...");
//
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }


    }
}

