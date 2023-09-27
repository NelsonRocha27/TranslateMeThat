package com.example.translatemethat.GraphicUtils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

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
    private Text.Element element = null;
    private Text.Line line = null;
    private String translated = null;

    public TextGraphic(GraphicOverlay overlay, Text.Element element, String translated) {
        super(overlay);

        this.element = element;
        this.translated = translated;

        rectPaint = new Paint();
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    public TextGraphic(GraphicOverlay overlay, Text.Line line) {
        super(overlay);

        this.line = line;

        rectPaint = new Paint();
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStyle(Paint.Style.FILL_AND_STROKE);
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
        if(element != null)
        {
            // Draws the bounding box around the TextBlock.
            RectF rect = new RectF(element.getBoundingBox());
            canvas.drawRect(rect, rectPaint);

            // Renders the text at the bottom of the box.
            String text =  translated;
            if (text == null) return;

            float textSize = textPaint.getTextSize();

            Rect r = new Rect();
            rect.round(r);
            textPaint.getTextBounds(text, 0, text.length(), r);

            while (r.width() > rect.right - rect.left)
            {
                textSize--;
                textPaint.setTextSize(textSize);
                textPaint.getTextBounds(text, 0, text.length(), r);
            }
            canvas.drawText(text, rect.left, rect.bottom, textPaint);
        }
        else if (line != null)
        {
            // Draws the bounding box around the TextBlock.
            RectF rect = new RectF(line.getBoundingBox());
            canvas.drawRect(rect, rectPaint);

            // Renders the text at the bottom of the box.

            String text = line.getText();
            float textSize = textPaint.getTextSize();

            Rect r = new Rect();
            rect.round(r);
            textPaint.getTextBounds(text, 0, text.length(), r);

            while (r.width() > rect.right - rect.left)
            {
                textSize--;
                textPaint.setTextSize(textSize);
                textPaint.getTextBounds(text, 0, text.length(), r);
            }

            //textPaint.setTextSize(rect.bottom - rect.top);
            canvas.drawText(line.getText(), rect.left, rect.bottom, textPaint);
        }
        else
        {
            throw new IllegalStateException("Attempting to draw a null text.");
        }
    }
}