package com.dmytrochernousan.acromaxdemo.helper.downloader;

import android.os.Handler;

import com.dmytrochernousan.acromaxdemo.helper.common.States;

import java.io.File;

abstract class URLDownloadHandler {
    Handler handler;

    URLDownloadHandler(Handler handler) {
        this.handler = handler;
    }

    abstract void handle(File file);
    void error (Exception e){
        handler.obtainMessage(States.ERROR.ordinal(), e).sendToTarget();
    }
}
