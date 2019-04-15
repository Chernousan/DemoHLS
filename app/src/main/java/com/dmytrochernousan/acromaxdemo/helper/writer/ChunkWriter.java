package com.dmytrochernousan.acromaxdemo.helper.writer;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ChunkWriter {

    private  static  final String FILE_NAME = "tmp.ts";
    private volatile int counter = 0;
    private File compliteFile;

    public ChunkWriter(Context context) throws IOException {
        compliteFile = getTempFile(context);

    }

    public void write(byte[] chunk, int index) throws Exception{
        FileOutputStream fos;
        synchronized (this) {
            while (index > counter) {
                wait();
            }
            fos = new FileOutputStream(compliteFile, true);
            fos.write(chunk);
            fos.close();
            counter++;
            notifyAll();
        }
    }

    public File getCompliteFile(){
        return  compliteFile;
    }

    private File getTempFile(Context context) throws IOException{
        File file;
        file = File.createTempFile(FILE_NAME, null, context.getCacheDir());
        return file;
    }
}
