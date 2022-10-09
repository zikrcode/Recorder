package com.zam.recorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Toolbar tSA;
    private LinearLayout llQuality;
    private TextView tvSampleQuality, tvSampleQualityDetail, tvLocation;
    private SwitchMaterial sDarkTheme, sNameManually;
    private int sampleRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getApplicationContext().getSharedPreferences("SETTINGS",MODE_PRIVATE);

        tSA=findViewById(R.id.tSA);
        tSA.setTitle(R.string.settings);
        tSA.setTitleTextColor(getColor(R.color.white));
        setSupportActionBar(tSA);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tSA.setNavigationIcon(R.drawable.back);

        llQuality=findViewById(R.id.llQuality);
        llQuality.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] quality={"8 kHz(phone)", "22 kHz(FM radio)", "44.1 kHz(CD)"};
                AlertDialog.Builder builder=new AlertDialog.Builder(SettingsActivity.this, R.style.MaterialThemeDialog);
                builder.setCancelable(false);
                builder.setTitle(getString(R.string.sample_rate));
                builder.setSingleChoiceItems(quality, sharedPreferences.getInt("SAMPLE_RATE",0), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sampleRate=which;
                    }
                });
                builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editor = sharedPreferences.edit();
                        editor.putInt("SAMPLE_RATE",sampleRate);
                        editor.apply();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();
            }
        });

        tvSampleQualityDetail=findViewById(R.id.tvSampleQualityDetail);

        tvLocation=findViewById(R.id.tvLocation);
        tvLocation.setText(getFilePath());

        tvSampleQuality=findViewById(R.id.tvSampleQuality);

        sDarkTheme=findViewById(R.id.sDarkTheme);
        sNameManually=findViewById(R.id.sNameManually);
        setTheme(sharedPreferences.getBoolean("DARK_THEME", false));

        sDarkTheme.setChecked(sharedPreferences.getBoolean("DARK_THEME", false));
        sDarkTheme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setTheme(isChecked);
            }
        });

        sNameManually.setChecked(sharedPreferences.getBoolean("NAME_MANUALLY", false));
        sNameManually.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setNameManually(isChecked);
            }
        });
    }

    private String getFilePath() {
        File folder;
        String filePath;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            folder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+ "/Recording (zikr.and.mehr)" );
            filePath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        }
        else{
            folder = new File(Environment.getExternalStorageDirectory() + "/Recording (zikr.and.mehr)");
            filePath=Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        if (!folder.exists()) {
            folder.mkdir();
        }

        return filePath+File.separator+"Recording (zikr.and.mehr)"+File.separator;
    }

    private void setTheme(boolean darkTheme){
        editor = sharedPreferences.edit();
        if (darkTheme){
            editor.putBoolean("DARK_THEME", true);
            SettingsActivity.this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.dark_background));
            tvSampleQuality.setTextColor(getColor(R.color.white));
            tvSampleQualityDetail.setTextColor(getColor(R.color.white));
            sDarkTheme.setTextColor(getColor(R.color.white));
            sNameManually.setTextColor(getColor(R.color.white));
        }
        else {
            editor.putBoolean("DARK_THEME", false);
            SettingsActivity.this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.white));
            tvSampleQuality.setTextColor(getColor(R.color.black));
            tvSampleQualityDetail.setTextColor(getColor(R.color.black));
            sDarkTheme.setTextColor(getColor(R.color.black));
            sNameManually.setTextColor(getColor(R.color.black));
        }
        editor.apply();
    }

    private void setNameManually(boolean nameManually) {
        editor = sharedPreferences.edit();
        editor.putBoolean("NAME_MANUALLY", nameManually);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.settings_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        this.finish();
        return super.onOptionsItemSelected(item);
    }
}

/*
<resources>
    <style name="Theme.ToDoList" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="android:statusBarColor">@color/primaryColor</item>
        <item name="colorPrimary">@color/primaryColor</item>
        <item name="colorPrimaryVariant">@color/primaryDarkColor</item>
        <item name="colorOnPrimary">@color/black</item>
        <item name="android:windowBackground">@color/black</item>
    </style>

    <style name="Theme" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="android:statusBarColor">@color/black</item>
        <item name="android:windowBackground">@color/black</item>
    </style>
</resources>
 */