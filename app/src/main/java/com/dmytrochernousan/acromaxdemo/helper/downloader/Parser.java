package com.dmytrochernousan.acromaxdemo.helper.downloader;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.dmytrochernousan.acromaxdemo.helper.common.States;
import com.dmytrochernousan.acromaxdemo.helper.writer.ChunkWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;




import static java.lang.Integer.parseInt;


public class Parser {

    private static final String BYTERANGE_PREFIX = "#EXT-X-BYTERANGE:";
    private static final String AUDIO_TYPE = "#EXT-X-MEDIA:TYPE=AUDIO";
    private static final String AUDIO_EXT = ".ts";

    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    private Handler handler;
    private String indexUrl;
    private String urlBase;
    private Context context;
    private volatile int currentChunk = 0;
    private int totalChunks;

    public Parser(Handler handler, Context context, String indexUrl) {
        this.handler = handler;
        this.indexUrl = indexUrl;
        this.context = context;
        this.urlBase = indexUrl.substring(0, indexUrl.lastIndexOf("/"));
    }

    public void downloadIndex() {
        DescriptorDownloaderThread indexDownloader = new DescriptorDownloaderThread(context, new IndexDownloadHandler(handler));
        indexDownloader.execute(indexUrl);
    }

    public void downloadAudioIndex(String url) {
        DescriptorDownloaderThread audioIndexDownloader = new DescriptorDownloaderThread(context, new AudioDownloadHandler(handler));
        audioIndexDownloader.execute(url);
    }

    private class IndexDownloadHandler extends URLDownloadHandler {
        IndexDownloadHandler(Handler handler) {
            super(handler);
        }
        @Override
        public void handle(File file) {
            try {
                String audioFile = parseIndex(file);
                handler.obtainMessage(States.READ_INFO.ordinal(), audioFile).sendToTarget();
            } catch (Exception e) {
                error(e);
            }
        }
    }

    private class AudioDownloadHandler extends URLDownloadHandler {
        AudioDownloadHandler(Handler handler) {
            super(handler);
        }
        @Override
        public void handle(File file) {
            try {
                Map<String, ArrayList<Integer[]>> result = parseAudio(file);
                handler.obtainMessage(States.START_CHUNK_DOWNLOAD.ordinal(), result).sendToTarget();
            } catch (Exception e) {
                error(e);
            }
        }
    }

    private class CompleteChunkDownloadHandler extends URLDownloadHandler {
        CompleteChunkDownloadHandler(Handler handler) {
            super(handler);
        }
        @Override
        public void handle(File file) {
            Integer obj[] = {currentChunk, totalChunks};
            synchronized (this) {
                handler.obtainMessage(States.SPINNER_SHOW.ordinal(), obj).sendToTarget();
                if (++currentChunk == totalChunks) {
                    handler.obtainMessage(States.FINISH_CHUNK_DOWNLOAD.ordinal(), file).sendToTarget();
                }
            }
        }
    }

    public void startChunksDownload(Map<String, ArrayList<Integer[]>> filenames) {
        String fileName="";
        for ( String key : filenames.keySet() ) {
            totalChunks = filenames.get(key).size();
            fileName = key;
        }
        try {


            ChunkWriter writer = new ChunkWriter(context);
            for (int i = 0; i < totalChunks; i++) {
                ChunkDownloadThread thread = new ChunkDownloadThread(urlBase+"/"+fileName, new CompleteChunkDownloadHandler(handler),i, writer, filenames.get(fileName).get(i) );
               executor.execute(thread);
            }
        } catch (IOException e) {
            handler.obtainMessage(States.ERROR.ordinal(),e).sendToTarget();
        }
    }

    private Map<String, ArrayList<Integer[]>> parseAudio(File file) throws Exception {
        Integer startByte = 0;
        Integer offsetByte = 0;
        ArrayList<Integer[]> arrayByteRange = new ArrayList<>();
        BufferedReader b = new BufferedReader(new FileReader(file));
        String readLine;
        Map<String, ArrayList<Integer[]>> hashMapfileNameWithRange = new HashMap<>();
        String fileName = "";
        while ((readLine = b.readLine()) != null) {
            if (readLine.endsWith(AUDIO_EXT)) {
                fileName = readLine;
            }
            if (readLine.contains(BYTERANGE_PREFIX)) {
                Integer[] byteRange = new Integer[2];
                readLine = readLine.substring(BYTERANGE_PREFIX.length());
                int position = readLine.indexOf("@");
                try {
                    startByte = parseInt(readLine.substring(position+1));
                    offsetByte = parseInt(readLine.substring(0, position));
                } catch (Exception ignored) {}
                byteRange[0] = startByte;
                byteRange[1] = startByte + offsetByte;
                arrayByteRange.add(byteRange);
            }
        }
        hashMapfileNameWithRange.put(fileName, arrayByteRange);
        return hashMapfileNameWithRange;
    }

    private String parseIndex(File file) throws Exception {
        List<String> result = new ArrayList();
        List<Integer> bitrate = new ArrayList<>();
        BufferedReader b = new BufferedReader(new FileReader(file));
        String readLine;
        String maxQ = null;
        while ((readLine = b.readLine()) != null) {
            if (readLine.startsWith(AUDIO_TYPE)) {
                readLine = readLine.substring(readLine.indexOf("URI=") + 4);
                try {
                    int end = readLine.indexOf("K_v4");
                    bitrate.add(parseInt(readLine.substring(6, end)));
                } catch (Exception e) {}
                maxQ =  Collections.max(bitrate).toString();
                result.add(readLine);
            }
        }
        return (urlBase + "/" + "hls_a" + maxQ + "K_v4.m3u8" );
    }


    public class DescriptorDownloaderThread extends AsyncTask<String, Integer, String> {

        private Context context;
        private URLDownloadHandler handler;
        private File file;

        DescriptorDownloaderThread(Context context, URLDownloadHandler handler) {
            this.context = context;
            this.handler=handler;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                file=Downloader.downloadFileInfo(sUrl[0],context);
            } catch (Exception e) {
                handler.error(e);
            }
            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            handler.handle(file);
        }
    }

}
