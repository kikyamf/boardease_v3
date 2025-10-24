package com.example.mock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CircularImageView extends ImageView {

    private Path path;

    public CircularImageView(Context context) {
        super(context);
        init();
    }

    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float radius = Math.min(getWidth(), getHeight()) / 2.0f;
        path.reset();
        path.addCircle(getWidth() / 2.0f, getHeight() / 2.0f, radius, Path.Direction.CW);
        canvas.clipPath(path);
        super.onDraw(canvas);
    }
}































