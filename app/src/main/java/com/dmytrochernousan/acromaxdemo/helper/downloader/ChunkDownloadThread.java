package com.dmytrochernousan.acromaxdemo.helper.downloader;

import com.dmytrochernousan.acromaxdemo.helper.writer.ChunkWriter;

public class ChunkDownloadThread  implements Runnable{
    private String filename;
    private URLDownloadHandler handler;
    private int index;
    private ChunkWriter writer;
    private Integer[] byteRange;

    ChunkDownloadThread(String filename, URLDownloadHandler handler, int index, ChunkWriter writer, Integer[] byteRange) {
        this.filename=filename;
        this.handler= handler;
        this.index = index;
        this.writer = writer;
        this.byteRange= byteRange;
    }


    @Override
    public void run() {
        try{
            byte[] chunk = Downloader.downloadChunk(filename, byteRange);
            writer.write(chunk, index);
            System.out.println("Chunk download completed: " + index);
            handler.handle(writer.getCompliteFile());
        } catch (Exception e) {
            handler.error(e);
        }

    }
}
