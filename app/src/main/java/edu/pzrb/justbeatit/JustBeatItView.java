package edu.pzrb.justbeatit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class JustBeatItView extends View {

    private static final Paint BLACK_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint GREEN_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        GREEN_PAINT.setColor(Color.GREEN);
    }


    private static final int TIME_TO_REDRAW = 25;

    private int parentWidth = 0;
    private int parentHeight = 0;

    private AtomicBoolean beat = new AtomicBoolean(false);

    private AtomicBoolean enabled = new AtomicBoolean(true);
    private long lastTime = System.currentTimeMillis();
    private Point currentPoint = null;

    private List<Float> drawPoints = Collections.synchronizedList(new ArrayList<Float>());

    private RefreshHandler mRedrawHandler = new RefreshHandler();


    private SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    private  final String prefTimeScaleKey=getContext().getString(R.string.preference_graph_time_key);
    private  final String prefTimeScaleDefault=getContext().getString(R.string.preference_graph_time_default);

    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            JustBeatItView.this.update();
            JustBeatItView.this.invalidate();
        }

        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    }


    public JustBeatItView(Context context, AttributeSet attr) {
        super(context, attr);
    }

    public JustBeatItView(Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(parentWidth, parentHeight);
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        if (this.enabled.get()) {
            beat.set(false);
            drawPoints.clear();
            currentPoint = new Point(0, parentHeight / 2);

            lastTime = System.currentTimeMillis();
            mRedrawHandler.sleep(TIME_TO_REDRAW);
        }
    }

    public void update() {
        if (!enabled.get()) {
            return;
        }

        final long now = System.currentTimeMillis();

        if (currentPoint == null) {
            currentPoint = new Point(0, parentHeight / 2);
        }

        int timeScale=Integer.parseInt(preferences.getString(prefTimeScaleKey,prefTimeScaleDefault));
        double partial = ((double) parentWidth) / timeScale;
        int speed = (int) (partial * (now - lastTime));


        if (beat.get()) {
            float beatLow = parentHeight * 0.8F;
            float beatHigh = parentHeight - beatLow;
            drawPoints.add((float) currentPoint.x);
            drawPoints.add((float) currentPoint.y);
            drawPoints.add((float) (currentPoint.x + speed / 2));
            drawPoints.add(beatHigh);

            drawPoints.add((float) (currentPoint.x + speed / 2));
            drawPoints.add(beatHigh);
            drawPoints.add((float) (currentPoint.x + speed / 2));
            drawPoints.add(beatLow);

            drawPoints.add((float) (currentPoint.x + speed / 2));
            drawPoints.add(beatLow);
            drawPoints.add((float) (currentPoint.x + speed));
            drawPoints.add((float) currentPoint.y);

            beat.set(false);
        } else {
            drawPoints.add((float) currentPoint.x);
            drawPoints.add((float) currentPoint.y);
            drawPoints.add((float) (currentPoint.x + speed));
            drawPoints.add((float) currentPoint.y);
        }

        if (currentPoint.x + speed >= parentWidth) {
            drawPoints.clear();
        }

        currentPoint.x = (currentPoint.x + speed) % parentWidth;

        mRedrawHandler.sleep(TIME_TO_REDRAW);
        lastTime = now;
    }

    public void beat() {
        beat.set(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas == null) {
            throw new NullPointerException();
        }

        canvas.drawRect(0, 0, parentWidth, parentHeight, BLACK_PAINT);

        if (currentPoint == null) {
            update();
            return;
        }

        float[] array = new float[drawPoints.size()];
        for (int i = 0; i < drawPoints.size(); i++) {
            array[i] = drawPoints.get(i);
        }
        canvas.drawLines(array, GREEN_PAINT);
        canvas.drawPoint(currentPoint.x, currentPoint.y, GREEN_PAINT);
    }
}
