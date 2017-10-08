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

package com.duy.compile;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.duy.JavaApplication;
import com.duy.compile.external.CompileHelper;
import com.duy.ide.debug.activities.DebugActivity;
import com.duy.ide.editor.code.MainActivity;
import com.duy.project.file.java.JavaProjectFolder;
import com.duy.run.activities.ExecuteActivity;
import com.duy.run.view.ConsoleEditText;

import java.io.File;
import java.io.InputStream;

import static android.content.Context.MODE_PRIVATE;
import static com.duy.compile.external.CompileHelper.Action.RUN_DEX;

/**
 * Created by Duy on 11-Feb-17.
 */

public class CompileManager implements ConsoleEditText.StdOutListener {

    public static final String FILE_PATH = "file_name";     // extras indicators
    public static final String IS_NEW = "is_new";
    public static final String INITIAL_POS = "initial_pos";
    public static final int ACTIVITY_EDITOR = 1001;
    public static final String MODE = "run_mode";

    public static final String PROJECT_FILE = "project_file";
    public static final String ACTION = "action";
    public static final String ARGS = "program_args";
    public static final String DEX_FILE = "dex_path";

    public static final int RESULT_DISTRIBUTED = 1010;

    private ConsoleEditText mConsoleEditText;
    private final Activity mActivity;

    public CompileManager(Activity activity) {
        this.mActivity = activity;
        mConsoleEditText = new ConsoleEditText(mActivity);
        mConsoleEditText.init(mActivity, this);
        initInOut();
    }

    private void initInOut() {
        JavaApplication application = (JavaApplication) mActivity.getApplication();
        application.addStdErr(mConsoleEditText.getErrorStream());
        application.addStdOut(mConsoleEditText.getOutputStream());
    }


    public void debug(String name) {
        Intent intent = new Intent(mActivity, DebugActivity.class);
        intent.putExtra(FILE_PATH, name);
        mActivity.startActivity(intent);
    }

    public void edit(String fileName, Boolean isNew) {
        Intent intent = new Intent(mActivity, MainActivity.class);
        intent.putExtra(FILE_PATH, fileName);
        intent.putExtra(IS_NEW, isNew);
        intent.putExtra(INITIAL_POS, 0);
        mActivity.startActivityForResult(intent, ACTIVITY_EDITOR);
    }


    public void executeDex(final JavaProjectFolder projectFile, final File dex) {
//        Intent intent = new Intent(mActivity, ExecuteActivity.class);
//        intent.putExtra(ACTION, CompileHelper.Action.RUN_DEX);
//        intent.putExtra(PROJECT_FILE, projectFile);
//        intent.putExtra(DEX_FILE, dex);
//        mActivity.startActivityForResult(intent, RESULT_DISTRIBUTED);
        Thread runThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runProgram(projectFile, RUN_DEX, dex);
                } catch (Error error) {
                    error.printStackTrace(mConsoleEditText.getErrorStream());
                } catch (Exception e) {
                    e.printStackTrace(mConsoleEditText.getErrorStream());
                } catch (Throwable e) {
                    e.printStackTrace(mConsoleEditText.getErrorStream());
                }
            }
        });
        runThread.start();
///*        try {
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }*/
    }

    private void runProgram(JavaProjectFolder projectFile, int action, File dex) throws Exception {
        InputStream in = mConsoleEditText.getInputStream();

        File tempDir = mActivity.getDir("dex", MODE_PRIVATE);
        switch (action) {
            case CompileHelper.Action.RUN: {
                CompileHelper.compileAndRun(in, tempDir, projectFile);
            }
            break;
            case CompileHelper.Action.RUN_DEX: {
                if (dex != null) {
                    String mainClass = projectFile.getMainClass().getName();
                    CompileHelper.executeDex(in, dex, tempDir, mainClass);
                }
                break;
            }
        }
    }

    public void buildApk() {

    }

    @Override
    public void onResultGet(String result) {
        Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
    }

    public interface ProcessCallback {
        void onFailed(String s);
    }
}
