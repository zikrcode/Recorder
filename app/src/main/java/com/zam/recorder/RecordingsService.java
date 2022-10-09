package com.zam.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import java.io.File;

public class RecordingsService extends Service {

    public static MediaPlayer mediaPlayer;
    public static boolean isPlaying = false;

    private NotificationManager notificationManager;
    private Notification notification=null;
    private Notification.Builder builder;
    private NotificationCompat.Builder builderCompat;
    private final IBinder binder=new RecordingsServiceBinder();
    private File recording;
    private Handler handler=new Handler();
    private SeekBar sbRA;
    private TextView tvRecordingDurationStart;
    private ImageView ivPlay;

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        notificationManager=getSystemService(NotificationManager.class);

        Intent notificationIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.zam.recorder");

        //Toast.makeText(this, String.valueOf(notificationIntent), Toast.LENGTH_SHORT).show();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            Notification.Builder builder = new Notification.Builder(this, "CHANNEL_ID_2")
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText(getText(R.string.notification_playing))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setAutoCancel(false);

            notification=builder.build();
            this.builder=builder;
        }
        else {
            NotificationCompat.Builder builderCompat = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_playing))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(false);

            notification = builderCompat.build();
            this.builderCompat=builderCompat;
        }

        startForeground(2, notification);

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID_2", name, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setRecording(File recording) {
        this.recording = recording;
    }

    public void setSbRA(SeekBar sbRA) {
        this.sbRA = sbRA;

        sbRA.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser){
                    mediaPlayer.seekTo(progress);
                    updateSeekBar();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                updateSeekBar();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateSeekBar();
            }
        });
    }

    public void setTvRecordingDurationStart(TextView tvRecordingDurationStart) {
        this.tvRecordingDurationStart = tvRecordingDurationStart;
    }

    public void setIvPlay(ImageView ivPlay) {
        this.ivPlay = ivPlay;
    }

    public void startPlaying() {
        mediaPlayer=new MediaPlayer();
        try {
            mediaPlayer.setDataSource(recording.getPath());
            mediaPlayer.prepare();
            ivPlay.setImageDrawable(getDrawable(R.drawable.pause));
            sbRA.setMax(mediaPlayer.getDuration());
            mediaPlayer.start();
            isPlaying=true;
            updateSeekBar();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                    startPlaying();
                    pausePlaying();
                    updateSeekBar();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopPlaying() {
        ivPlay.setImageDrawable(getDrawable(R.drawable.play));
        handler.removeCallbacks(runnable);
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer=null;
    }

    public void pausePlaying() {
        ivPlay.setImageDrawable(getDrawable(R.drawable.play));
        handler.removeCallbacks(runnable);
        mediaPlayer.pause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setContentText(getText(R.string.notification_audio));
            notificationManager.notify(2,builder.build());
        }
        else {
            builderCompat.setContentText(getText(R.string.notification_audio));
            notificationManager.notify(2,builderCompat.build());
        }
        isPlaying=false;
    }

    public void resumePlaying() {
        ivPlay.setImageDrawable(getDrawable(R.drawable.pause));
        mediaPlayer.start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setContentText(getText(R.string.notification_playing));
            notificationManager.notify(2,builder.build());
        }
        else {
            builderCompat.setContentText(getText(R.string.notification_playing));
            notificationManager.notify(2,builderCompat.build());
        }
        isPlaying=true;
        updateSeekBar();
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                sbRA.setProgress(mediaPlayer.getCurrentPosition());
                tvRecordingDurationStart.setText(RecordingsActivity.formatMilliSecond(mediaPlayer.getCurrentPosition()));
                updateSeekBar();
            }
        }
    };

    public void updateSeekBar() {
        handler.post(runnable);
    }

    @Override
    public void onDestroy (){
        stopForeground(true);
    }

    public class RecordingsServiceBinder extends Binder {
        RecordingsService getService() {
            return RecordingsService.this;
        }
    }
}
