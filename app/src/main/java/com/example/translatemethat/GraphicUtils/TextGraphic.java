package com.example.translatemethat.GraphicUtils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import com.example.translatemethat.GraphicUtils.GraphicOverlay.Graphic;
import com.google.mlkit.vision.text.Text;

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class TextGraphic extends Graphic {

    private static final String TAG = "TextGraphic";
    private static final int TEXT_COLOR = Color.RED;
    private static final float TEXT_SIZE = 54.0f;
    private static final float STROKE_WIDTH = 4.0f;

    private final Paint rectPaint;
    private final Paint textPaint;
    private final Text.Element element;

    public TextGraphic(GraphicOverlay overlay, Text.Element element) {
        super(overlay);

        this.element = element;

        rectPaint = new Paint();
        rectPaint.setColor(TEXT_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        if (element == null) {
            throw new IllegalStateException("Attempting to draw a null text.");
        }

        // Draws the bounding box around the TextBlock.
        RectF rect = new RectF(element.getBoundingBox());
        canvas.drawRect(rect, rectPaint);

        // Renders the text at the bottom of the box.
        canvas.drawText(element.getText(), rect.left, rect.bottom, textPaint);
    }
}