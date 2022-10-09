package com.zam.recorder;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        sharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS",MODE_PRIVATE);
        setTheme(sharedPreferences.getBoolean("DARK_THEME", false));

        tMa=findViewById(R.id.tMA);
        tMa.setTitleTextColor(getColor(R.color.white));
        setSupportActionBar(tMa);

        ivStart = findViewById(R.id.ivStart);
        ivStart.setOnTouchListener(this);

        tvState = findViewById(R.id.tvState);

        ivRecordings = findViewById(R.id.ivRecordings);
        ivRecordings.setOnTouchListener(this);

        bDelete = findViewById(R.id.bCancel);
        bDelete.setOnClickListener(this);

        bSave = findViewById(R.id.bSave);
        bSave.setOnClickListener(this);

        chronometer = findViewById(R.id.chronometer);

        intent = new Intent(MainActivity.this, MainService.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTheme(sharedPreferences.getBoolean("DARK_THEME", false));

        if (!serviceBound){
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private void setTheme(boolean darkTheme){
        if (darkTheme){
            this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.dark_background));
        }
        else {
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
        Intent intentSettings = new Intent(MainActivity.this, SettingsActivity.class);
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
                            }
                            else {
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
                        }
                        else{
                            if (resume) {
                                ivStart.setImageDrawable(getDrawable(R.drawable.recording_pause));
                                tvState.setText(getString(R.string.resume));
                                mainService.pauseRecording();
                                resume = false;
                            }
                            else {
                                ivStart.setImageDrawable(getDrawable(R.drawable.recording_resume));
                                tvState.setText(getString(R.string.pause));
                                mainService.resumeRecording();
                                resume = true;
                            }
                        }
                    }
                    else {
                        showPermissionNeeded();
                    }
                    break;

                case R.id.ivRecordings:
                    unbindService(connection);
                    stopService(intent);
                    serviceBound=false;
                    Intent intent = new Intent(MainActivity.this, RecordingsActivity.class);
                    startActivity(intent);
                    break;
            }
        }
        return true;
    }

    private boolean isPermissionGranted() {
        boolean permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            permissions= checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
        else{
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
        if (v.getId()==R.id.bCancel){
            cancelRecording();
        }
        if (v.getId()==R.id.bSave){
            saveRecording();
            if (sharedPreferences.getBoolean("NAME_MANUALLY", false)) {
                renameRecording();
            }
            else {
                Toast.makeText(this, "Recording Saved", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cancelRecording() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,  R.style.MaterialThemeDialog);
        builder.setTitle(R.string.cancel_);
        builder.setMessage(R.string.cancel_recording);
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String fileName= mainService.getFileName();
                saveRecording();
                if (! new File(fileName).delete()){
                    Toast.makeText(MainActivity.this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                }
                dialogInterface.cancel();
            };
        });

        builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

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
        currentRecording=new File(MainService.fileName);
        FILE_PATH=getFilesDir().getAbsolutePath();
        View view = getLayoutInflater().inflate(R.layout.rename_recording, null);
        EditText textInputEditText = (EditText) view.findViewById(R.id.etName);
        textInputEditText.setText(currentRecording.getName().substring(0,currentRecording.getName().length()-4));

        AlertDialog.Builder builder = new AlertDialog.Builder(this,  R.style.MaterialThemeDialog);
        builder.setTitle(R.string.save_recording);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.ok), null);
        builder.setNegativeButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (! new File(MainService.fileName).delete()){
                    Toast.makeText(MainActivity.this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                }
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button button = ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newFileName=textInputEditText.getText()+".mp4";
                        if (!newFileName.equals(currentRecording.getName())){
                            int frequency=Collections.frequency(getRecordingList(), new File(FILE_PATH,newFileName));
                            if (frequency>0) {
                                textInputEditText.setError(getString(R.string.rename_error));
                            }
                            else {
                                if (! currentRecording.renameTo(new File(FILE_PATH,File.separator+textInputEditText.getText()+".mp4")) ) {
                                    Toast.makeText(MainActivity.this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    Toast.makeText(MainActivity.this, "Recording Saved", Toast.LENGTH_SHORT).show();
                                }
                                alertDialog.dismiss();
                            }
                        }
                        else {
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });

        alertDialog.show();
    }

    private ArrayList<File> getRecordingList() {
        ArrayList<File> recordingList=new ArrayList<>();

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