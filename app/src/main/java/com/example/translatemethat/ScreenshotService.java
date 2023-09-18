/***
 Copyright (c) 2015 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 Covered in detail in the book _The Busy Coder's Guide to Android Development_
 https://commonsware.com/Android
 */

package com.example.translatemethat;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import com.example.translatemethat.BuildConfig;
import com.example.translatemethat.GraphicUtils.GraphicOverlay;
import com.example.translatemethat.GraphicUtils.TextGraphic;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

public class ScreenshotService extends Service implements View.OnClickListener{
  private static final String CHANNEL_WHATEVER="channel_whatever";
  private static final int NOTIFY_ID=9906;
  static final String EXTRA_RESULT_CODE="resultCode";
  static final String EXTRA_RESULT_INTENT="resultIntent";
  static final String ACTION_RECORD = BuildConfig.APPLICATION_ID+".RECORD";
  static final String ACTION_SHUTDOWN = BuildConfig.APPLICATION_ID+".SHUTDOWN";
  static final int VIRT_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY |
          DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
  private MediaProjection projection;
  private VirtualDisplay vdisplay;
  final private HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName(),
          android.os.Process.THREAD_PRIORITY_BACKGROUND);
  private Handler handler;
  private MediaProjectionManager mgr;
  private WindowManager wmgr;
  private ImageTransmogrifier it;
  private int resultCode;
  private Intent resultData;
  final private ToneGenerator beeper = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

  private WindowManager mWindowManager;
  private View mFloatingView;
  private View collapsedView;
  private View expandedView;
  private View translationView;
  public Bitmap bitm = null;

  private GraphicOverlay mGraphicOverlay;

  @Override
  public void onCreate() {
    super.onCreate();

    setFloatingView();

    mgr=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
    wmgr=(WindowManager)getSystemService(WINDOW_SERVICE);

    handlerThread.start();
    handler=new Handler(handlerThread.getLooper());
  }

  @Override
  public int onStartCommand(Intent i, int flags, int startId) {
    if (i.getAction()==null) {
      resultCode=i.getIntExtra(EXTRA_RESULT_CODE, 1337);
      if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        resultData=i.getParcelableExtra(EXTRA_RESULT_INTENT, Intent.class);
      } else {
        resultData=i.getParcelableExtra(EXTRA_RESULT_INTENT);
      }
      foregroundify();
    }
    else if (ACTION_SHUTDOWN.equals(i.getAction())) {
      beeper.startTone(ToneGenerator.TONE_PROP_NACK);
      stopForeground(true);
      stopSelf();
    }

    return(START_NOT_STICKY);
  }

  @Override
  public void onDestroy() {
    stopCapture();

    super.onDestroy();
    if (mFloatingView != null)
    {
      mWindowManager.removeView(mFloatingView);
      mWindowManager.removeView(translationView);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.layoutExpanded:
        //switching views
        collapsedView.setVisibility(View.VISIBLE);
        expandedView.setVisibility(View.GONE);
        break;

      case R.id.buttonClose:
        //closing the widget
        stopSelf();
        break;
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    throw new IllegalStateException("Binding not supported. Go away.");
  }

  WindowManager getWindowManager() {
    return(wmgr);
  }

  Handler getHandler() {
    return(handler);
  }

  void setFloatingView()
  {
    //getting the widget layout from xml using layout inflater
    mFloatingView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);
    translationView = LayoutInflater.from(this).inflate(R.layout.translation_layout, null);

    //setting the layout parameters
    final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);

    WindowManager.LayoutParams params2 = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);

    //getting windows services and adding the floating view to it
    mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    mWindowManager.addView(translationView, params2);
    mWindowManager.addView(mFloatingView, params);

    translationView.setVisibility(View.GONE); // children clickable

    mGraphicOverlay = translationView.findViewById(R.id.graphic_overlay);

    //getting the collapsed and expanded view from the floating view
    collapsedView = mFloatingView.findViewById(R.id.layoutCollapsed);
    expandedView = mFloatingView.findViewById(R.id.layoutExpanded);

    //adding click listener to close button and expanded view
    mFloatingView.findViewById(R.id.buttonClose).setOnClickListener(this);
    //mFloatingView.findViewById(R.id.collapsed_iv).setOnClickListener(this);
    expandedView.setOnClickListener(this);

    //adding an touchlistener to make drag movement of the floating widget
    mFloatingView.findViewById(R.id.relativeLayoutParent).setOnTouchListener(new View.OnTouchListener() {
      private int initialX;
      private int initialY;
      private float initialTouchX;
      private float initialTouchY;

      private static final int MAX_CLICK_DURATION = 125;
      private long startClickTime;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            startClickTime = Calendar.getInstance().getTimeInMillis();
            initialX = params.x;
            initialY = params.y;
            initialTouchX = event.getRawX();
            initialTouchY = event.getRawY();
            return true;

          case MotionEvent.ACTION_UP:
            long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
            if(clickDuration < MAX_CLICK_DURATION) {
              startCapture();
              collapsedView.setVisibility(View.GONE);
              expandedView.setVisibility(View.VISIBLE);
              while(bitm == null)
              {

              }
              startTranslate(bitm);
            }
            return true;

          case MotionEvent.ACTION_MOVE:
            //this code is helping the widget to move around the screen with fingers
            params.x = initialX + (int) (event.getRawX() - initialTouchX);
            params.y = initialY + (int) (event.getRawY() - initialTouchY);
            mWindowManager.updateViewLayout(mFloatingView, params);
            return true;
        }
        return false;
      }
    });
  }

  void processImage(final byte[] png) {
    new Thread() {
      @Override
      public void run() {
        File output=new File("/storage/emulated/0/Download/screenshot.png");

        try {
          FileOutputStream fos=new FileOutputStream(output);

          fos.write(png);
          fos.flush();
          fos.getFD().sync();
          fos.close();

          MediaScannerConnection.scanFile(ScreenshotService.this,
            new String[] {output.getAbsolutePath()},
            new String[] {"image/png"},
            null);
        }
        catch (Exception e) {
          Log.e(getClass().getSimpleName(), "Exception writing out screenshot", e);
        }
      }
    }.start();

    beeper.startTone(ToneGenerator.TONE_PROP_ACK);
    stopCapture();
  }

  private void stopCapture() {
    if (projection!=null) {
      projection.stop();
      vdisplay.release();
      projection=null;
    }
  }

  private void startCapture() {
    projection=mgr.getMediaProjection(resultCode, resultData);
    it=new ImageTransmogrifier(this);

    MediaProjection.Callback cb=new MediaProjection.Callback() {
      @Override
      public void onStop() {
        vdisplay.release();
      }
    };

    vdisplay=projection.createVirtualDisplay("andshooter",
      it.getWidth(), it.getHeight(),
      getResources().getDisplayMetrics().densityDpi,
      VIRT_DISPLAY_FLAGS, it.getSurface(), null, handler);
    projection.registerCallback(cb, handler);
  }

  private void foregroundify() {
    NotificationCompat.Builder b=
      new NotificationCompat.Builder(this, CHANNEL_WHATEVER);

    b.setContentTitle("TranslateMeThat")
      .setSmallIcon(R.mipmap.ic_launcher)
      .setTicker("TranslateMeThat");

    b.addAction(R.drawable.ic_launcher_background,
      "Shutdown",
      buildPendingIntent(ACTION_SHUTDOWN));

    startForeground(NOTIFY_ID, b.build());
  }

  private PendingIntent buildPendingIntent(String action) {
    Intent i=new Intent(this, getClass());

    i.setAction(action);

    return(PendingIntent.getService(this, 0, i, 0));
  }

  void startTranslate(Bitmap bitmap) {
    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    InputImage image = InputImage.fromBitmap(bitmap, 0);

    mGraphicOverlay.clear();

    if(image != null) {
      Task<Text> result =
              recognizer.process(image)
                      .addOnSuccessListener(visionText -> {
                        processTextRecognitionResult(visionText);
                      })
                      .addOnFailureListener(
                              e -> {
                                Toast.makeText(getApplicationContext(), "No image selected", Toast.LENGTH_SHORT).show();
                              });
    }
  }

  //perform operation on the full text recognized in the image.
  private void processTextRecognitionResult(Text texts) {

    String resultText = texts.getText();

    translationView.setVisibility(View.VISIBLE);

    //Toast.makeText(getApplicationContext(), resultText, Toast.LENGTH_LONG).show();

    List<Text.TextBlock> blocks = texts.getTextBlocks();
    if (blocks.size() == 0) {
      Toast.makeText(getApplicationContext(), "No text found", Toast.LENGTH_SHORT).show();
      return;
    }
    for (Text.TextBlock block : texts.getTextBlocks()) {
      for (Text.Line line : block.getLines()) {
        /*GraphicOverlay.Graphic textGraphic = new TextGraphic(mGraphicOverlay, line);
        mGraphicOverlay.add(textGraphic);*/
        for (Text.Element element : line.getElements()) {
          //Draws the bounding box around the element.
          GraphicOverlay.Graphic textGraphic = new TextGraphic(mGraphicOverlay, element);
          mGraphicOverlay.add(textGraphic);
        }
      }
    }

    /*for (Text.TextBlock block : result.getTextBlocks()) {
      String blockText = block.getText();
      Point[] blockCornerPoints = block.getCornerPoints();
      Rect blockFrame = block.getBoundingBox();
      for (Text.Line line : block.getLines()) {
        String lineText = line.getText();
        Point[] lineCornerPoints = line.getCornerPoints();
        Rect lineFrame = line.getBoundingBox();
        for (Text.Element element : line.getElements()) {
          String elementText = element.getText();
          Point[] elementCornerPoints = element.getCornerPoints();
          Rect elementFrame = element.getBoundingBox();
          for (Text.Symbol symbol : element.getSymbols()) {
            String symbolText = symbol.getText();
            Point[] symbolCornerPoints = symbol.getCornerPoints();
            Rect symbolFrame = symbol.getBoundingBox();
          }
        }
      }
    }*/
  }
}
