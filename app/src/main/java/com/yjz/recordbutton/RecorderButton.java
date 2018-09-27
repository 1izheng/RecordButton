package com.yjz.recordbutton;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.io.File;

/**
 * 仿微信录音
 *
 * @author lizheng
 *         created at 2018/8/21 下午1:45
 */
public class RecorderButton extends AppCompatButton {

    private AudioDialogManager mAudioDialogManager;
    private String mFileName = null;
    private OnFinishedRecordListener finishedListener;
    /**
     * 最短录音时间
     */
    private static final int MIN_INTERVAL_TIME = 2000;
    private long startTime = 0;
    /**
     * 录音时长
     */
    private long mLong;
    private MediaRecorder recorder;
    private ObtainDecibelThread thread;
    private Handler volumeHandler;
    /**
     * 两次点击录音按钮的间隔
     */
    private long doubleTime = 0;
    //是否应用于IM
    private boolean usedInIm = false;
    private Context mContext;
    private CheckRecordPermissionListener checkRecordPermissionListener;

    public RecorderButton(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public RecorderButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    public RecorderButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public void setSavePath(String path) {
        mFileName = path;
    }

    public void setOnFinishedRecordListener(OnFinishedRecordListener listener) {
        finishedListener = listener;
    }

    private void init() {
        volumeHandler = new ShowVolumeHandler();
        mAudioDialogManager = new AudioDialogManager(getContext());
    }

    private void prepareStartRecording() {
        initDialogAndStartRecord();
    }

    private long exitTime = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 屏蔽父控件拦截onTouch事件
        getParent().requestDisallowInterceptTouchEvent(true);

        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //验证录音权限
                if (checkPermission()) {
                    // 判断两次点击时间少于2秒不执行操作
                    doubleTime = System.currentTimeMillis() - exitTime;
                    if (doubleTime > 2000) {
                        prepareStartRecording();
                        exitTime = System.currentTimeMillis();
                    }
                } else {
                    if (checkRecordPermissionListener != null) {
                        checkRecordPermissionListener.checkRecordPermission();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isRecording) {
                    // 根据XY坐标判断是否想要取消
                    if (wantToCancel(x, y)) {
                        changeState(STATE_WANT_TO_CANCEL);
                    } else {
                        changeState(STATE_RECORDING);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mCurState == STATE_WANT_TO_CANCEL) {
                    cancelRecord();
                } else if (mCurState == STATE_RECORDING) {
                    finishRecord();
                }
                if (usedInIm) {
                    setBackgroundResource(R.drawable.button_recorder_normal);
                    setText(R.string.str_recorder_normal);
                }
                break;
            default:
                break;
        }

        return true;
    }

    private void initDialogAndStartRecord() {
        //防止没释放
        stopRecording();
        startRecording();
        mAudioDialogManager.showRecordingDialog();
        if (isRecording) {
            changeState(STATE_RECORDING);
        }
    }


    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission(Manifest.permission.RECORD_AUDIO)) {
                //录音
                return true;
            } else {
                //验证权限
                return false;
            }
        } else {
            //录音
            return true;
        }
    }

    public boolean checkPermission(@NonNull String permission) {
        return ActivityCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void finishRecord() {
        stopRecording();
        if (doubleTime <= 2000) {
            return;
        }
        mLong = System.currentTimeMillis() - startTime;
        if (mLong < MIN_INTERVAL_TIME) {
            mAudioDialogManager.tooShort();
            // 1/2秒后关闭提示
            volumeHandler.sendEmptyMessageDelayed(1000, 500);
            File file = new File(mFileName);
            file.delete();
            return;
        }
        mAudioDialogManager.dimissDialog();
        if (finishedListener != null) {
            String longStr = mLong / 1000 + "";
            File file = new File(mFileName);
            if (file != null && file.length() == 0) {
                showDefaultAlert(getContext(), "检测到录音失败，请确保录音权限开启");
            } else {
                finishedListener.onFinishedRecord(mFileName, longStr);
            }
        }
    }


    public void showDefaultAlert(Context context, String msg) {
        AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setMessage(msg);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();//显示对话框
    }

    private void cancelRecord() {
        stopRecording();
        mAudioDialogManager.dimissDialog();
        // 删除文件
        File file = new File(mFileName);
        file.delete();
    }

    private void startRecording() {
        mFileName = getRecordPathByCurrentTime();

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        // 设置输出文件
        recorder.setOutputFile(mFileName);

        try {
            //防止第三方权限拦截软件拒绝录音权限出错
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRecording = true;
        thread = new ObtainDecibelThread();
        thread.start();

        startTime = System.currentTimeMillis();
    }

    private void stopRecording() {
        if (thread != null) {
            thread.exit();
            thread = null;
        }

        if (recorder != null) {
            try {
                //防止第三方权限拦截软件拒绝录音权限出错
                recorder.stop();
                recorder.release();
            } catch (Exception e) {
            }
            recorder = null;
        }

        isRecording = false;
    }

    /**
     * 获取录音保存路径
     *
     * @return
     */
    private String getRecordPathByCurrentTime() {
        File sdcard = Environment.getExternalStorageDirectory();
        File cacheDir = new File(sdcard, "recorder_temp");
        checkAndMkdirs(cacheDir);
        File filesDir = new File(cacheDir, "voices");
        checkAndMkdirs(filesDir);
        return new File(filesDir, "record_" + System.currentTimeMillis()).getAbsolutePath() + ".amr";
    }

    private File checkAndMkdirs(File file) {
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public void setCheckRecordPermissionListener(CheckRecordPermissionListener checkRecordPermissionListener) {
        this.checkRecordPermissionListener = checkRecordPermissionListener;
    }

    private class ObtainDecibelThread extends Thread {

        private volatile boolean running = true;

        public void exit() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(100);
                    mLong += 0.1f;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (recorder == null || !running) {
                    break;
                }
                volumeHandler
                        .sendEmptyMessage(7 * recorder.getMaxAmplitude() / 32768 + 1);
            }
        }

    }

    class ShowVolumeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1000) {
                mAudioDialogManager.dimissDialog();
            } else {
                mAudioDialogManager.updateVoiceLevel(msg.what);
            }
        }
    }

    /**
     * 录音结束回调
     */
    public interface OnFinishedRecordListener {
        void onFinishedRecord(String audioPath, String voiceLong);
    }

    /**
     * 改变dialog状态
     *
     * @param state
     */
    private static final int STATE_NORMAL = 1;
    private static final int STATE_RECORDING = 2;
    private static final int STATE_WANT_TO_CANCEL = 3;
    private int mCurState = STATE_NORMAL;
    private boolean isRecording = false;

    private void changeState(int state) {
        if (mCurState != state) {
            mCurState = state;
            switch (state) {
                case STATE_NORMAL:
                    if (usedInIm) {
                        setBackgroundResource(R.drawable.button_recorder_normal);
                        setText(R.string.str_recorder_normal);
                    }
                    mAudioDialogManager.dimissDialog();
                    break;
                case STATE_RECORDING:
                    if (usedInIm) {
                        setBackgroundResource(R.drawable.button_recorder_recording);
                        setText(R.string.str_recorder_recording);
                    }
                    if (isRecording) {
                        mAudioDialogManager.recording();
                    }
                    break;
                case STATE_WANT_TO_CANCEL:
                    if (usedInIm) {
                        setBackgroundResource(R.drawable.button_recorder_recording);
                    }
                    mAudioDialogManager.wantToCancel();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 判断手指是否滑出按钮
     *
     * @param x
     * @param y
     * @return
     */
    private boolean wantToCancel(int x, int y) {
        if (x < 0 || x > getWidth()) {
            return true;
        }
        if (y < -50 || y > getHeight() + 50) {
            return true;
        }
        return false;
    }
}
