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
import android.widget.NumberPicker;

public class CircleProgressBar extends View
{
    /**
     * ProgressBar's line thickness
     */
    private float strokeWidth = 4;
    private int progress = 0;
    private int min = 0;
    private int max = 100;
    /**
     * Start the progress at 12 o'clock
     */
    private int startAngle = -90;
    private int foregroundColor = Color.DKGRAY;
    private int backgroundColor = Color.GRAY;
    private int innerColor = Color.LTGRAY;
    private float mainTextSize = 60;
    private float topTextSize = 20;
    private float bottomTextSize = 20;
    private float topVerticalOffset = 1.5f;
    private float bottomVerticalOffset = 1.5f;
    private RectF rectF;
    private Paint paint;

    private NumberPicker.OnValueChangeListener valueChangeListener;

    public String mainText;
    public String topText;
    public String bottomText;

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
        try
        {
            strokeWidth = typedArray.getDimension(R.styleable.CircleProgressBar_progressBarThickness, strokeWidth);
            progress = typedArray.getInt(R.styleable.CircleProgressBar_progress, progress);
            mainTextSize = typedArray.getDimension(R.styleable.CircleProgressBar_mainTextSize, mainTextSize);
            topTextSize = typedArray.getDimension(R.styleable.CircleProgressBar_topTextSize, topTextSize);
            bottomTextSize = typedArray.getDimension(R.styleable.CircleProgressBar_bottomTextSize, bottomTextSize);
            foregroundColor = typedArray.getInt(R.styleable.CircleProgressBar_frontColor, foregroundColor);
            backgroundColor = typedArray.getInt(R.styleable.CircleProgressBar_backColor, backgroundColor);
            innerColor = typedArray.getInt(R.styleable.CircleProgressBar_innerColor, innerColor);
            min = typedArray.getInt(R.styleable.CircleProgressBar_min, min);
            max = typedArray.getInt(R.styleable.CircleProgressBar_max, max);
            mainText = typedArray.getString(R.styleable.CircleProgressBar_mainText);
            topText = typedArray.getString(R.styleable.CircleProgressBar_topText);
            bottomText = typedArray.getString(R.styleable.CircleProgressBar_bottomText);
            topVerticalOffset = typedArray.getFloat(R.styleable.CircleProgressBar_topOffset, topVerticalOffset);
            bottomVerticalOffset = typedArray.getFloat(R.styleable.CircleProgressBar_bottomOffset, bottomVerticalOffset);

        }
        finally
        {
            typedArray.recycle();
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setShadowLayer(50.0f, 5.0f, 5.0f, getResources().getColor(R.color.neutral_900));
//        setLayerType(LAYER_TYPE_SOFTWARE, paint);
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
        paint.setStrokeWidth(strokeWidth);

        float centerX = rectF.centerX();
        float centerY = rectF.centerY();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(innerColor);
        canvas.drawOval(rectF, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(backgroundColor);
        canvas.drawOval(rectF, paint);

        paint.setColor(foregroundColor);
        float angle = 360 * progress / max;
        canvas.drawArc(rectF, startAngle, angle, false, paint);


        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStyle(Paint.Style.FILL);

        if (mainText != null)
        {
            paint.setTextSize(mainTextSize);
            canvas.drawText(mainText, centerX, centerY + mainTextSize / 3, paint);
        }

        paint.setColor(backgroundColor);
        paint.setTextSize(topTextSize);
        if (topText != null)
        {
            paint.setTextSize(topTextSize);
            canvas.drawText(topText, centerX, centerY * (topVerticalOffset - 1), paint);
        }

        if (bottomText != null)
        {
            paint.setTextSize(bottomTextSize);
            canvas.drawText(bottomText, centerX, centerY * bottomVerticalOffset, paint);
        }

        //canvas.drawText(String.valueOf(rectF.height()), centerX, centerY * (topVerticalOffset - 1), paint);

        paint.setStrokeWidth(strokeWidth / 2);
        paint.setColor(foregroundColor);

        // Рисуем верхнюю вертикальную пипку
        canvas.drawLine(rectF.centerX(),rectF.centerY() + calculateY((float)Math.PI) * 0.8f,
                rectF.centerX(), rectF.top - strokeWidth / 2, paint);

        // Рисуем наклонную пипку
        float dx = calculateX(angle);
        float dy = calculateY(angle);
        canvas.drawLine(rectF.centerX() + dx * 0.8f, rectF.centerY() + dy * 0.8f, rectF.centerX() + dx, rectF.centerY() + dy, paint);
    }

    private float calculateX(float angle)
    {
        return (float) Math.cos(Math.toRadians(angle - 90)) * (rectF.right + strokeWidth / 2) / 2;
    }

    private float calculateY(float angle)
    {
        return (float) Math.sin(Math.toRadians(angle - 90)) * (rectF.bottom + strokeWidth / 2) / 2;
    }

    public void setProgress(int progress)
    {
        valueChangeListener.onValueChange(null, this.progress, progress);
        this.progress = progress;
        invalidate();
    }

    public void setOnValueChangeListener(NumberPicker.OnValueChangeListener listener)
    {
        valueChangeListener = listener;
    }
}
