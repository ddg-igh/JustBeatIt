package edu.pzrb.justbeatit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

public class JustBeatItActivity extends AppCompatActivity {

    private static final String TAG = "JustBeatIt";
    private static final AtomicBoolean processing = new AtomicBoolean(false);

    private static SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;

    private static Camera camera = null;
    private static JustBeatItView graph = null;

    private static TextView status = null;

    private static TextView debugLabel = null;

    private static PowerManager.WakeLock wakeLock = null;

    private static int averageIndex = 0;
    private static final int averageArraySize = 4;
    private static final int[] averageArray = new int[averageArraySize];

    public static enum State {
        NO_BEAT, BEAT, PAUSED
    };

    private static State currentType = State.NO_BEAT;

    private static int beatsIndex = 0;
    private static final int beatsArraySize = 3;
    private static final int[] beatsArray = new int[beatsArraySize];
    private static double beats = 0;
    private static long startTime = 0;

    private static boolean enabled = true;
    private static int treshold = 200;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    //preview.setVisibility(View.GONE);

                    fab.setImageResource(android.R.drawable.ic_media_play);

                    graph.setEnabled(false);
                } else {
                    Snackbar.make(view, getString(R.string.scan_resume), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    enabled = true;
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    currentType = State.NO_BEAT;

                    //preview.setVisibility(View.VISIBLE);
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

        graph =(JustBeatItView) findViewById(R.id.image);
        status = (TextView) findViewById(R.id.text);

        debugLabel = (TextView) findViewById(R.id.debugText);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        wakeLock.acquire();
        camera = Camera.open();
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
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    private static Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null){
                throw new NullPointerException();
            }
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null){
                throw new NullPointerException();
            }
            if(currentType == State.PAUSED){
                return;
            }
            if (!processing.compareAndSet(false, true)){
                return;
            }

            int imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(),size.width, size.height);
            debugLabel.setText("ImgAvg: " + imgAvg);
            if (imgAvg == 0 || imgAvg == 255) {
                processing.set(false);
                return;
            }else if(imgAvg > treshold) {
                if(status.getText().equals(R.string.action_finger)) status.setText("--");

                int averageArrayAvg = 0;
                int averageArrayCnt = 0;
                for (int i = 0; i < averageArray.length; i++) {
                    if (averageArray[i] > 0) {
                        averageArrayAvg += averageArray[i];
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

                if (averageIndex == averageArraySize){
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

                    if (beatsIndex == beatsArraySize){
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
                    if (enabled){
                        status.setText(String.valueOf(beatsAvg));
                    }
                    startTime = System.currentTimeMillis();
                    beats = 0;
                }
            }else{
                // Finger is not placed on camera
                // Reset the previously captured data
                status.setText(R.string.action_finger);
                for(int i = 0; i < averageArraySize; i++){
                    averageArray[i] = 0;
                }
                for(int i = 0; i < beatsArraySize; i++){
                    beatsArray[i] = 0;
                }
            }
            processing.set(false);
        }
    };

    private static SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(previewHolder);
                camera.setPreviewCallback(previewCallback);
            } catch (Throwable t) {
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();
            if(enabled) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                Camera.Size size = getSmallestPreviewSize(width, height, parameters);
                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    Log.d(TAG, "Using width=" + size.width + " height=" + size.height);
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
        }else if(id == R.id.action_settings){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Recognition treshold").setTitle("Settings");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            // Add slider for the treshold
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        return super.onOptionsItemSelected(item);
    }
}
