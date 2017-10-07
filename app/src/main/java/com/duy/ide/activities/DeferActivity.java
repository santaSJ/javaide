package com.duy.ide.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

import com.duy.WebAppInterface;
import com.duy.ide.BuildConfig;
import com.duy.ide.R;

public class DeferActivity extends AppCompatActivity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_defer);
        webView = (WebView) findViewById(R.id.webview_distributed);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == 1) {
            String result = data.getStringExtra("result");
            if(result != null) {
                if (BuildConfig.VERSION_CODE >= android.os.Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript("sendResult(" + result + ")", null);
                } else {
                    webView.loadUrl("javascript:sendResult(" + result +");");
                }
            }
        }
    }
}
