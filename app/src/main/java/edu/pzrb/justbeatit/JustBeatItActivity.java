package edu.pzrb.justbeatit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class JustBeatItActivity extends AppCompatActivity {

    private static final String TAG = "JustBeatIt";
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private SurfaceView preview = null;
    private SurfaceHolder previewHolder = null;

    private Camera camera = null;
    private JustBeatItView graph = null;

    private TextView status = null;

    private TextView debugLabel = null;

    private PowerManager.WakeLock wakeLock = null;

    private int averageIndex = 0;
    private final int averageArraySize = 4;
    private final int[] averageArray = new int[averageArraySize];

    public enum State {
        NO_BEAT, BEAT, PAUSED
    }

    ;

    private State currentType = State.NO_BEAT;

    private int beatsIndex = 0;
    private final int beatsArraySize = 3;
    private final int[] beatsArray = new int[beatsArraySize];
    private double beats = 0;
    private long startTime = 0;

    private boolean enabled = true;

    private SharedPreferences preferences;
    private String prefThresholdKey;
    private String prefThresholdDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        prefThresholdKey = getString(R.string.preference_threshold_key);
        prefThresholdDefault = getString(R.string.preference_threshold_default);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Camera.Parameters parameters = camera.getParameters();
                if (enabled) {
                    Snackbar.make(view, getString(R.string.scan_pause), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    enabled = false;

                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    currentType = State.PAUSED;

                    fab.setImageResource(android.R.drawable.ic_media_play);

                    graph.setEnabled(false);
                } else {
                    Snackbar.make(view, getString(R.string.scan_resume), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    enabled = true;


                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    currentType = State.NO_BEAT;

                    fab.setImageResource(android.R.drawable.ic_media_pause);

                    graph.setEnabled(true);
                }
                camera.setParameters(parameters);
            }
        });

        preview = (SurfaceView) findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        graph = (JustBeatItView) findViewById(R.id.image);
        status = (TextView) findViewById(R.id.text);

        debugLabel = (TextView) findViewById(R.id.debugText);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
    }


    @Override
    public void onResume() {
        super.onResume();
        wakeLock.acquire();
        startCamera();
    }

    private void startCamera() {
        camera = Camera.open();
        try {
            camera.setPreviewDisplay(previewHolder);
        } catch (IOException e) {
            throw new RuntimeException("setPreviewDisplay failed", e);
        }
        camera.setDisplayOrientation(90);
        startTime = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        wakeLock.release();
        pauseCamera();
    }

    private void pauseCamera() {
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null) {
                throw new NullPointerException();
            }
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) {
                throw new NullPointerException();
            }
            if (currentType == State.PAUSED) {
                return;
            }
            if (!processing.compareAndSet(false, true)) {
                return;
            }

            int imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(), size.width, size.height);
            debugLabel.setText("ImgAvg: " + imgAvg);
            int treshold = Integer.parseInt(preferences.getString(prefThresholdKey, prefThresholdDefault));
            if (imgAvg == 0 || imgAvg == 255) {
                processing.set(false);
                return;
            } else if (imgAvg > treshold) {
                if (status.getText().equals(getString(R.string.action_finger))) {
                    status.setText("--");
                }

                int averageArrayAvg = 0;
                int averageArrayCnt = 0;
                for (int anAverageArray : averageArray) {
                    if (anAverageArray > 0) {
                        averageArrayAvg += anAverageArray;
                        averageArrayCnt++;
                    }
                }

                int rollingAverage = (averageArrayCnt > 0) ? (averageArrayAvg / averageArrayCnt) : 0;
                State newType = currentType;
                if (imgAvg < rollingAverage) {
                    newType = State.BEAT;
                    if (newType != currentType) {
                        beats++;
                        graph.beat();
                    }
                } else if (imgAvg > rollingAverage) {
                    newType = State.NO_BEAT;
                }

                if (averageIndex == averageArraySize) {
                    averageIndex = 0;
                }
                averageArray[averageIndex] = imgAvg;
                averageIndex++;

                // Transitioned from one state to another to the same
                if (newType != currentType) {
                    currentType = newType;
                }

                long endTime = System.currentTimeMillis();
                double totalTimeInSecs = (endTime - startTime) / 1000d;
                if (totalTimeInSecs >= 10) {
                    double bps = (beats / totalTimeInSecs);
                    int dpm = (int) (bps * 60d);
                    if (dpm < 30 || dpm > 180) {
                        startTime = System.currentTimeMillis();
                        beats = 0;
                        processing.set(false);
                        return;
                    }

                    if (beatsIndex == beatsArraySize) {
                        beatsIndex = 0;
                    }
                    beatsArray[beatsIndex] = dpm;
                    beatsIndex++;

                    int beatsArrayAvg = 0;
                    int beatsArrayCnt = 0;
                    for (int aBeatsArray : beatsArray) {
                        if (aBeatsArray > 0) {
                            beatsArrayAvg += aBeatsArray;
                            beatsArrayCnt++;
                        }
                    }
                    int beatsAvg = (beatsArrayAvg / beatsArrayCnt);
                    if (enabled) {
                        status.setText(String.valueOf(beatsAvg));
                    }
                    startTime = System.currentTimeMillis();
                    beats = 0;
                }
            } else {
                // Finger is not placed on camera
                // Reset the previously captured data
                status.setText(R.string.action_finger);
                for (int i = 0; i < averageArraySize; i++) {
                    averageArray[i] = 0;
                }
                for (int i = 0; i < beatsArraySize; i++) {
                    beatsArray[i] = 0;
                }
            }
            processing.set(false);
        }
    };

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(previewHolder);
                camera.setPreviewCallback(previewCallback);
            } catch (Throwable ignored) {
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getSmallestPreviewSize(width, height, parameters);
                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    Log.d(TAG, "Using width=" + size.width + " height=" + size.height);
                }
                if (enabled){
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
                camera.setParameters(parameters);
                camera.startPreview();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    private static Camera.Size getSmallestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea < resultArea) result = size;
                }
            }
        }

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.app_name) + " v." + getString(R.string.app_version) + "\nAuthors: " + getString(R.string.app_authors)).setTitle("About");
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
