package com.originos.wiggle;

import android.content.SharedPreferences;
import android.graphics.*;
import android.hardware.*;
import android.net.Uri;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.service.wallpaper.WallpaperService;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WiggleWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new WiggleEngine();
    }

    private class WiggleEngine extends Engine implements SensorEventListener {
        private final Handler handler = new Handler();
        private Bitmap wallpaperBitmap;
        private SensorManager sensorManager;
        private Sensor accelerometer;
        private float tiltX = 0f, tiltY = 0f;
        private float touchX = -100f, touchY = -100f, touchRadius = 0f;
        private boolean isRunning = true;

        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                drawFrame();
                if (isRunning) {
                    handler.postDelayed(this, 16); // ~60 FPS
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accelerometer != null) {
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                }
            }
            loadUserWallpaper();
            handler.post(drawRunner);
        }

        private void loadUserWallpaper() {
            SharedPreferences prefs = getSharedPreferences("WallpaperPrefs", MODE_PRIVATE);
            String uriStr = prefs.getString("image_uri", null);
            if (uriStr != null) {
                try {
                    Uri uri = Uri.parse(uriStr);
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    wallpaperBitmap = BitmapFactory.decodeStream(inputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            isRunning = false;
            handler.removeCallbacks(drawRunner);
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                touchX = event.getX();
                touchY = event.getY();
                touchRadius = 60f; // Squish effect radius
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                touchX = -100f;
                touchY = -100f;
                touchRadius = 0f;
            }
            super.onTouchEvent(event);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // Map tilt and shake to offset values
                tiltX = -event.values[0] * 4.0f;
                tiltY = event.values[1] * 4.0f;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    int width = canvas.getWidth();
                    int height = canvas.getHeight();

                    // Background Fill
                    canvas.drawColor(Color.BLACK);

                    // Draw Image with Tilt/Wiggle offset
                    if (wallpaperBitmap != null) {
                        Rect destRect = new Rect(
                            (int)(tiltX - 20), 
                            (int)(tiltY - 20), 
                            width + (int)(tiltX + 20), 
                            height + (int)(tiltY + 20)
                        );
                        canvas.drawBitmap(wallpaperBitmap, null, destRect, null);
                    }

                    // Render Custom Drawn Interactive Circle/Cycles (as shown in reference image)
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.argb(160, 255, 60, 100));
                    
                    float centerX = width / 2f + tiltX * 2;
                    float centerY = height / 2f + tiltY * 2;
                    float radius = 180f + (touchRadius > 0 ? 30f : 0f); // Spring bounce on touch
                    
                    canvas.drawCircle(centerX, centerY, radius, paint);

                    // Depth Effect Time Clock Overlay
                    Paint timePaint = new Paint();
                    timePaint.setAntiAlias(true);
                    timePaint.setColor(Color.WHITE);
                    timePaint.setTextSize(180f);
                    timePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("HH\nmm", Locale.getDefault());
                    String timeText = sdf.format(new Date());

                    // Draw split layered depth effect clock
                    canvas.drawText(timeText, 100f, 350f, timePaint);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}