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
import androidx.appcompat.widget.Toolbar;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.zam.recorder.utils.AppConstants;

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
        setContentView(R.layout.activity_settings);

        sharedPreferences = getApplicationContext().getSharedPreferences(AppConstants.SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);

        tSA = findViewById(R.id.tSA);
        llQuality = findViewById(R.id.llQuality);
        tvSampleQualityDetail = findViewById(R.id.tvSampleQualityDetail);

        tvLocation = findViewById(R.id.tvLocation);
        tvSampleQuality = findViewById(R.id.tvSampleQuality);

        sDarkTheme = findViewById(R.id.sDarkTheme);
        sNameManually = findViewById(R.id.sNameManually);

        setupViews();
    }

    private void setupViews() {
        tSA.setTitle(R.string.settings);
        tSA.setTitleTextColor(getColor(R.color.white));
        setSupportActionBar(tSA);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tSA.setNavigationIcon(R.drawable.back);

        llQuality.setOnClickListener(v -> {
            String[] quality = {
                    "8 kHz(phone)",
                    "22 kHz(FM radio)",
                    "44.1 kHz(CD)"
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MaterialThemeDialog);
            builder.setCancelable(false);
            builder.setTitle(getString(R.string.sample_rate));
            builder.setSingleChoiceItems(
                    quality,
                    sharedPreferences.getInt(AppConstants.SHARED_PREF_KEY_SAMPLE_RATE, 0),
                    (dialog, which) -> sampleRate = which
            );
            builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                editor = sharedPreferences.edit();
                editor.putInt(AppConstants.SHARED_PREF_KEY_SAMPLE_RATE, sampleRate);
                editor.apply();
                dialog.dismiss();
            });
            builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

            builder.show();
        });

        tvLocation.setText(getFilePath());

        setTheme(sharedPreferences.getBoolean(AppConstants.SHARED_PREF_KEY_DARK_THEME, false));

        sDarkTheme.setChecked(sharedPreferences.getBoolean(AppConstants.SHARED_PREF_KEY_DARK_THEME, false));
        sDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> setTheme(isChecked));

        sNameManually.setChecked(sharedPreferences.getBoolean(AppConstants.SHARED_PREF_KEY_NAME_MANUALLY, false));
        sNameManually.setOnCheckedChangeListener((buttonView, isChecked) -> setNameManually(isChecked));

    }

    private String getFilePath() {
        File folder;
        String filePath;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            folder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + AppConstants.DEVICE_FOLDER_NAME);
            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        } else {
            folder = new File(Environment.getExternalStorageDirectory() + AppConstants.DEVICE_FOLDER_NAME);
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        if (!folder.exists()) {
            folder.mkdir();
        }

        return filePath + File.separator + "Recording (zikr.and.mehr)" + File.separator;
    }

    private void setTheme(boolean darkTheme) {
        editor = sharedPreferences.edit();
        if (darkTheme) {
            editor.putBoolean(AppConstants.SHARED_PREF_KEY_DARK_THEME, true);
            SettingsActivity.this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.dark_background));
            tvSampleQuality.setTextColor(getColor(R.color.white));
            tvSampleQualityDetail.setTextColor(getColor(R.color.white));
            sDarkTheme.setTextColor(getColor(R.color.white));
            sNameManually.setTextColor(getColor(R.color.white));
        } else {
            editor.putBoolean(AppConstants.SHARED_PREF_KEY_DARK_THEME, false);
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
        editor.putBoolean(AppConstants.SHARED_PREF_KEY_NAME_MANUALLY, nameManually);
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