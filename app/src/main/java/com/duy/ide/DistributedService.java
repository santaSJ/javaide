package com.duy.ide;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;

import com.duy.compile.CompileJavaTask;
import com.duy.compile.CompileManager;
import com.duy.ide.file.FileManager;
import com.duy.project.file.java.JavaProjectFolder;
import com.duy.project.utils.ClassUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;


public class DistributedService extends Service {

    private CompileManager mCompileManager;
    private static final String PACKAGE_NAME = "org.delta.distributed";
    private String code;
    private Socket client;

    private final IBinder mBinder = new LocalBinder();
    private JavaProjectFolder mProjectFile;

    private Thread listenerThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return  START_STICKY;
    }

    @Override
    public void onCreate() {
        mCompileManager = new CompileManager(this, getApplication(), new CompileManager.PublishProgressToServer() {
            @Override
            public void onFinish(String result) {
                try {
                    PrintWriter output = new PrintWriter(client.getOutputStream());
                    output.println(result);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
//        code = "package "+ PACKAGE_NAME +";\npublic class Main { public static void main(String[] args) {System.out.println(\"Hello World!!\");}}";
//        compileAndExecuteCode(code);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public DistributedService getService() {
            return DistributedService.this;
        }
    }

    public void stopListening() {
        listenerThread.interrupt();
    }

    public void startListening() {
        listenerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(8080);

                    boolean done = false;
                    while(!done) {
                        client = serverSocket.accept();

                        BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        PrintWriter output = new PrintWriter(client.getOutputStream());

                        String code = "", str;
                        while ((str=input.readLine()) != null) {
                            code = code + str + "\n";
                        }

                        compileAndExecuteCode(code);

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        listenerThread.start();
    }

    public String compileAndExecuteCode(String sourceCode) {
        doCreateProject();
        String path = mProjectFile.getMainClass().getPath(mProjectFile);
        FileManager.saveFile(path, sourceCode);
        runProject();
        return null;
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

    public void runProject() {
//        saveAllFile();
        if (mProjectFile != null) {
            compileJavaProject();
        } else {
            Toast.makeText(this, "You need create project", Toast.LENGTH_SHORT).show();
        }
    }

    private void compileJavaProject() {
        //check main class exist
        if (mProjectFile.getMainClass() == null
                || mProjectFile.getPackageName() == null
                || mProjectFile.getPackageName().isEmpty()
                || !mProjectFile.getMainClass().exist(mProjectFile)) {
            String msg = getString(R.string.main_class_not_define);
            return;
        }
        //check main function exist
        if (!ClassUtil.hasMainFunction(new File(mProjectFile.getMainClass().getPath(mProjectFile)))) {
            SpannableStringBuilder msg = new SpannableStringBuilder(getString(R.string.can_not_find_main_func));
            Spannable clasz = new SpannableString(mProjectFile.getMainClass().getName());
            clasz.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.dark_color_accent))
                    , 0, clasz.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            msg.append(clasz);
            return;
        }
        CompileJavaTask.CompileListener compileListener = new CompileJavaTask.CompileListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onError(Exception e, ArrayList<Diagnostic> diagnostics) {
            }

            @Override
            public void onComplete(final JavaProjectFolder projectFile,
                                   final List<Diagnostic> diagnostics) {
//                Toast.makeText(.this, R.string.compile_success, Toast.LENGTH_SHORT).show();
                try {
                    mCompileManager.executeDex(projectFile, mProjectFile.getDexedClassesFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNewMessage(byte[] chars, int start, int end) {
                onNewMessage(new String(chars, start, end));
            }

            @Override
            public void onNewMessage(String msg) {
            }
        };
        new CompileJavaTask(compileListener).execute(mProjectFile);
    }
}
