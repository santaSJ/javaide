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

package com.duy.ide.editor.code;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.duy.JavaApplication;
import com.duy.compile.BuildApkTask;
import com.duy.compile.BuildJarAchieveTask;
import com.duy.compile.CompileJavaTask;
import com.duy.compile.CompileManager;
import com.duy.compile.diagnostic.DiagnosticFragment;
import com.duy.ide.Builder;
import com.duy.ide.DistributedService;
import com.duy.ide.R;
import com.duy.ide.autocomplete.AutoCompleteProvider;
import com.duy.ide.autocomplete.model.Description;
import com.duy.ide.autocomplete.util.JavaUtil;
import com.duy.ide.code_sample.activities.DocumentActivity;
import com.duy.ide.code_sample.activities.SampleActivity;
import com.duy.ide.editor.code.view.EditorView;
import com.duy.ide.editor.uidesigner.inflate.DialogLayoutPreview;
import com.duy.ide.file.FileManager;
import com.duy.ide.setting.AppSetting;
import com.duy.ide.themefont.activities.ThemeFontActivity;
import com.duy.ide.utils.RootUtils;
import com.duy.project.file.android.AndroidProjectFolder;
import com.duy.project.file.java.ClassFile;
import com.duy.project.file.java.JavaProjectFolder;
import com.duy.project.file.java.ProjectManager;
import com.duy.project.utils.ClassUtil;
import com.duy.run.dialog.DialogRunConfig;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;

import static com.duy.compile.CompileManager.RESULT_DISTRIBUTED;
import static com.duy.run.activities.ExecuteActivity.KEY_RESULT;

