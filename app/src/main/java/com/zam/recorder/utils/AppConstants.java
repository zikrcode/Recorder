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

