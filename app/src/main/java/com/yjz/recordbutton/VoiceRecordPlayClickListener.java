package com.yjz.recordbutton;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

/**
 * 带动画的播放录音事件
 *
 * @author lizheng
 *         created at 2018/9/27 上午9:15
 */

public class VoiceRecordPlayClickListener implements View.OnClickListener {
    String message;
    /**
     * 本地路径
     */
    String voiceBody;
    ImageView voiceIconView;
    public String currentUrl;
    private AnimationDrawable voiceAnimation = null;
    MediaPlayer mediaPlayer = null;
    Context activity;

    public static boolean isPlaying = false;
    public static VoiceRecordPlayClickListener currentPlayListener = null;
    public static String currentPlayUrl = null;

    public VoiceRecordPlayClickListener(Context activity, String message, ImageView v,
                                        String lcoalPath) {
        this.message = message;
        voiceBody = lcoalPath;
        voiceIconView = v;
        this.activity = activity;
    }


    public void stopPlayVoice() {
        voiceAnimation.stop();
        voiceIconView.setImageResource(R.drawable.chat_voice_right3);
        // stop play voice
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        isPlaying = false;
        currentPlayUrl = null;
    }

    public void playVoice(String filePath) {
        if (!(new File(filePath).exists())) {
            return;
        }
        currentPlayUrl = message;
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

        mediaPlayer = new MediaPlayer();
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    stopPlayVoice(); // stop animation
                }

            });
            isPlaying = true;
            currentPlayListener = this;
            mediaPlayer.start();
            showAnimation();
        } catch (Exception e) {
        }
    }

    // show the voice playing animation
    private void showAnimation() {
        // play voice, and start animation
        voiceIconView.setImageResource(R.drawable.chat_anim_voice_right);
        voiceAnimation = (AnimationDrawable) voiceIconView.getDrawable();
        voiceAnimation.start();
    }

    @Override
    public void onClick(View v) {
        if (isPlaying) {
            if (currentPlayUrl != null && (currentPlayUrl.equals(message))) {
                currentPlayListener.stopPlayVoice();
                return;
            }
            currentPlayListener.stopPlayVoice();
        }
        // for sent msg, we will try to play the voice file directly
        playVoice(voiceBody);
    }
}