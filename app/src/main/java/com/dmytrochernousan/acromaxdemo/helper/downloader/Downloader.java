package com.dmytrochernousan.acromaxdemo.helper.downloader;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class Downloader {
    public static String TAG = "LOG";

    static File downloadFileInfo(String href, Context context) throws Exception{
        File file;
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(href);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            input = connection.getInputStream();
            file = getTempFile(context);
            output = new FileOutputStream(file);
            byte data[] = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
        } finally {
            if (output != null)
                output.close();
            if (input != null)
                input.close();

            if (connection != null)
                connection.disconnect();
        }
        return file;
    }

    static byte[] downloadChunk(String href, Integer[] byteRange, Integer index) throws Exception{
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        HttpURLConnection connection = null;
        try {
            Log.i(TAG, "Chunk download start: " + index);
            URL url = new URL(href);
            connection = (HttpURLConnection) url.openConnection();
            //connection.setRequestProperty("Range", "bytes=" + byteRange[0] + "-" + byteRange[1]);
            connection.connect();
            InputStream is = connection.getInputStream();
            int nRead;
            byte[] data = new byte[4096];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } finally {
            buffer.close();

            if (connection != null)
                connection.disconnect();
        }
        return buffer.toByteArray();
    }

    private static File getTempFile(Context context) throws IOException {
        File file;
        file = File.createTempFile("info", null, context.getCacheDir());
        return file;
    }
}
