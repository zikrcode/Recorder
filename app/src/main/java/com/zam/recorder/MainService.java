package com.zam.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Chronometer;

import androidx.core.app.NotificationCompat;

import com.zam.recorder.utils.AppConstants;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainService extends Service {

    public  static MediaRecorder mediaRecorder=null;
    public static String fileName;

    private SharedPreferences sharedPreferences;
    private Chronometer chronometer;
    private final IBinder binder = new MainServiceBinder();
    private Notification notification = null;
    private NotificationManager notificationManager;
    private Notification.Builder builder;
    private NotificationCompat.Builder builderCompat;

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        notificationManager = getSystemService(NotificationManager.class);

        Intent notificationIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(AppConstants.PACKAGE_NAME);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            Notification.Builder builder = new Notification.Builder(this, AppConstants.CHANNEL_ID_1)
                            .setContentTitle(getText(R.string.app_name))
                            .setContentText(getText(R.string.notification_recording))
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentIntent(pendingIntent)
                            .setVisibility(Notification.VISIBILITY_PUBLIC)
                            .setAutoCancel(false);

            notification = builder.build();
            this.builder = builder;
        } else {
            NotificationCompat.Builder builderCompat = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_recording))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(false);

            notification = builderCompat.build();

            this.builderCompat = builderCompat;
        }

        startForeground(1, notification);

        startRecording();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(AppConstants.CHANNEL_ID_1, name, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void setChronometer(Chronometer chronometer){
        this.chronometer = chronometer;
    }
    
    private boolean running = false;
    private long pauseOffset = 0;

    private void startRecording() {
        prepareFilePath();
        sharedPreferences = getApplicationContext().getSharedPreferences(AppConstants.SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(fileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        switch (sharedPreferences.getInt(AppConstants.SHARED_PREF_KEY_SAMPLE_RATE, 0)) {
            case 0:
                mediaRecorder.setAudioSamplingRate(8000);
                mediaRecorder.setAudioEncodingBitRate(48000);
                break;
            case 1:
                mediaRecorder.setAudioSamplingRate(22050);
                mediaRecorder.setAudioEncodingBitRate(128000);
                break;
            case 2:
                mediaRecorder.setAudioSamplingRate(44100);
                mediaRecorder.setAudioEncodingBitRate(192000);
                break;
        }

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();

            if (!running) {
                chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
                chronometer.start();
                running = true;
            }
        } catch (IOException e) {
            Log.e("AudioRecord", "prepare() failed");
        }
    }

    private void prepareFilePath(){
        SimpleDateFormat zm = new SimpleDateFormat("yyyyMMddhhmmss");
        String t = zm.format(new Date());
        fileName = File.separator + t + AppConstants.FILE_NAME_EXTENSION_ZAM;
        fileName = getFilesDir().getAbsolutePath() + fileName;
    }

    @Override
    public IBinder onBind(Intent intent) {
       return binder;
    }

    public void pauseRecording() {
        mediaRecorder.pause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setContentText(getText(R.string.notification_paused) + "  " + chronometer.getText());
            notificationManager.notify(1, builder.build());
        } else {
            builderCompat.setContentText(getText(R.string.notification_paused) + "  " + chronometer.getText());
            notificationManager.notify(1, builderCompat.build());
        }

        if (running) {
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            running = false;
        }
    }

    public void resumeRecording() {
        mediaRecorder.resume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setContentText(getText(R.string.notification_recording));
            notificationManager.notify(1, builder.build());
        } else {
            builderCompat.setContentText(getText(R.string.notification_recording));
            notificationManager.notify(1, builderCompat.build());
        }

        if (!running) {
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            running = true;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

        stopForeground(true);

        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
        running = false;
    }

    public class MainServiceBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }
}