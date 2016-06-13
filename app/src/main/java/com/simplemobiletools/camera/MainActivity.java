package com.simplemobiletools.camera;

import android.Manifest;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.simplemobiletools.camera.Preview.PreviewListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements SensorEventListener, PreviewListener {
    @BindView(R.id.viewHolder) RelativeLayout viewHolder;
    @BindView(R.id.toggle_camera) ImageView toggleCameraBtn;
    @BindView(R.id.toggle_flash) ImageView toggleFlashBtn;
    @BindView(R.id.toggle_videocam) ImageView togglePhotoVideoBtn;
    @BindView(R.id.shutter) ImageView shutterBtn;
    @BindView(R.id.video_rec_curr_timer) TextView recCurrTimer;

    public static int orientation;
    private static final int CAMERA_STORAGE_PERMISSION = 1;
    private static SensorManager sensorManager;
    private Preview preview;
    private int currCamera;
    private boolean isFlashEnabled;
    private boolean isPhoto;
    private int currVideoRecTimer;
    private Handler timerHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        tryInitCamera();
    }

    private void tryInitCamera() {
        if (hasCameraAndStoragePermission()) {
            initializeCamera();
        } else {
            final List<String> permissions = new ArrayList<>(2);
            if (!Utils.hasCameraPermission(getApplicationContext())) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (!Utils.hasStoragePermission(getApplicationContext())) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), CAMERA_STORAGE_PERMISSION);
        }
    }

    private void initializeCamera() {
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        currCamera = Camera.CameraInfo.CAMERA_FACING_BACK;
        preview = new Preview(this, (SurfaceView) findViewById(R.id.surfaceView), this);
        preview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        viewHolder.addView(preview);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        isPhoto = true;
        timerHandler = new Handler();
    }

    private boolean hasCameraAndStoragePermission() {
        return Utils.hasCameraPermission(getApplicationContext()) && Utils.hasStoragePermission(getApplicationContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_STORAGE_PERMISSION) {
            if (hasCameraAndStoragePermission()) {
                initializeCamera();
            } else {
                Utils.showToast(getApplicationContext(), R.string.no_permissions);
                finish();
            }
        }
    }

    @OnClick(R.id.toggle_camera)
    public void toggleCamera() {
        disableFlash();
        hideTimer();
        if (currCamera == Camera.CameraInfo.CAMERA_FACING_BACK) {
            currCamera = Camera.CameraInfo.CAMERA_FACING_FRONT;
            toggleCameraBtn.setImageResource(R.mipmap.camera_rear);
        } else {
            currCamera = Camera.CameraInfo.CAMERA_FACING_BACK;
            toggleCameraBtn.setImageResource(R.mipmap.camera_front);
        }

        disableFlash();
        preview.releaseCamera();
        preview.setCamera(currCamera);
    }

    @OnClick(R.id.toggle_flash)
    public void toggleFlash() {
        if (isFlashEnabled) {
            disableFlash();
        } else {
            enableFlash();
        }
    }

    private void disableFlash() {
        preview.disableFlash();
        isFlashEnabled = false;
        toggleFlashBtn.setImageResource(R.mipmap.flash_off);
    }

    private void enableFlash() {
        preview.enableFlash();
        isFlashEnabled = true;
        toggleFlashBtn.setImageResource(R.mipmap.flash_on);
    }

    @OnClick(R.id.shutter)
    public void shutterPressed() {
        if (isPhoto) {
            preview.takePicture();
        } else {
            final Resources res = getResources();
            final boolean isRecording = preview.toggleRecording();
            if (isRecording) {
                shutterBtn.setImageDrawable(res.getDrawable(R.mipmap.video_stop));
                toggleCameraBtn.setVisibility(View.INVISIBLE);
                showTimer();
            } else {
                shutterBtn.setImageDrawable(res.getDrawable(R.mipmap.video_rec));
                toggleCameraBtn.setVisibility(View.VISIBLE);
                hideTimer();
            }
        }
    }

    @OnClick(R.id.about)
    public void launchAbout() {
        final Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.toggle_videocam)
    public void toggleVideo() {
        disableFlash();
        hideTimer();
        isPhoto = !isPhoto;
        toggleCameraBtn.setVisibility(View.VISIBLE);

        if (isPhoto) {
            initPhoto();
        } else {
            initVideo();
        }
    }

    private void initPhoto() {
        final Resources res = getResources();
        togglePhotoVideoBtn.setImageDrawable(res.getDrawable(R.mipmap.videocam));
        shutterBtn.setImageDrawable(res.getDrawable(R.mipmap.camera));
        preview.initPhotoMode();
    }

    private void initVideo() {
        final Resources res = getResources();
        togglePhotoVideoBtn.setImageDrawable(res.getDrawable(R.mipmap.photo));
        shutterBtn.setImageDrawable(res.getDrawable(R.mipmap.video_rec));
        preview.initRecorder();
        toggleCameraBtn.setVisibility(View.VISIBLE);
    }

    private void hideNavigationBarIcons() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    private void hideTimer() {
        recCurrTimer.setText(Utils.formatSeconds(0));
        recCurrTimer.setVisibility(View.GONE);
        currVideoRecTimer = 0;
        timerHandler.removeCallbacksAndMessages(null);
    }

    private void showTimer() {
        recCurrTimer.setVisibility(View.VISIBLE);
        setupTimer();
    }

    private void setupTimer() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recCurrTimer.setText(Utils.formatSeconds(currVideoRecTimer++));
                timerHandler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasCameraAndStoragePermission()) {
            resumeCameraItems();
        }
    }

    private void resumeCameraItems() {
        final int cnt = Camera.getNumberOfCameras();
        if (cnt == 1) {
            toggleCameraBtn.setVisibility(View.INVISIBLE);
        }

        preview.setCamera(currCamera);
        hideNavigationBarIcons();

        if (sensorManager != null) {
            final Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        if (!isPhoto) {
            preview.initRecorder();
            initVideo();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!hasCameraAndStoragePermission())
            return;

        hideTimer();
        if (preview != null) {
            preview.releaseCamera();
        }

        if (sensorManager != null)
            sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] < 6.5 && event.values[0] > -6.5) {
            orientation = Constants.ORIENT_PORTRAIT;
        } else {
            if (event.values[0] > 0) {
                orientation = Constants.ORIENT_LANDSCAPE_LEFT;
            } else {
                orientation = Constants.ORIENT_LANDSCAPE_RIGHT;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void setFlashAvailable(boolean available) {
        if (available) {
            toggleFlashBtn.setVisibility(View.VISIBLE);
        } else {
            toggleFlashBtn.setVisibility(View.INVISIBLE);
            disableFlash();
        }
    }
}
