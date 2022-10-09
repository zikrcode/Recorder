package com.zam.recorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreenActivity extends AppCompatActivity {

    private String [] permissions;
    private boolean permissionAccepted = true;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_splash_screen);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            permissions= new String[]{Manifest.permission.RECORD_AUDIO};
        }
        else{
            permissions= new String[]{Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,};
        }

        ActivityCompat.requestPermissions(SplashScreenActivity.this, permissions, 23);

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent=new Intent(SplashScreenActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        },1500);
    }
}

/*private void setTheme(boolean darkTheme){
        WindowInsetsControllerCompat wicc = new WindowInsetsControllerCompat(getWindow(),getWindow().getDecorView());
        if (darkTheme){
            //wicc.setAppearanceLightStatusBars(true);
            getWindow().setStatusBarColor(getColor(R.color.dark_background));
            //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.dark_background));
        }
        else {
            //wicc.setAppearanceLightStatusBars(false);
            getWindow().setStatusBarColor(getColor(R.color.white));
            this.getWindow().getDecorView().setBackgroundColor(getColor(R.color.white));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 23){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {permissionAccepted = false;}
            if (grantResults.length==2){
                if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {permissionAccepted = false;}
            }
        }
    }*/