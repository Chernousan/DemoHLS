package com.dmytrochernousan.acromaxdemo;

import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dmytrochernousan.acromaxdemo.helper.common.CustomImageView;
import com.dmytrochernousan.acromaxdemo.helper.common.States;
import com.dmytrochernousan.acromaxdemo.helper.deleter.CacheCleaner;
import com.dmytrochernousan.acromaxdemo.helper.downloader.Parser;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements  MediaPlayer.OnCompletionListener{
    public static final String URL = "http://pubcache1.arkiva.de/test/hls_index.m3u8";
    private Parser parser;
    private MediaPlayer player;
    private TextView spinnerText;
    private ProgressBar spinnerBar;
    private int xDelta;
    private ViewGroup mainLayout;
    ImageView fetchImageView;
    ImageView playImageView;
    CustomImageView pauseImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fetchImageView = findViewById(R.id.fetch);

        fetchImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parser = new Parser(handler, getApplicationContext(), URL);
                parser.downloadIndex();
                handler.obtainMessage(States.FETCH.ordinal()).sendToTarget();
            }
        });

        playImageView = findViewById(R.id.play);
        playImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.start();
                playImageView.setVisibility(View.INVISIBLE);
                pauseImageView.setVisibility(View.VISIBLE);
            }
        });

        mainLayout = findViewById(R.id.relativeLayout);
        pauseImageView = mainLayout.findViewById(R.id.pause);
        pauseImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.performClick();
                RelativeLayout.LayoutParams layoutParams;
                final int x = (int) motionEvent.getRawX();

                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        layoutParams = (RelativeLayout.LayoutParams)
                                view.getLayoutParams();
                        xDelta = x - layoutParams.leftMargin;
                        break;
                    case MotionEvent.ACTION_UP:
                        layoutParams = (RelativeLayout.LayoutParams) view
                                .getLayoutParams();
                        if (layoutParams.leftMargin < 30) {
                            player.pause();
                            pauseImageView.setVisibility(View.INVISIBLE);
                            playImageView.setVisibility(View.VISIBLE);
                        } else {
                            speed(1f);
                        }
                        layoutParams.leftMargin = 0;
                        view.setLayoutParams(layoutParams);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        layoutParams = (RelativeLayout.LayoutParams) view
                                .getLayoutParams();
                        if (layoutParams.leftMargin > 30) {
                            speed(2f);
                        }
                        layoutParams.leftMargin = x - xDelta;
                        view.setLayoutParams(layoutParams);
                        break;
                }
                mainLayout.invalidate();
                return true;
            }
        });
        spinnerText = findViewById(R.id.progress);
        player = MediaPlayer.create(this, R.raw.audio_sample);
        player.setLooping(false);
        player.setOnCompletionListener(this);
        spinnerBar = findViewById(R.id.spinner);
        spinnerBar.setVisibility(View.INVISIBLE);
    }

    void speed (Float speed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (player.isPlaying()) {
                player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
            }
        }
    }


    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            States currentStatus = States.values()[inputMessage.what];
            switch (currentStatus) {
                case FETCH: {
                    spinnerBar.setVisibility(View.VISIBLE);
                    spinnerText.setVisibility(View.VISIBLE);
                    fetchImageView.setVisibility(View.INVISIBLE);
                    spinnerText.setText(R.string.loading);
                    break;
                }
                case ERROR: {
                    String e = (String) inputMessage.obj;
                    spinnerText.setVisibility(View.VISIBLE);
                    spinnerText.setText(e);
                    spinnerBar.setVisibility(View.INVISIBLE);
                    spinnerText.setVisibility(View.INVISIBLE);
                    break;
                }
                case SPINNER_SHOW: {
                    Integer[] e = (Integer[]) inputMessage.obj;
                    spinnerBar.setMax(e[1]);
                    spinnerBar.setProgress(e[0]+1);
                    int progres = ((e[0]+1)*100)/(e[1]);
                    Resources res = getResources();
                    String info = String.format(res.getString(R.string.percentage), progres,  e[0]+1);
                    spinnerText.setText(info);

                    break;
                }
                case READ_INFO: {
                    String f = (String) inputMessage.obj;
                    parser.downloadAudioIndex(f);
                    break;
                }
                case START_CHUNK_DOWNLOAD: {
                    Map<String, ArrayList<Integer[]>> f = (Map<String, ArrayList<Integer[]>>) inputMessage.obj;
                    parser.startChunksDownload(f);
                    break;
                }
                case FINISH_CHUNK_DOWNLOAD: {
                    spinnerBar.setVisibility(View.INVISIBLE);
                    spinnerText.setVisibility(View.INVISIBLE);
                    player.start();
                    pauseImageView.setVisibility(View.VISIBLE);
                    break;
                }
                case PLAYER_DONE: {
                    CacheCleaner.deleteCache(MainActivity.this);
                    playImageView.setVisibility(View.INVISIBLE);
                    pauseImageView.setVisibility(View.INVISIBLE);
                    spinnerText.setVisibility(View.VISIBLE);
                    spinnerText.setText(R.string.clearCache);
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            spinnerText.setVisibility(View.INVISIBLE);
                            fetchImageView.setVisibility(View.VISIBLE);
                        }
                    }, 2000);
                    break;
                }
            }
        }
    };

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        handler.obtainMessage(States.PLAYER_DONE.ordinal()).sendToTarget();
    }
}
