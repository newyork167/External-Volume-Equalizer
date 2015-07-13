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
    protected int minVolume = 5;
    protected int maxVolume = 20;
    protected SeekBar seekBar;
    protected double previousAmplitude = 0;
    protected double currentAmplitude;
    protected Button resetAverageButton;
    protected Button onOffButton;
    protected boolean onOff = true;
    protected SeekBar refreshRateSeekBar;
    protected int refreshRate = 250;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        minVolume = settings.getInt("minimumVolume", 5);

        refreshRateSeekBar = (SeekBar)findViewById(R.id.refreshRateSeekBar);
        refreshRateSeekBar.setProgress(settings.getInt("refreshRate", 250));
        refreshRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView refreshTextView = (TextView)findViewById(R.id.RefreshRateTextView);
                refreshTextView.setText("Refresh Rate: " + Integer.toString(progress) + "ms");
                refreshRate = progress;
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

        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setProgress(minVolume);
        TextView minVolumeText = (TextView)findViewById(R.id.MinVolumeTextView);
        minVolumeText.setText("Minimum Volume: " + Integer.toString(minVolume));
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
                h.postDelayed(this, 250);
                AsyncMeter asMeter = new AsyncMeter();
                asMeter.execute();
            }
        }, refreshRate);
    }

    protected void setupTextViews(){
        TextView refreshTextView = (TextView)findViewById(R.id.RefreshRateTextView);
        refreshTextView.setText("Refresh Rate: " + Integer.toString(refreshRate) + "ms");
    }

    @Override
    protected void onStop(){
        super.onStop();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("minimumVolume", minVolume);
        editor.putInt("refreshRate", refreshRate);

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

            if (previousAmplitude != 0) {
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

            if (averageAmplitude > 250) {
                if (volume < 1.5 * minVolume)
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
            currentVolumeTV.setText("Current Volume:    \t\t\t" + Integer.toString(currentVolume));
        }
    }
}
