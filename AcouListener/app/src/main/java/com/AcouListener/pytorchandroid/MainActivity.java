package com.AcouListener.pytorchandroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.File;
import java.io.FileInputStream;
import android.media.MediaRecorder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;


import android.os.Bundle;
import android.util.Log;
import com.chaquo.python.Kwarg;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.Python;
import java.util.ArrayList;
import java.util.List;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import android.graphics.BitmapFactory;
import java.io.BufferedWriter;
import java.io.FileWriter;



public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    static final String TAG = "PythonOnAndroid";
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private boolean isRecording = false;
    private ProgressBar progressBar;
    private Handler handler;
    private long startTime;
    private TextView tvTime;
    Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initPython();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

// 新建一个分类对象
        final MainActivity activity = this;
        classifier = new Classifier(Utils.assetFilePath(activity, "mobilenet-v2.pt"));
// 新建一个按钮对象用于启动拍照
        Button capture_cir = findViewById(R.id.capture);
        capture_cir.setOnClickListener(this);


        // 初始化按钮
        progressBar = findViewById(R.id.progress_bar);
        handler = new Handler();
        tvTime = findViewById(R.id.tv_time);
        Button play_start = findViewById(R.id.play_start);
        Button play_stop = findViewById(R.id.play_stop);
        Button record_start = findViewById(R.id.record_start);
        Button record_stop = findViewById(R.id.record_stop);
        // 按钮设置监听点击事件
        play_start.setOnClickListener(this);
        play_stop.setOnClickListener(this);
        record_start.setOnClickListener(this);
        record_stop.setOnClickListener(this);
        // 检查权限，不够则申请
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 2);
        } else {
            Toast.makeText(this, "获取了权限", Toast.LENGTH_SHORT).show();
            initMediaPlayer(); }
    }


    protected void CirResult (){
        File CIR_outputFile = new File(Environment.getExternalStorageDirectory() + "/AcouListener/cir_image.png");
        if (CIR_outputFile.exists()) {
            Intent resultView = new Intent(this, Result.class);   // 新建一个活动，result类
            try {
                // 读取文件并创建 Bitmap 对象
                Bitmap imageBitmap = BitmapFactory.decodeFile(CIR_outputFile.getAbsolutePath());
                // 调整图像大小为 224x224
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, 224, 224, true);
                // 将 resizedBitmap 和 pred 传递给 Result 活动
                resultView.putExtra("imagedata", resizedBitmap);
                String pred = classifier.predict(resizedBitmap);
                resultView.putExtra("pred", pred);
            } catch (Exception e) {
                e.printStackTrace();
            }
            startActivity(resultView);  // 启动互动 resultView
        } else {
            System.out.println("----------------------------------------文件不存在");
        }
    }



    private void initMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.sending_audio); // 使用 MediaPlayer.create() 初始化 MediaPlayer 并指定要播放的音频文件
            mediaPlayer.setLooping(true); // 设置循环播放
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//----------------------------------------------------------------------------------------

    private AudioRecord audioRecord;
    private int bufferSize;
    private String RECORDING_outputFile;
    private Thread recordingThread;

    private void initRecorder() {
        // 初始化 AudioRecord
        int sampleRate = 48000;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        bufferSize = Math.max(minBufferSize, sampleRate);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, channelConfig, audioFormat, bufferSize);

        // 设置输出文件路径
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "recording" + timeStamp + ".wav";
        RECORDING_outputFile = Environment.getExternalStorageDirectory() + "/AcouListener/" + fileName;

        String directoryPath = Environment.getExternalStorageDirectory() + "/AcouListener/";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

    }

    private void startRecording() {
        initRecorder();
        audioRecord.startRecording();

        // 在单独的线程中执行录音操作
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(RECORDING_outputFile);

                    byte[] buffer = new byte[bufferSize];
                    int bytesRead;

                    while (isRecording) {
                        bytesRead = audioRecord.read(buffer, 0, bufferSize);
                        if (bytesRead != AudioRecord.ERROR_INVALID_OPERATION) {
                            fileOutputStream.write(buffer, 0, bytesRead);  // 只写入有效的字节数据
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        recordingThread.start();  // 启动录音线程
    }

    private void stopRecording() {
        isRecording = false;  // 设置录音停止

        try {
            recordingThread.join();
            audioRecord.stop();
            audioRecord.release();
        } catch (Exception e) {
            e.printStackTrace();
        }



    }


//----------------------------------------------------------------------------------------

    @Override    // 检查权限不够时会申请，申请失败则退出，否则执行initMediaPlayer、initRecorder
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_start:
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start(); // 开始播放
                    Button play_start = findViewById(R.id.play_start);
                    play_start.setBackgroundColor(0xFFFF0000);
                }
                break;
            case R.id.play_stop:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.reset(); // 停止播放
                    initMediaPlayer();
                    Button play_start = findViewById(R.id.play_start);
                    play_start.setBackgroundColor(0xFF808080);
                }
                break;
            case R.id.record_start:
                if (!isRecording) {
                    startRecording();
                    startTime = SystemClock.uptimeMillis();
                    handler.postDelayed(updateProgressRunnable, 100);
                    isRecording = true;
                    Button record_start = findViewById(R.id.record_start);
                    record_start.setBackgroundColor(0xFFFF0000);
                    }
                break;
            case R.id.record_stop:
                if (isRecording) {
                    handler.removeCallbacks(updateProgressRunnable);
                    stopRecording();
                    isRecording = false;
                    Button record_start = findViewById(R.id.record_start);
                    record_start.setBackgroundColor(0xFF808080);
                    callPythonCode();
                }
                break;
            case R.id.capture:
                CirResult();
                break;
            default:
                break;
        }
    }

    private Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long elapsedTime = SystemClock.uptimeMillis() - startTime;
                int progress = (int) ((elapsedTime / 100) * 10); // 映射10秒到1000单位的进度条
                progressBar.setProgress(progress);
                String formattedTime = formatTime(elapsedTime);
                tvTime.setText(formattedTime);
                // 继续更新进度条
                handler.postDelayed(this, 100);
            }
        }
    };

    private String formatTime(long timeInMillis) {
        int seconds = (int) (timeInMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }


    // 初始化Python环境
    void initPython(){
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }
    // 调用python代码
    void callPythonCode() {
        // 读取音频文件的原始数据
        File file = new File(RECORDING_outputFile);
        byte[] audioData = new byte[(int) file.length()];

        try (FileInputStream inputStream = new FileInputStream(file)) {
            inputStream.read(audioData);
        } catch (IOException e) {
            e.printStackTrace();
        }

// 将原始数据转换为 short 数组
        short[] audioDataShort = new short[audioData.length / 2];

        for (int i = 0; i < audioData.length / 2; i++) {
            audioDataShort[i] = (short) ((audioData[2 * i + 1] << 8) | audioData[2 * i]);
        }


        Python py = Python.getInstance();
        // 将 Java 的 short[] 数组转换为一个 NumPy 数组
        PyObject audioDataPyObj = py.getModule("numpy").callAttr("array", audioDataShort);
        String CIR_outputFile = Environment.getExternalStorageDirectory() + "/AcouListener/";
//        System.out.println("---------------------audioData 数组的长度：" + audioDataShort.length);
        py.getModule("cal_cir").callAttr("main", audioDataPyObj, CIR_outputFile);
    }
}
