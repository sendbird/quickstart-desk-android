package com.sendbird.desk.android.sample.utils.image;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.sendbird.desk.android.sample.R;

public class RoundedImageView extends AppCompatImageView {

    private final Path mClipPath = new Path();
    private final Path mBackgroundPath = new Path();
    private Paint mBackgroundPaint;
    private int mCornerRadius;

    private final RectF mRect = new RectF();

    public RoundedImageView(Context context) {
        super(context);
    }

    public RoundedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView, defStyleAttr, 0);

        mCornerRadius = a.getDimensionPixelSize(R.styleable.RoundedImageView_deskCornerRadius, 0);
        int borderWidth = a.getDimensionPixelSize(R.styleable.RoundedImageView_deskBorderWidth, 0);
        int borderColor = a.getColor(R.styleable.RoundedImageView_deskBorderColor, 0);

        a.recycle();

        // Below Jelly Bean, clipPath on canvas would not work because lack of hardware acceleration
        // support. Hence, we should explicitly say to use software acceleration.

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStrokeWidth(borderWidth);
        mBackgroundPaint.setColor(borderColor);
        mBackgroundPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mRect.set(0, 0, getWidth(), getHeight());
        mClipPath.addRoundRect(mRect, mCornerRadius, mCornerRadius, Path.Direction.CW);
        mBackgroundPath.addRoundRect(mRect, mCornerRadius, mCornerRadius, Path.Direction.CW);

        canvas.clipPath(mClipPath);
        super.onDraw(canvas);
        canvas.drawPath(mBackgroundPath, mBackgroundPaint);
    }
}
