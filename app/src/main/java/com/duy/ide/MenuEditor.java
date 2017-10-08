/*
 *  Copyright (c) 2017 Tran Le Duy
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

package com.duy.ide;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.duy.ide.activities.InstallActivity;
import com.duy.ide.code_sample.activities.SampleActivity;
import com.duy.ide.editor.code.MainActivity;
import com.duy.ide.setting.AppSetting;
import com.duy.ide.setting.SettingsActivity;
import com.duy.ide.utils.DonateUtils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.pluscubed.logcat.ui.LogcatActivity;

/**
 * Handler for menu click
 * Created by Duy on 03-Mar-17.
 */

public class MenuEditor {
    @NonNull
    private MainActivity activity;
    @Nullable
    private EditorControl listener;
    private Menu menu;
    private AppSetting pascalPreferences;
    private Builder builder;

    public MenuEditor(@NonNull MainActivity activity,
                      @Nullable EditorControl listener) {
        this.activity = activity;
        this.builder = activity;
        this.listener = listener;
        pascalPreferences = new AppSetting(this.activity);
    }




    @Nullable
    public EditorControl getListener() {
        return listener;
    }

    public void setListener(@Nullable EditorControl listener) {
        this.listener = listener;
    }

    public boolean getChecked(int action_auto_save) {
        if (menu != null) {
            if (menu.findItem(action_auto_save).isChecked()) {
                return true;
            }
        }
        return false;
    }

}
