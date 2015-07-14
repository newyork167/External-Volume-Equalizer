package com.atomiconsoftware.externalvolumeequalizer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.lang.String;

public class MainActivity extends ActionBarActivity {

    public static final String PREFS_NAME = "MyPrefsFile";

    protected SoundMeter meter;
    protected double oldAverageAmplitude;
    protected double averageAmplitude;
    protected double totalAmplitude;
    protected long amplitudeCount;
    protected int minVolume;
    protected int maxVolume;
    protected SeekBar seekBar;
    protected double previousAmplitude = 0;
    protected double currentAmplitude;
    protected Button resetAverageButton;
    protected Button onOffButton;
    protected boolean onOff = true;
    protected SeekBar maxVolumeSeekBar;
    protected int refreshRate = 250;
    protected SeekBar amplitudeThresholdSeekBar;
    protected int threshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        threshold = settings.getInt("threshold", 250);
        amplitudeThresholdSeekBar = (SeekBar)findViewById(R.id.amplitudeThresholdSeekBar);
        amplitudeThresholdSeekBar.setProgress(threshold);
        amplitudeThresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView thresholdTextView = (TextView) findViewById(R.id.amplitudeThresholdTextView);
                thresholdTextView.setText("Amplitude Threshold: " + progress);
                threshold = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        maxVolume = settings.getInt("maxVolume", 15);
        maxVolumeSeekBar = (SeekBar)findViewById(R.id.maxVolumeSeekBar);
        maxVolumeSeekBar.setProgress(maxVolume);
        maxVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView refreshTextView = (TextView)findViewById(R.id.RefreshRateTextView);
                refreshTextView.setText("Max Volume: " + Integer.toString(progress));
                maxVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        resetAverageButton = (Button)findViewById(R.id.resetAverageButton);
        resetAverageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                averageAmplitude = 0;
                totalAmplitude = 0;
                oldAverageAmplitude = 0;
                currentAmplitude = 0;
                previousAmplitude = 0;
                amplitudeCount = 0;
            }
        });

        onOffButton = (Button)findViewById(R.id.onOffButton);
        onOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOff = !onOff;
                onOffButton.setText(onOff ? "On" : "Off");
            }
        });

        minVolume = settings.getInt("minimumVolume", 7);
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setProgress(minVolume);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minVolume = progress;
                TextView minVolumeText = (TextView)findViewById(R.id.MinVolumeTextView);
                minVolumeText.setText("Minimum Volume: " + Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        averageAmplitude = 0.0;
        oldAverageAmplitude = 0.0;
        amplitudeCount = 0;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (meter == null) {
            meter = new SoundMeter();
            meter.start();
        }

        final Handler h = new Handler();

        h.postDelayed(new Runnable() {
            public void run() {
                //do something
                h.postDelayed(this, 500);
                AsyncMeter asMeter = new AsyncMeter();
                asMeter.execute();
            }
        }, refreshRate);
    }

    protected void onStart(){
        super.onStart();

        if (meter == null){
            meter = new SoundMeter();
            meter.start();
        }
        else
            meter.start();

        setupTextViews();
    }

    protected void setupTextViews(){
        TextView refreshTextView = (TextView)findViewById(R.id.RefreshRateTextView);
        refreshTextView.setText("Max Volume: " + Integer.toString(maxVolume));

        TextView thresholdTextView = (TextView)findViewById(R.id.amplitudeThresholdTextView);
        thresholdTextView.setText("Amplitude Threshold: " + Integer.toString(threshold));

        TextView minVolumeText = (TextView)findViewById(R.id.MinVolumeTextView);
        minVolumeText.setText("Minimum Volume: " + Integer.toString(minVolume));
    }

    @Override
    protected void onStop(){
        super.onStop();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("minimumVolume", minVolume);
        editor.putInt("refreshRate", refreshRate);
        editor.putInt("maxVolume", maxVolume);
        editor.putInt("threshold", threshold);

        meter.stop();

        // Commit the edits!
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class AsyncMeter extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params){
            return Double.toString(meter.getAmplitude());
        }

        protected void onPostExecute(String result){
            if (!onOff)
                return;

            TextView tv = (TextView)findViewById(R.id.aplitudeTextView);
            TextView aTv = (TextView)findViewById(R.id.averageAmplitudeTextView);

            currentAmplitude = Double.parseDouble(result);

            if (amplitudeCount > 10){
                resetAverageButton.callOnClick();
                amplitudeCount = 0;
            }

            if (true) {
                amplitudeCount++;
                totalAmplitude += Double.parseDouble(result);
                averageAmplitude = totalAmplitude / amplitudeCount;
                tv.setText("Current Amplitude:  \t\t" + String.format("%.2f", Double.parseDouble(result)));
                aTv.setText("Average Amplitude: \t\t" + String.format("%.2f", averageAmplitude));

                setVolume();
            }

            previousAmplitude = currentAmplitude;
        }

        protected void setVolume(){
            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (averageAmplitude > threshold) {
                if (volume < maxVolume)
                    volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + 1;
            }
            else
                volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1;

            if (volume < minVolume)
                volume = minVolume;
            else if (volume > maxVolume)
                volume = maxVolume;

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);

            TextView currentVolumeTV = (TextView)findViewById(R.id.currentVolumeTextView);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            currentVolumeTV.setText("Current Volume: \t\t\t\t\t" + Integer.toString(currentVolume));
        }
    }
}
