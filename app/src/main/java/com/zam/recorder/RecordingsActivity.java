/*
 * Copyright (C) 2023 Zokirjon Mamadjonov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zam.recorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zam.recorder.utils.AppConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class RecordingsActivity extends AppCompatActivity implements View.OnTouchListener{

    private SharedPreferences sharedPreferences;
    private int darkColor;
    private Toolbar tRA;
    private RecyclerView rvRA;
    private ArrayList<File> recordingList;
    private RecordingAdapter recordingAdapter;
    private LinearLayout llPlay;
    private SeekBar sbRA;
    private TextView tvRecordingDurationStart;
    private ImageView ivReplay, ivPlay, ivForward, ivMore;
    private CardViewTouchListener cardViewTouchListener = new CardViewTouchListener();
    private CardView cvCopyToDevice, cvShare, cvDelete, cvRename;
    private LinearLayout llMore;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);

        sharedPreferences = getApplicationContext().getSharedPreferences(AppConstants.SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
        setTheme(sharedPreferences.getBoolean(AppConstants.SHARED_PREF_KEY_DARK_THEME, false));

        tRA = findViewById(R.id.tRA);
        rvRA = findViewById(R.id.rvRA);
        llPlay = findViewById(R.id.llPlay);
        sbRA = findViewById(R.id.sbRA);
        tvRecordingDurationStart = findViewById(R.id.tvRecordingDurationStart);
        ivReplay = findViewById(R.id.ivReplay);

        ivPlay = findViewById(R.id.ivPlay);
        ivForward = findViewById(R.id.ivForward);
        ivMore = findViewById(R.id.ivMore);

        cvCopyToDevice = findViewById(R.id.cvCopyToDevice);
        cvShare = findViewById(R.id.cvShare);
        cvRename = findViewById(R.id.cvRename);
        cvDelete = findViewById(R.id.cvDelete);

        llMore = findViewById(R.id.llMore);

        setupViews();
    }

    private void setupViews() {
        tRA.setTitle(R.string.recordings);
        tRA.setTitleTextColor(getColor(R.color.white));
        setSupportActionBar(tRA);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tRA.setNavigationIcon(R.drawable.back);

        rvRA.setLayoutManager(new LinearLayoutManager(this));

        recordingList = getRecordingList();

        recordingAdapter = new RecordingAdapter();
        rvRA.setAdapter(recordingAdapter);
        //********************************************************************************************
        rvRA.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                int MAX_VELOCITY_Y = 7000;
                if (Math.abs(velocityY) > MAX_VELOCITY_Y) {
                    velocityY = MAX_VELOCITY_Y * (int) Math.signum((double)velocityY);
                    rvRA.fling(velocityX, velocityY);
                    return true;
                }
                return false;
            }
        });
        //To reduce scroll speed to be able to bind holders on -> onBindViewHolder() method of RecordingAdapter class
        //Source (https://stackoverflow.com/questions/32120452/control-fling-speed-for-recycler-view)
        //********************************************************************************************

        ivReplay.setOnTouchListener(this);
        ivPlay.setOnTouchListener(this);
        ivForward.setOnTouchListener(this);
        ivMore.setOnTouchListener(this);

        cvCopyToDevice.setOnTouchListener(cardViewTouchListener);
        cvShare.setOnTouchListener(cardViewTouchListener);
        cvRename.setOnTouchListener(cardViewTouchListener);
        cvDelete.setOnTouchListener(cardViewTouchListener);

        intent = new Intent(this, RecordingsService.class);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!serviceBound) {
            serviceBound = bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private RecordingsService recordingsService;
    boolean serviceBound = false;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            RecordingsService.RecordingsServiceBinder binder = (RecordingsService.RecordingsServiceBinder) service;
            recordingsService = binder.getService();
            recordingsService.setSbRA(sbRA);
            recordingsService.setIvPlay(ivPlay);
            recordingsService.setTvRecordingDurationStart(tvRecordingDurationStart);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };


    private void setTheme(boolean darkTheme){
        if (darkTheme) {
            this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.dark_background));
            darkColor = getColor(R.color.white);
        } else {
            this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.white));
            darkColor = getColor(R.color.black);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.recordings_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    private String FILE_PATH;
    private ArrayList<File> songsList = new ArrayList<>();

    private ArrayList<File> getRecordingList() {
        FILE_PATH = getFilesDir().getAbsolutePath();

        if (FILE_PATH != null) {
            File home = new File(FILE_PATH);
            File[] listFiles = home.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                songsList.addAll(Arrays.asList(listFiles));
            }
        }
        Collections.sort(songsList, (o1, o2) -> {
            String date1, date2;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss");
            date1 = sdf.format(new Date(o1.lastModified()));
            date2 = sdf.format(new Date(o2.lastModified()));
            return date1.compareTo(date2);
        });
        return songsList;
    }

    private class RecordingAdapter extends RecyclerView.Adapter<RecordingHolder>{

        @NonNull
        @Override
        public RecordingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecordingHolder(getLayoutInflater(),parent);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordingHolder holder, int position) {
            File recording = recordingList.get(position);
            holder.bind(recording);
            bindClickListenersToHolder(holder, position, recording);
        }

        @Override
        public int getItemCount() {
            return recordingList.size();
        }
    }

    private class RecordingHolder extends RecyclerView.ViewHolder{

        private final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        private final TextView tvRecordingName, tvRecordingDuration, tvRecordingDate;
        private final ImageView ivRecording, ivDelete;

        public RecordingHolder(LayoutInflater layoutInflater, @NonNull ViewGroup parent) {
            super(layoutInflater.inflate(R.layout.recording_holder, parent,false));

            tvRecordingName = itemView.findViewById(R.id.tvRecordingName);
            tvRecordingDuration = itemView.findViewById(R.id.tvRecordingDuration);
            tvRecordingDate = itemView.findViewById(R.id.tvRecordingDate);
            ivRecording = itemView.findViewById(R.id.ivRecording);
            ivDelete = itemView.findViewById(R.id.ivDelete);

            if (sharedPreferences.getBoolean(AppConstants.SHARED_PREF_KEY_DARK_THEME, false)) {
                changeColorToLight();
            } else {
                changeColorToDark();
            }
        }

        private void changeColorToLight() {
            tvRecordingName.setTextColor(getColor(R.color.white));
            tvRecordingDuration.setTextColor(getColor(R.color.white));
            tvRecordingDate.setTextColor(getColor(R.color.white));
        }

        private void changeColorToDark() {
            tvRecordingName.setTextColor(darkColor);
            tvRecordingDuration.setTextColor(darkColor);
            tvRecordingDate.setTextColor(darkColor);
        }

        public void bind(File recording) {
            tvRecordingName.setText(recording.getName());

            mmr.setDataSource(recording.getPath());
            long milliseconds = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            tvRecordingDuration.setText(formatMilliSecond(milliseconds));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  HH:mm");
            if (!DateFormat.is24HourFormat(RecordingsActivity.this)) {
                sdf = new SimpleDateFormat("yyyy/MM/dd  hh:mm a");
            }
            tvRecordingDate.setText(sdf.format(new Date(recording.lastModified())));
        }

        public TextView getTvRecordingName() {
            return tvRecordingName;
        }

        public ImageView getIvRecording() {
            return ivRecording;
        }

        public ImageView getIvDelete() {
            return ivDelete;
        }
    }

    public static String formatMilliSecond(long milliseconds) {
        String duration = "";
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours);

        if (hours > 0) {
            duration += hours + ":";
        }

        if (minutes < 10) {
            duration += "0" + minutes + ":";
        } else {
            duration += minutes + ":";
        }

        if (seconds < 10) {
            duration += "0" + seconds;
        } else {
            duration += seconds;
        }

        return duration;
    }

    private boolean isEnable = false, isSelectAllClicked;
    private MutableLiveData<String> mutableLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isSelectAll = new MutableLiveData<>(false);
    private ArrayList<File> selectedRecordingList = new ArrayList<>();
    private String playingRecordingName = "";

    private void bindClickListenersToHolder(RecordingHolder holder, int position, File recording) {
        holder.itemView.setOnLongClickListener(v -> {
            if (RecordingsService.mediaPlayer == null) {
                playingRecordingName = "";
                if (!isEnable) {
                    isSelectAllClicked = false;
                    ActionMode.Callback callback = new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                            MenuInflater menuInflater = mode.getMenuInflater();
                            menuInflater.inflate(R.menu.recordings_activity_delete_menu, menu);
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            isEnable = true;
                            ClickItem(recording, holder);
                            mutableLiveData.observe(RecordingsActivity.this, s ->
                                    mode.setTitle(s + " " + getString(R.string.selected))
                            );
                            return true;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menu_delete:
                                    deleteAllRecording();
                                    break;
                                case R.id.menu_select_all:
                                    isSelectAll.observe(RecordingsActivity.this, aBoolean -> {
                                        if (!aBoolean) {
                                            item.setIcon(R.drawable.select_all);
                                        } else {
                                            item.setIcon(R.drawable.all_selected);
                                        }
                                    });
                                    if (selectedRecordingList.size() == recordingList.size() && isSelectAllClicked) {
                                        isSelectAll.setValue(false);
                                        selectedRecordingList.clear();
                                    } else {
                                        isSelectAll.setValue(true);
                                        selectedRecordingList.clear();
                                        selectedRecordingList.addAll(recordingList);
                                        isSelectAllClicked = true;
                                    }
                                    mutableLiveData.setValue(String.valueOf(selectedRecordingList.size()));
                                    recordingAdapter.notifyDataSetChanged();
                                    break;
                            }
                            return true;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                            isEnable = false;
                            isSelectAll.setValue(false);
                            selectedRecordingList.clear();
                            recordingAdapter.notifyDataSetChanged();
                        }
                    };
                    startSupportActionMode(callback);
                } else {
                    ClickItem(recording, holder);
                }
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isEnable) {
                ClickItem(recording, holder);
            } else {
                playRecording(recording, holder);
            }
        });

        if (!selectedRecordingList.contains(recordingList.get(position))) {
            holder.getIvDelete().setVisibility(View.GONE);
            holder.getIvRecording().setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        } else {
            holder.getIvDelete().setVisibility(View.VISIBLE);
            holder.getIvRecording().setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(getColor(R.color.transparent));
        }

        if (playingRecordingName.equals(recording.getName())){
            holder.tvRecordingName.setTextColor(getColor(R.color.red));
        } else {
            holder.tvRecordingName.setTextColor(darkColor);
        }
    }

    private void deleteAllRecording() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MaterialThemeDialog);
        builder.setTitle(R.string.delete_all);
        builder.setMessage(getString(R.string.delete_all_recording));
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.delete), (dialogInterface, i) -> {
            for (File x : selectedRecordingList) {
                x.delete();
            }
            recordingList.removeAll(selectedRecordingList);
            selectedRecordingList.clear();
            mutableLiveData.setValue(String.valueOf(selectedRecordingList.size()));
            recordingAdapter.notifyDataSetChanged();
            dialogInterface.cancel();
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.cancel());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void ClickItem(File recording, RecordingHolder holder) {
        if (holder.getIvDelete().getVisibility() == View.GONE) {
            holder.getIvDelete().setVisibility(View.VISIBLE);
            holder.getIvRecording().setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(getColor(R.color.transparent));
            selectedRecordingList.add(recording);

            if (selectedRecordingList.size() == recordingList.size()) {
                isSelectAll.setValue(true);
            }
        } else {
            holder.getIvDelete().setVisibility(View.GONE);
            holder.getIvRecording().setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            selectedRecordingList.remove(recording);
            isSelectAll.setValue(false);
        }
        mutableLiveData.setValue(String.valueOf(selectedRecordingList.size()));
    }

    private File currentRecording;
    private RecordingHolder currentHolder = null;

    private void playRecording(File recording, RecordingHolder holder) {

        if (currentHolder != null && currentHolder != holder) {
            currentHolder.getTvRecordingName().setTextColor(darkColor);
        }

        currentRecording = recording;
        currentHolder = holder;

        if (RecordingsService.mediaPlayer != null) {
            stopRecordingsService();
        }

        if (holder.getTvRecordingName().getTextColors().getDefaultColor() != getColor(R.color.red)) {
            holder.tvRecordingName.setTextColor(getColor(R.color.red));
            playingRecordingName = recording.getName();
            llPlay.setVisibility(View.VISIBLE);

            serviceBound = bindService(intent, connection, Context.BIND_AUTO_CREATE);
            startRecordingsService();
            recordingsService.setRecording(recording);
            recordingsService.startPlaying();
        } else {
            holder.tvRecordingName.setTextColor(darkColor);
        }

        int recordingPosition = holder.getAdapterPosition();
        rvRA.getLayoutManager().scrollToPosition(recordingPosition);
    }

    private void startRecordingsService(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopRecordingsService(){
        recordingsService.stopPlaying();
        unbindService(connection);
        stopService(intent);
        llPlay.setVisibility(View.GONE);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView imageView = (ImageView) v;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            imageView.setColorFilter(getColor(R.color.red));
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            imageView.clearColorFilter();

            switch (v.getId()) {
                case R.id.ivReplay:
                    RecordingsService.mediaPlayer.seekTo(RecordingsService.mediaPlayer.getCurrentPosition() - 5000);
                    recordingsService.updateSeekBar();
                    break;
                case R.id.ivPlay:
                    if (RecordingsService.isPlaying) {
                        recordingsService.pausePlaying();
                    } else {
                        recordingsService.resumePlaying();
                    }
                    break;
                case R.id.ivForward:
                    RecordingsService.mediaPlayer.seekTo(RecordingsService.mediaPlayer.getCurrentPosition() + 5000);
                    recordingsService.updateSeekBar();
                    break;
                case R.id.ivMore:
                    if (llMore.getVisibility() == View.VISIBLE) {
                        llMore.setVisibility(View.GONE);
                    } else {
                        llMore.setVisibility(View.VISIBLE);
                        int recordingPosition = currentHolder.getAdapterPosition();
                        rvRA.getLayoutManager().scrollToPosition(recordingPosition);
                    }
                    break;
            }
        }
        return true;
    }

    private class CardViewTouchListener implements View.OnTouchListener{

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            CardView cardView = (CardView) v;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                cardView.setCardBackgroundColor(getColor(R.color.transparent));
            }

            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                cardView.setCardBackgroundColor(getColor(R.color.primaryColor));

                switch (v.getId()) {
                    case R.id.cvCopyToDevice: copyToDevice(); break;
                    case R.id.cvShare: shareRecording(); break;
                    case R.id.cvRename: renameRecording(); break;
                    case R.id.cvDelete: deleteRecording(); break;
                }
            }
            return true;
        }
    }

    private void copyToDevice() {
        File folder;
        String filePath;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            folder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + AppConstants.DEVICE_FOLDER_NAME);
            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        } else {
            folder = new File(Environment.getExternalStorageDirectory() + AppConstants.DEVICE_FOLDER_NAME);
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        if (!folder.exists()) {
            folder.mkdir();
        }

        filePath = filePath+File.separator + "Recording (zikr.and.mehr)" + File.separator + currentRecording.getName();

        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(currentRecording.getAbsoluteFile());
            os = new FileOutputStream(filePath);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
    }

    private void shareRecording() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", currentRecording));
        shareIntent.setType("audio/mp4");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_)));
    }

    private void renameRecording() {
        View view = getLayoutInflater().inflate(R.layout.rename_recording, null);
        EditText textInputEditText = view.findViewById(R.id.etName);
        textInputEditText.setText(currentRecording.getName().substring(0, currentRecording.getName().length() - 4));

        AlertDialog.Builder builder = new AlertDialog.Builder(this,  R.style.MaterialThemeDialog);
        builder.setTitle(R.string.rename_);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.ok), null);
        builder.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.cancel());

        AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(dialogInterface -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view1 -> {
                String newFileName=textInputEditText.getText() + AppConstants.FILE_NAME_EXTENSION;
                int frequency = Collections.frequency(recordingList, new File(FILE_PATH, newFileName));
                if (frequency > 0) {
                    textInputEditText.setError(getString(R.string.rename_error));
                } else {
                    if (! currentRecording.renameTo(new File(FILE_PATH, File.separator+textInputEditText.getText() + AppConstants.FILE_NAME_EXTENSION))) {
                        Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                    } else {
                        recordingList.set(recordingList.indexOf(currentRecording), new File(FILE_PATH, File.separator + textInputEditText.getText() + AppConstants.FILE_NAME_EXTENSION));
                        currentRecording = new File(FILE_PATH,File.separator+textInputEditText.getText()+".mp4");
                        playingRecordingName = currentRecording.getName();
                        currentHolder.tvRecordingName.setText(currentRecording.getName());
                        recordingAdapter.notifyItemChanged(currentHolder.getAdapterPosition(),currentRecording);
                    }
                    alertDialog.dismiss();
                }
            });
        });

        alertDialog.show();
    }

    private void deleteRecording() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,  R.style.MaterialThemeDialog);
        builder.setTitle(R.string.delete_);
        builder.setMessage(getString(R.string.delete_recording));
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.delete), (dialogInterface, i) -> {
            File x = currentRecording;
            stopRecordingsService();
            if (x.delete()){
                Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show();
                recordingList.remove(x);
                recordingAdapter.notifyItemRemoved(currentHolder.getAdapterPosition());
            } else {
                Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
            }
            dialogInterface.cancel();
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.cancel());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (RecordingsService.mediaPlayer != null) {
            stopRecordingsService();
            currentHolder.tvRecordingName.setTextColor(darkColor);
            playingRecordingName = "";
        } else {
            super.onBackPressed();
        }
    }
}