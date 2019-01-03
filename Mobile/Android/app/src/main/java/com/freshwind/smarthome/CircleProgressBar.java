package com.freshwind.smarthome;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class CircleProgressBar extends View
{
    /**
     * ProgressBar's line thickness
     */
    private float strokeWidth = 4;
    private float progress = 0;
    private int min = 0;
    private int max = 100;
    /**
     * Start the progress at 12 o'clock
     */
    private int startAngle = -90;
    private int foregroundColor = Color.DKGRAY;
    private int backgroundColor = Color.GRAY;
    private int innerColor = Color.LTGRAY;
    private RectF rectF;
    private Paint paint;


    public CircleProgressBar(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs)
    {
        rectF = new RectF();
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CircleProgressBar,
                0, 0);
        //Reading values from the XML layout
        try {
            strokeWidth = typedArray.getDimension(R.styleable.CircleProgressBar_progressBarThickness, strokeWidth);
            progress = typedArray.getFloat(R.styleable.CircleProgressBar_progress, progress);
            foregroundColor = typedArray.getInt(R.styleable.CircleProgressBar_frontColor, foregroundColor);
            backgroundColor = typedArray.getInt(R.styleable.CircleProgressBar_backColor, backgroundColor);
            innerColor = typedArray.getInt(R.styleable.CircleProgressBar_innerColor, innerColor);
            min = typedArray.getInt(R.styleable.CircleProgressBar_min, min);
            max = typedArray.getInt(R.styleable.CircleProgressBar_max, max);
        } finally {
            typedArray.recycle();
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(strokeWidth);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        setMeasuredDimension(width, height);
        rectF.set(0 + strokeWidth / 2, 0 + strokeWidth / 2, width - strokeWidth / 2, height - strokeWidth / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(innerColor);
        canvas.drawOval(rectF, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(backgroundColor);
        canvas.drawOval(rectF, paint);

        paint.setColor(foregroundColor);
        float angle = 360 * progress / max;
        canvas.drawArc(rectF, startAngle, angle, false, paint);


        float dx = (float) Math.cos(Math.toRadians(angle - 90)) * (rectF.right + strokeWidth / 2) / 2;
        float dy = (float) Math.sin(Math.toRadians(angle - 90)) * (rectF.bottom + strokeWidth / 2) / 2;
        canvas.drawLine(rectF.centerX() + dx/2, rectF.centerY() + dy/2, rectF.centerX() + dx, rectF.centerY() + dy, paint);
    }
}
