package com.zam.recorder.utils;

public class AppConstants {

    //MainActivity
    public static final Integer PERMISSION_REQUEST_CODE = 23;
    public static final String SETTINGS_SHARED_PREFERENCES = "SETTINGS";
    public static final String SHARED_PREF_KEY_DARK_THEME = "DARK_THEME";
    public static final String SHARED_PREF_KEY_NAME_MANUALLY = "NAME_MANUALLY";
    public static final String FILE_NAME_EXTENSION = ".mp4";

    //MainService
    public static final String PACKAGE_NAME = "com.zam.recorder";
    public static final String CHANNEL_ID_1 = "CHANNEL_ID_1";
    public static final String SHARED_PREF_KEY_SAMPLE_RATE = "SAMPLE_RATE";
    public static final String FILE_NAME_EXTENSION_ZAM = "zam.mp4";

    //RecordingActivity
    public static final String DEVICE_FOLDER_NAME = "/Recording (zikr.and.mehr)";

    //RecordingService
    public static final String CHANNEL_ID_2 = "CHANNEL_ID_2";

    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}

