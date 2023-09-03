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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.zam.recorder.utils.AppConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener{

    private SharedPreferences sharedPreferences;
    private Toolbar tMa;
    private ImageView ivStart, ivRecordings;
    private TextView tvState;
    private Button bDelete, bSave;
    private Chronometer chronometer;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_main);

        requestRecordingPermissions();

        sharedPreferences = getApplicationContext()
                .getSharedPreferences(AppConstants.SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
        setTheme(sharedPreferences.getBoolean(AppConstants.SHARED_PREF_KEY_DARK_THEME, false));

        tMa = findViewById(R.id.tMA);
        ivStart = findViewById(R.id.ivStart);
        tvState = findViewById(R.id.tvState);
        ivRecordings = findViewById(R.id.ivRecordings);
        bDelete = findViewById(R.id.bCancel);
        bSave = findViewById(R.id.bSave);
        chronometer = findViewById(R.id.chronometer);

        setupViews();
    }

    private void requestRecordingPermissions() {
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions = new String[] {
                    Manifest.permission.RECORD_AUDIO
            };
        } else {
            permissions = new String[] {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };
        }

        ActivityCompat.requestPermissions(this, permissions, AppConstants.PERMISSION_REQUEST_CODE);
    }

    private void setupViews() {
        tMa.setTitleTextColor(getColor(R.color.white));
        setSupportActionBar(tMa);

        ivStart.setOnTouchListener(this);
        ivRecordings.setOnTouchListener(this);
        bDelete.setOnClickListener(this);
        bSave.setOnClickListener(this);

        intent = new Intent(this, MainService.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTheme(sharedPreferences.getBoolean(AppConstants.SHARED_PREF_KEY_DARK_THEME, false));

        if (!serviceBound) {
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private void setTheme(boolean darkTheme) {
        if (darkTheme) {
            this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.dark_background));
        } else {
            this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.white));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intentSettings = new Intent(this, SettingsActivity.class);
        startActivity(intentSettings);
        return true;
    }

    private boolean resume = false, finish = true;

    private MainService mainService;
    boolean serviceBound = false;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainService.MainServiceBinder binder = (MainService.MainServiceBinder) service;
            mainService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView imageView = (ImageView) v;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            imageView.setColorFilter(getColor(R.color.transparent));
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            imageView.clearColorFilter();

            switch (v.getId()) {
                case R.id.ivStart:
                    if (isPermissionGranted()) {
                        chronometer.setVisibility(View.VISIBLE);

                        if (finish) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent);
                            } else {
                                startService(intent);
                            }
                            mainService.setChronometer(chronometer);
                            finish = false;

                            tvState.setVisibility(View.VISIBLE);

                            ivRecordings.setVisibility(View.GONE);
                            bDelete.setVisibility(View.VISIBLE);
                            bSave.setVisibility(View.VISIBLE);

                            ivStart.setImageDrawable(getDrawable(R.drawable.recording_resume));
                            tvState.setText(getString(R.string.pause));
                            resume=true;
                        } else {
                            if (resume) {
                                ivStart.setImageDrawable(getDrawable(R.drawable.recording_pause));
                                tvState.setText(getString(R.string.resume));
                                mainService.pauseRecording();
                                resume = false;
                            } else {
                                ivStart.setImageDrawable(getDrawable(R.drawable.recording_resume));
                                tvState.setText(getString(R.string.pause));
                                mainService.resumeRecording();
                                resume = true;
                            }
                        }
                    } else {
                        showPermissionNeeded();
                    }
                    break;

                case R.id.ivRecordings:
                    unbindService(connection);
                    stopService(intent);
                    serviceBound = false;
                    Intent intent = new Intent(this, RecordingsActivity.class);
                    startActivity(intent);
                    break;
            }
        }
        return true;
    }

    private boolean isPermissionGranted() {
        boolean permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            permissions= checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return permissions;
    }

    private void showPermissionNeeded() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,  R.style.MaterialThemeDialog);
        builder.setTitle(R.string.permission);
        builder.setMessage(R.string.permission_required);
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.ok), null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bCancel) {
            cancelRecording();
        }
        if (v.getId() == R.id.bSave) {
            saveRecording();
            if (sharedPreferences.getBoolean(AppConstants.SHARED_PREF_KEY_NAME_MANUALLY, false)) {
                renameRecording();
            } else {
                Toast.makeText(this, getString(R.string.recording_saved), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cancelRecording() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,  R.style.MaterialThemeDialog);
        builder.setTitle(R.string.cancel_);
        builder.setMessage(R.string.cancel_recording);
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.yes), (dialogInterface, i) -> {
            String fileName = mainService.getFileName();
            saveRecording();
            if (!new File(fileName).delete()) {
                Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
            }
            dialogInterface.cancel();
        });

        builder.setNegativeButton(getString(R.string.no), (dialogInterface, i) -> dialogInterface.cancel());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void saveRecording() {
        chronometer.setVisibility(View.GONE);
        tvState.setVisibility(View.GONE);

        mainService.stopRecording();

        ivStart.setImageDrawable(getDrawable(R.drawable.recording));
        resume = false;
        finish = true;

        bDelete.setVisibility(View.GONE);
        bSave.setVisibility(View.GONE);
        ivRecordings.setVisibility(View.VISIBLE);
    }

    private File currentRecording;
    private String FILE_PATH;

    private void renameRecording() {
        currentRecording = new File(MainService.fileName);
        FILE_PATH = getFilesDir().getAbsolutePath();
        View view = getLayoutInflater().inflate(R.layout.rename_recording, null);
        EditText textInputEditText = view.findViewById(R.id.etName);
        textInputEditText.setText(currentRecording.getName().substring(0, currentRecording.getName().length() - 4));

        AlertDialog.Builder builder = new AlertDialog.Builder(this,  R.style.MaterialThemeDialog);
        builder.setTitle(R.string.save_recording);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.ok), null);
        builder.setNegativeButton(getString(R.string.delete), (dialogInterface, i) -> {
            if (!new File(MainService.fileName).delete()) {
                Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
            }
            dialogInterface.cancel();
        });

        AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(dialogInterface -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view1 -> {
                String newFileName = textInputEditText.getText() + AppConstants.FILE_NAME_EXTENSION;
                if (!newFileName.equals(currentRecording.getName())) {
                    int frequency = Collections.frequency(getRecordingList(), new File(FILE_PATH, newFileName));
                    if (frequency > 0) {
                        textInputEditText.setError(getString(R.string.rename_error));
                    } else {
                        if (!currentRecording.renameTo(new File(FILE_PATH, File.separator + textInputEditText.getText() + AppConstants.FILE_NAME_EXTENSION))) {
                            Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, getString(R.string.recording_saved), Toast.LENGTH_SHORT).show();
                        }
                        alertDialog.dismiss();
                    }
                } else {
                    alertDialog.dismiss();
                }
            });
        });

        alertDialog.show();
    }

    private ArrayList<File> getRecordingList() {
        ArrayList<File> recordingList = new ArrayList<>();

        if (FILE_PATH != null) {
            File home = new File(FILE_PATH);
            File[] listFiles = home.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                recordingList.addAll(Arrays.asList(listFiles));
            }
        }
        return recordingList;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}