public class MainActivity extends ProjectManagerActivity implements
        DrawerLayout.DrawerListener,
        DialogRunConfig.OnConfigChangeListener,
        Builder, ServiceConnection {
    public static final int REQUEST_CODE_SAMPLE = 1015;

    private static final String TAG = "MainActivity";

    private CompileManager mCompileManager;
    private AutoCompleteProvider mAutoCompleteProvider;

    private static final String PACKAGE_NAME = "org.delta.distributed";
    private String code;

    private DistributedService service;

    private void populateAutoCompleteService(AutoCompleteProvider provider) {
        mPagePresenter.setAutoCompleteProvider(provider);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mCompileManager = new CompileManager(this);
////        mMenuEditor = new MenuEditor(this, this);
//
//        Intent intent = getIntent();
//        code = intent.getStringExtra("code");
////        initView(savedInstanceState);
////
////        startAutoCompleteService();
////
////        code = "package "+ PACKAGE_NAME +";\npublic class Main { public static void main(String[] args) {System.out.println(\"Hello World!!\");}}";
////
////        doCreateProject();
//        compileAndExecuteCode(code);
    }


    private void doCreateProject() {
        if (true) {
            JavaProjectFolder projectFile = new JavaProjectFolder(
                    new File(FileManager.EXTERNAL_DIR),
                    PACKAGE_NAME + "." + "Main",
                    PACKAGE_NAME, "DistributedAndroid",
                    new File(getFilesDir(), "system/classes/android.jar").getPath());
            try {
                projectFile.createMainClass();
                mProjectFile = projectFile;
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Can not create project. Error " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }



//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        return mMenuEditor.onOptionsItemSelected(item);
//    }

//    @Override
//    public void invalidateOptionsMenu() {
//        super.invalidateOptionsMenu();
//    }

    @Override
    public void onKeyClick(View view, String text) {
        EditorFragment currentFragment = mPageAdapter.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.insert(text);
        }
    }

    @Override
    public void onKeyLongClick(String text) {
        EditorFragment currentFragment = mPageAdapter.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.insert(text);
        }
    }

    /**
     * create dialog find and replace
     */
    @Override
    public void runProject() {
//        saveAllFile();
        if (mProjectFile != null) {
            compileJavaProject();
        } else {
            Toast.makeText(this, "You need create project", Toast.LENGTH_SHORT).show();
        }
    }



    public String compileAndExecuteCode(String sourceCode) {
        doCreateProject();
        String path = mProjectFile.getMainClass().getPath(mProjectFile);
        FileManager.saveFile(path, sourceCode);
        runProject();
        return null;
    }

    private void compileJavaProject() {
        //check main class exist
        if (mProjectFile.getMainClass() == null
                || mProjectFile.getPackageName() == null
                || mProjectFile.getPackageName().isEmpty()
                || !mProjectFile.getMainClass().exist(mProjectFile)) {
            String msg = getString(R.string.main_class_not_define);
            Snackbar.make(findViewById(R.id.coordinate_layout), msg, Snackbar.LENGTH_LONG)
                    .setAction(R.string.config, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showDialogRunConfig();
                        }
                    }).show();
            return;
        }
        //check main function exist
        if (!ClassUtil.hasMainFunction(new File(mProjectFile.getMainClass().getPath(mProjectFile)))) {
            SpannableStringBuilder msg = new SpannableStringBuilder(getString(R.string.can_not_find_main_func));
            Spannable clasz = new SpannableString(mProjectFile.getMainClass().getName());
            clasz.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.dark_color_accent))
                    , 0, clasz.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            msg.append(clasz);
            Snackbar.make(findViewById(R.id.coordinate_layout), msg, Snackbar.LENGTH_LONG)
                    .setAction(R.string.config, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showDialogRunConfig();
                        }
                    }).show();
            return;
        }
        CompileJavaTask.CompileListener compileListener = new CompileJavaTask.CompileListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onError(Exception e, ArrayList<Diagnostic> diagnostics) {
                Toast.makeText(MainActivity.this, R.string.failed_msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete(final JavaProjectFolder projectFile,
                                   final List<Diagnostic> diagnostics) {
                Toast.makeText(MainActivity.this, R.string.compile_success, Toast.LENGTH_SHORT).show();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mCompileManager.executeDex(projectFile, mProjectFile.getDexedClassesFile());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 200);
            }

            @Override
            public void onNewMessage(byte[] chars, int start, int end) {
                onNewMessage(new String(chars, start, end));
            }

            @Override
            public void onNewMessage(String msg) {
                mMessagePresenter.append(msg);
            }
        };
        new CompileJavaTask(compileListener).execute(mProjectFile);
    }


    /**
     * replace dialog find
     */

    public String getCode() {
        EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
        if (editorFragment != null) {
            return editorFragment.getCode();
        }
        return "";
    }


    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, DistributedService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    @Override
    public void onSharedPreferenceChanged(@NonNull SharedPreferences sharedPreferences, @NonNull String s) {
        if (s.equals(getString(R.string.key_show_suggest_popup))
                || s.equals(getString(R.string.key_show_line_number))
                || s.equals(getString(R.string.key_pref_word_wrap))) {
            EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
            if (editorFragment != null) {
                editorFragment.refreshCodeEditor();
            }
        } else if (s.equals(getString(R.string.key_show_symbol))) {
            if (mContainerSymbol != null) {
                mContainerSymbol.setVisibility(getPreferences().isShowListSymbol()
                        ? View.VISIBLE : View.GONE);
            }
        } else if (s.equals(getString(R.string.key_show_suggest_popup))) {
            EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
            if (editorFragment != null) {
                EditorView editor = editorFragment.getEditor();
                editor.setSuggestData(new ArrayList<Description>());
            }
        }
        //toggle ime/no suggest mode
        else if (s.equalsIgnoreCase(getString(R.string.key_ime_keyboard))) {
            EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
            if (editorFragment != null) {
                EditorView editor = editorFragment.getEditor();
                editorFragment.refreshCodeEditor();
            }
        } else {
            super.onSharedPreferenceChanged(sharedPreferences, s);
        }
    }

    /**
     * show dialog create new source file
     */




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SAMPLE:
                if (resultCode == RESULT_OK) {
                    final JavaProjectFolder projectFile = (JavaProjectFolder)
                            data.getSerializableExtra(SampleActivity.PROJECT_FILE);
                    if (projectFile != null) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                onProjectCreated(projectFile);
                            }
                        }, 100);
                    }
                }
                break;
            case RESULT_DISTRIBUTED:
                String result = data.getStringExtra(KEY_RESULT);
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                Intent i = new Intent();
                i.putExtra("result", result);
                setResult(1);
                finish();
                break;
        }
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(View drawerView) {
        closeKeyBoard();
    }

    @Override
    public void onDrawerClosed(View drawerView) {

    }



    @Override
    public void runFile(String filePath) {
//        saveCurrentFile();
        if (mProjectFile == null) return;
        boolean canRun = ClassUtil.hasMainFunction(new File(filePath));
        if (!canRun) {
            Toast.makeText(this, (getString(R.string.main_not_found)), Toast.LENGTH_SHORT).show();
            return;
        }
        String className = JavaUtil.getClassName(mProjectFile.dirJava, filePath);
        if (className == null) {
            Toast.makeText(this, ("Class \"" + filePath + "\"" + "invalid"), Toast.LENGTH_SHORT).show();
            return;
        }
        mProjectFile.setMainClass(new ClassFile(className));
        runProject();
    }

    @Override
    public void previewLayout(String path) {
//        saveCurrentFile();
        File currentFile = getCurrentFile();
        if (currentFile != null) {
            DialogLayoutPreview dialogPreview = DialogLayoutPreview.newInstance(currentFile);
            dialogPreview.show(getSupportFragmentManager(), DialogLayoutPreview.TAG);
        } else {
            Toast.makeText(this, "Can not find file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)
                || mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            if (mContainerOutput.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                mContainerOutput.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                return;
            } else {
                mDrawerLayout.closeDrawers();
                return;
            }
        }

//        /*
//          check can undo
//         */
//        if (getPreferences().getBoolean(getString(R.string.key_back_undo))) {
//            undo();
//            return;
//        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.exit)
                .setMessage(R.string.exit_mgs)
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create().show();
    }

    public void showDialogRunConfig() {
        if (mProjectFile != null) {
            DialogRunConfig dialogRunConfig = DialogRunConfig.newInstance(mProjectFile);
            dialogRunConfig.show(getSupportFragmentManager(), DialogRunConfig.TAG);
        } else {
            Toast.makeText(this, "Please create project", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConfigChange(JavaProjectFolder projectFile) {
        this.mProjectFile = projectFile;
        if (projectFile != null) {
            ProjectManager.saveProject(this, projectFile);
        }
    }



    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        DistributedService.LocalBinder binder = (DistributedService.LocalBinder) iBinder;
        service = binder.getService();

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service = null;
    }
}