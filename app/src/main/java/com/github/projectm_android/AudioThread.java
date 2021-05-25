package com.github.projectm_android;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

import java.util.Arrays;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;

public class AudioThread extends Thread {
    private volatile static boolean keep_recording = true;
    private volatile static boolean currently_recording = false;

    @Override
    public void run() {
        int SAMPLE_RATE = 44100; //22050; //11025; //When sample rate does not evenly divide 44.1kHz, slowdown
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, // bufferSize in BYTES
                CHANNEL_IN_MONO,
                ENCODING_PCM_16BIT);
        //bufferSize = bufferSize*2;
        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                CHANNEL_IN_MONO,
                ENCODING_PCM_16BIT,
                bufferSize); // in BYTES
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.d("AudioThread","ERROR in bufferSize, size = samplerate * 2");
            bufferSize = SAMPLE_RATE * 2;
        }
        short[] audioBuffer = new short[bufferSize/2]; // each SHORT is 2 BYTES

        Log.d("AudioThread",String.format("bufferSize: %d", bufferSize));

        while (currently_recording) {
            Log.d("AudioThread","currently recoring, waiting 50ms before attempting again");
            // bufferSize is usually worth 80ms of audio
            // So waiting 50ms - this means, next attempt to grab AudioRecord will succeed.
            SystemClock.sleep(50);
        }
        record.startRecording();
        currently_recording = true;

        while (keep_recording) {
            record.read(audioBuffer, 0, audioBuffer.length); //audioBuffer.length is the number of shorts
            libprojectMJNIWrapper.addPCM(audioBuffer, (short) audioBuffer.length);
            //short max = 0;
            //Log.d("AUDIOBUFFER", "arr: " + Arrays.toString(audioBuffer));
            //int ix;
            //for (ix=0; ix<audioBuffer.length && audioBuffer[ix] == 0; ix++) {
                //max = (short) Math.max(audioBuffer[ix], max);
            //}
            //Log.i("NUM_ZEROES:", String.valueOf(ix));
            //Log.i("LENGTH:", String.valueOf(audioBuffer.length));
            // GOOD DATA (no leading 0s) IS BEING SENT TO C++ when array is of size bufferSize/2
            // when array is of size bufferSize (twice as large as min), more good data is pulled from record
        }
        record.stop();
        currently_recording = false;
    }

    public void stop_recording() {
        keep_recording = false;
    }

}
