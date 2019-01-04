package com.freshwind.smarthome;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class FancyDialogBackground extends View
{
    private Paint paint;
    private RectF rectf;
    private Path path;

    private float strokeWidth = 4;
    private int backgroundColor = getResources().getColor(R.color.neutral_500);
    private int strokeColor = getResources().getColor(R.color.neutral_700);

    public FancyDialogBackground(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs)
    {
        rectf = new RectF();
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.FancyDialog,
                0, 0);

        try
        {
            strokeWidth = typedArray.getDimension(R.styleable.FancyDialog_strokeWidth, strokeWidth);
            strokeColor = typedArray.getInt(R.styleable.FancyDialog_strokeColor, strokeColor);
            backgroundColor = typedArray.getInt(R.styleable.FancyDialog_backgroundColor, backgroundColor);
        }
        finally
        {
            typedArray.recycle();
        }

        path = new Path();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        setMeasuredDimension(width, height);
        rectf.set(0 + strokeWidth / 2, 0 + strokeWidth / 2, width - strokeWidth / 2, height - strokeWidth / 2);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        paint.setColor(backgroundColor);
        paint.setStyle(Paint.Style.FILL);

        float delta = rectf.centerY() / 2;

        path.reset();
        path.moveTo(rectf.left, rectf.top + delta);
        path.lineTo(rectf.left + delta, rectf.top);
        path.lineTo(rectf.right - delta, rectf.top);
        path.lineTo(rectf.right, rectf.top + delta);
        path.lineTo(rectf.right, rectf.bottom - delta);
        path.lineTo(rectf.right - delta, rectf.bottom);
        path.lineTo(rectf.left + delta, rectf.bottom);
        path.lineTo(rectf.left, rectf.bottom - delta);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(strokeColor);
        paint.setStyle(Paint.Style.STROKE);

        path.reset();
        path.moveTo(rectf.left, rectf.top + delta);
        path.lineTo(rectf.left + delta, rectf.top);
        path.lineTo(rectf.right - delta, rectf.top);
        path.lineTo(rectf.right, rectf.top + delta);
        path.lineTo(rectf.right, rectf.bottom - delta);
        path.lineTo(rectf.right - delta, rectf.bottom);
        path.lineTo(rectf.left + delta, rectf.bottom);
        path.lineTo(rectf.left, rectf.bottom - delta);
        path.close();
        canvas.drawPath(path, paint);
    }
}
