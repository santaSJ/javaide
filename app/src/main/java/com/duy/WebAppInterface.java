package com.duy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;

import com.duy.ide.editor.code.MainActivity;


public class WebAppInterface {
    Context context;

    public WebAppInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void compileAndExecuteCode(String code) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("code", code);
        if(context instanceof Activity) {
            ((Activity) context).startActivityForResult(intent, 1);
        }
    }
}
