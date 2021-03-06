/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.java;


import static com.google.mlkit.vision.demo.CameraSource.getF;
import static com.google.mlkit.vision.demo.CameraSource.getSensorX;
import static com.google.mlkit.vision.demo.CameraSource.getSensorY;
import static com.google.mlkit.vision.demo.java.facedetector.FaceGraphic.getDeltaX;
import static com.google.mlkit.vision.demo.java.facedetector.FaceGraphic.getDeltaY;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.vision.demo.CameraSource;
import com.google.mlkit.vision.demo.CameraSourcePreview;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.java.brightness.MyObserver;
import com.google.mlkit.vision.demo.java.facedetector.FaceDetectorProcessor;
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.demo.preference.SettingsActivity;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Live preview demo for ML Kit APIs. */
@KeepName
public final class LivePreviewActivity extends AppCompatActivity
        implements OnRequestPermissionsResultCallback,
        OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener,
        SensorEventListener {

  private SensorManager sensorManager;

  private Double p;
  private Double t;

  private static final String FACE_DETECTION = "Face Detection";

  private static final String TAG = "LivePreviewActivity";
  private static final int PERMISSION_REQUESTS = 1;


  //brightness
  Context context; // same for distance
  private TextView textView;
  MyObserver yourObserver;
  // brightness




  private CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  private String selectedModel = FACE_DETECTION;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");

    setContentView(R.layout.activity_vision_live_preview);


    preview = findViewById(R.id.preview_view);
    if (preview == null) {
      Log.d(TAG, "Preview is null");
    }
    graphicOverlay = findViewById(R.id.graphic_overlay);
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null");
    }

    ToggleButton facingSwitch = findViewById(R.id.facing_switch);
    facingSwitch.setOnCheckedChangeListener(this);


    //settings button
    ImageView settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
            v -> {
              Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
              intent.putExtra(
                      SettingsActivity.EXTRA_LAUNCH_SOURCE, SettingsActivity.LaunchSource.LIVE_PREVIEW);
              startActivity(intent);
            });
    // end settings

    if (allPermissionsGranted()) {
      createCameraSource(selectedModel);
    } else {
      getRuntimePermissions();
    }









    // Brightness setter
    context = this;
    textView = findViewById(R.id.Br);

    Handler handler = new Handler();

    yourObserver = new MyObserver(handler,this,textView);

    getContentResolver().
            registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                    true,
                    yourObserver);

    String brightnes = String.valueOf(Settings.System.getInt(context.getContentResolver(),
            Settings.System.SCREEN_BRIGHTNESS, -1));  // in the range [0, 255]
    textView.setText(String.format("Brightness: %s", brightnes));
    // brightness END


    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    if (accelerometer != null) {
      sensorManager.registerListener(this, accelerometer,
              SensorManager.SENSOR_DELAY_FASTEST,
              SensorManager.SENSOR_DELAY_FASTEST);
    }





  }

  @Override
  public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent.getItemAtPosition(pos).toString();
    Log.d(TAG, "Selected model: " + selectedModel);
    preview.stop();
    if (allPermissionsGranted()) {
      createCameraSource(selectedModel);
      startCameraSource();
    } else {
      getRuntimePermissions();
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // Do nothing.
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    Log.d(TAG, "Set facing");
    if (cameraSource != null) {
      if (isChecked) {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
      } else {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);

      }
    }
    preview.stop();
    startCameraSource();
  }

  private void createCameraSource(String model) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay);
    }

    try {
      switch (model) {
        case FACE_DETECTION:
          Log.i(TAG, "Using Face Detector Processor");
          FaceDetectorOptions faceDetectorOptions =
                  PreferenceUtils.getFaceDetectorOptionsForLivePreview(this);
          cameraSource.setMachineLearningFrameProcessor(
                  new FaceDetectorProcessor(this, faceDetectorOptions));
          break;
        default:
          Log.e(TAG, "Unknown model: " + model);
      }
    } catch (RuntimeException e) {
      Log.e(TAG, "Can not create image processor: " + model, e);
      Toast.makeText(
              getApplicationContext(),
              "Can not create image processor: " + e.getMessage(),
              Toast.LENGTH_LONG)
              .show();
    }
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private void startCameraSource() {
    if (cameraSource != null) {
      try {
        if (preview == null) {
          Log.d(TAG, "resume: Preview is null");
        }
        if (graphicOverlay == null) {
          Log.d(TAG, "resume: graphOverlay is null");
        }
        preview.start(cameraSource, graphicOverlay);


      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    createCameraSource(selectedModel);
    startCameraSource();
  }

  /** Stops the camera. */
  @Override
  protected void onPause() {
    super.onPause();
    preview.stop();
    sensorManager.unregisterListener(this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (cameraSource != null) {
      cameraSource.release();
    }
    getContentResolver().
            unregisterContentObserver(yourObserver);
    sensorManager.unregisterListener(this);
  }

  private String[] getRequiredPermissions() {
    try {
      PackageInfo info =
              this.getPackageManager()
                      .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
      String[] ps = info.requestedPermissions;
      if (ps != null && ps.length > 0) {
        return ps;
      } else {
        return new String[0];
      }
    } catch (Exception e) {
      return new String[0];
    }
  }

  private boolean allPermissionsGranted() {
    for (String permission : getRequiredPermissions()) {
      if (!isPermissionGranted(this, permission)) {
        return false;
      }
    }
    return true;
  }

  private void getRuntimePermissions() {
    List<String> allNeededPermissions = new ArrayList<>();
    for (String permission : getRequiredPermissions()) {
      if (!isPermissionGranted(this, permission)) {
        allNeededPermissions.add(permission);
      }
    }

    if (!allNeededPermissions.isEmpty()) {
      ActivityCompat.requestPermissions(
              this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
    }
  }

  @Override
  public void onRequestPermissionsResult(
          int requestCode, @NonNull String[] permissions, int[] grantResults) {
    Log.i(TAG, "Permission granted!");
    if (allPermissionsGranted()) {
      createCameraSource(selectedModel);
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  private static boolean isPermissionGranted(Context context, String permission) {
    if (ContextCompat.checkSelfPermission(context, permission)
            == PackageManager.PERMISSION_GRANTED) {
      Log.i(TAG, "Permission granted: " + permission);
      return true;
    }
    Log.i(TAG, "Permission NOT granted: " + permission);
    return false;
  }


  @Override
  public void onSensorChanged(SensorEvent event) {
    //Get Rotation Vector Sensor Values
    double[] g = convertFloatsToDoubles(event.values.clone());

    //Normalise
    double norm = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2] + g[3] * g[3]);
    g[0] /= norm;
    g[1] /= norm;
    g[2] /= norm;
    g[3] /= norm;

    //Set values to commonly known quaternion letter representatives
    double x = g[0];
    double y = g[1];
    double z = g[2];
    double w = g[3];

    //Calculate Pitch in degrees (-180 to 180)
    double sinP = 2.0 * (w * x + y * z);
    double cosP = 1.0 - 2.0 * (x * x + y * y);
    p = Math.atan2(sinP, cosP) * (180 / Math.PI);

    //Calculate Tilt in degrees (-90 to 90)
    double sinT = 2.0 * (w * y - z * x);
    if (Math.abs(sinT) >= 1)
      t = Math.copySign(Math.PI / 2, sinT) * (180 / Math.PI);
    else
      t = Math.asin(sinT) * (180 / Math.PI);


    DATA.pitchangle = p;
    DATA.tiltangle = t;
  }

  private double[] convertFloatsToDoubles(float[] input)
  {
    if (input == null)
      return null;

    double[] output = new double[input.length];

    for (int i = 0; i < input.length; i++)
      output[i] = input[i];

    return output;
  }


  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {
    //
  }

}
