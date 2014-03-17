/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.achep.activedisplay.settings;

import android.app.FragmentManager;
import android.util.Log;

import com.achep.activedisplay.Project;

/**
 * Stub class for showing sub-settings; we can't use the main Settings class
 * since for our app it is a special singleTask class.
 */
public class SubSettings extends Settings {

    @Override
    public boolean onNavigateUp() {
        if (!popFragment()) {
            finish();
        }
        return true;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (Project.DEBUG) Log.d("SubSettings", "Launching fragment " + fragmentName);
        return true;
    }

    private boolean popFragment() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            return true;
        }
        return false;
    }
}