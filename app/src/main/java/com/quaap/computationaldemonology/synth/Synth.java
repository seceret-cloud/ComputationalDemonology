package com.quaap.computationaldemonology.synth;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tom on 12/2/16.
 */

public abstract class Synth extends Thread {


    protected List<Filter> filters = new ArrayList<>();
    protected int sampleRate = 16000;
    protected int mSnipsPerSample = 5;
    private AudioTrack mAudioTrack;

    private boolean mRun = false;
    private boolean mPause = false;

    protected float mVol = 1f;

    public Synth() {
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

    }

    @Override
    public void run() {
        mRun = true;
        try {
            mAudioTrack.play();
            int entries = (int)(sampleRate/mSnipsPerSample);
            short [] data = new short[entries];

            while (mRun) {
                if (mPause) {
                    sleep(100);
                } else {
                    Arrays.fill(data, (short)0);
                    getData(data);
                    write(data);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public float getMaxVol() {
        return AudioTrack.getMaxVolume();
    }

    public float getVol() {
        return mVol;
    }

    public void setVol(float vol) {
        mVol = vol;
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioTrack.setVolume(mVol);
        } else {
            mAudioTrack.setStereoVolume(mVol,mVol);
        }
    }

    public void incVol(float volInc) {
        mVol += volInc;
        if (mVol>getMaxVol())  mVol = getMaxVol();
        if (mVol<0)  mVol = 0;
        setVol(mVol);
    }

    public void pauseSynth() {
        mPause = true;
    }
    public void unpauseSynth() {
        mPause = false;
    }

    public void stopSynth() {
        mRun = false;
    }

    public boolean isRunning() {
        return mRun;
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public void close() {
        if (mAudioTrack!=null) {
            try {
                mAudioTrack.pause();
                mAudioTrack.flush();
            } finally {
                mAudioTrack.release();
            }
            mAudioTrack=null;
        }

    }


    protected abstract int getData(short[] data);


    private void write(short[] buffer) {

        for(Filter filter: filters) {
            filter.filter(buffer, buffer.length);
        }

        mAudioTrack.write(buffer, 0, buffer.length);
    }
}


