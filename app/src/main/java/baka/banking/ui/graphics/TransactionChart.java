package baka.banking.ui.graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TransactionChart extends Drawable {

    private static final float C_ANGLE = 360f;
    private static final float GAP_ANGLE = 2f;

    private final double[] slicePercents;
    private final Paint chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public enum Colors {

        PURPLE(0xFFAA00ff),
        PINK(0xFFD500F9),
        RED(0xFFF06292),
        BLUE(0xFF00B0FF),
        LAVENDER(0xFFBB86FC),
        CYAN(0xFF6200EE);

        private final int color;

        Colors(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    public TransactionChart(double[] slicePercents) {
        this.slicePercents = slicePercents;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        chartPaint.setColor(Color.WHITE);
        RectF bounds = new RectF(getBounds());
        float radius = (float) Math.min(getBounds().width(), getBounds().height()) / 6;
        float endAngle, startAngle = 0f;
        for (int i = 0; i < slicePercents.length; ++i) {
            slicePaint.setColor(Colors.values()[i].getColor());
            endAngle = (float) (C_ANGLE / 100 * slicePercents[i]);
            canvas.drawArc(bounds, startAngle, endAngle - GAP_ANGLE, true, slicePaint);
            canvas.rotate(endAngle - GAP_ANGLE, bounds.centerX(), bounds.centerY());
            canvas.drawArc(bounds, startAngle, GAP_ANGLE, true, chartPaint);
            canvas.rotate(GAP_ANGLE, bounds.centerX(), bounds.centerY());
        }
        canvas.drawCircle((float) getBounds().width()/2, (float) getBounds().height()/2, radius, chartPaint);
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
