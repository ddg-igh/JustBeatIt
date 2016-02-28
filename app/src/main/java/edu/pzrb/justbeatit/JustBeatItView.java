package edu.pzrb.justbeatit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class JustBeatItView extends View {

    private static final Matrix matrix = new Matrix();
    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static Bitmap pausedBitmap = null;
    private static Bitmap emptyBitmap = null;
    private static Bitmap heartBitmap = null;

    private static int parentWidth = 0;
    private static int parentHeight = 0;

    public JustBeatItView(Context context, AttributeSet attr) {
        super(context, attr);
        pausedBitmap = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_pause);
        emptyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.empty_icon);
        heartBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.heart_icon);


    }

    public JustBeatItView(Context context) {
        super(context);
        pausedBitmap = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_pause);

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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas == null) throw new NullPointerException();

        Bitmap bitmap = null;
        if (JustBeatItActivity.getCurrent() == JustBeatItActivity.State.NO_BEAT){
            bitmap = emptyBitmap;
        }else if (JustBeatItActivity.getCurrent() == JustBeatItActivity.State.BEAT){
            bitmap = heartBitmap;
        }else{
            bitmap = pausedBitmap;
        }

        int bitmapX = bitmap.getWidth() / 2;
        int bitmapY = bitmap.getHeight() / 2;

        int parentX = parentWidth / 2;
        int parentY = parentHeight / 2;

        int centerX = parentX - bitmapX;
        int centerY = parentY - bitmapY;

        matrix.reset();
        matrix.postTranslate(centerX, centerY);
        canvas.drawBitmap(bitmap, matrix, paint);
        invalidate();
    }
}
