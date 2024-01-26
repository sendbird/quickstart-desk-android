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

public class TopCornerRoundedImageView extends AppCompatImageView {

    private Paint mBackgroundPaint;
    private int mCornerRadius;

    private final RectF mRect = new RectF();

    public TopCornerRoundedImageView(Context context) {
        super(context);
    }

    public TopCornerRoundedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TopCornerRoundedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
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
        Path clipPath = getPath(mCornerRadius, true, true, false, false);
        Path backgroundPath = getPath(mCornerRadius, true, true, false, false);

        canvas.clipPath(clipPath);
        super.onDraw(canvas);
        canvas.drawPath(backgroundPath, mBackgroundPaint);
    }

    private Path getPath(float radius, boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft) {
        final Path path = new Path();
        final float[] radii = new float[8];

        if (topLeft) {
            radii[0] = radius;
            radii[1] = radius;
        }

        if (topRight) {
            radii[2] = radius;
            radii[3] = radius;
        }

        if (bottomRight) {
            radii[4] = radius;
            radii[5] = radius;
        }

        if (bottomLeft) {
            radii[6] = radius;
            radii[7] = radius;
        }

        path.addRoundRect(mRect, radii, Path.Direction.CW);
        return path;
    }
}
