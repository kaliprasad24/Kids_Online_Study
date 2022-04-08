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

package com.google.mlkit.vision.demo.java.facedetector;
import android.content.res.Configuration;
import static com.google.mlkit.vision.demo.CameraSource.getF;
import static com.google.mlkit.vision.demo.CameraSource.getSensorX;
import static com.google.mlkit.vision.demo.CameraSource.getSensorY;
import java.lang.Math;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.google.mlkit.vision.demo.GraphicOverlay;

import com.google.mlkit.vision.demo.GraphicOverlay.Graphic;
import com.google.mlkit.vision.demo.java.DATA;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType;

import java.util.ArrayList;
import java.util.Locale;


/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
public class FaceGraphic extends Graphic {


  // difference
  public float diffarray;

  //distance
  public static float deltaX;
  public static float deltaY;
  static final int AVERAGE_EYE_DISTANCE = 63; // in mm
  static final int IMAGE_WIDTH = 1024;
  static final int IMAGE_HEIGHT = 1024;
  //distance



  private static final float FACE_POSITION_RADIUS = 8.0f;
  private static final float ID_TEXT_SIZE = 30.0f;
  private static final float ID_Y_OFFSET = 40.0f;
  private static final float BOX_STROKE_WIDTH = 5.0f;
  private static final int NUM_COLORS = 10;
  private static final int[][] COLORS =
          new int[][] {
                  // {Text color, background color}
                  {Color.BLACK, Color.WHITE},
                  {Color.WHITE, Color.MAGENTA},
                  {Color.BLACK, Color.LTGRAY},
                  {Color.WHITE, Color.RED},
                  {Color.WHITE, Color.BLUE},
                  {Color.WHITE, Color.DKGRAY},
                  {Color.BLACK, Color.CYAN},
                  {Color.BLACK, Color.YELLOW},
                  {Color.WHITE, Color.BLACK},
                  {Color.BLACK, Color.GREEN}
          };

  private final Paint facePositionPaint;
  private final Paint faceP;
  private final Paint[] idPaints;
  private final Paint[] boxPaints;
  private final Paint[] labelPaints;

  private volatile Face face;

  FaceGraphic(GraphicOverlay overlay, Face face) {
    super(overlay);

    this.face = face;
    final int selectedColor = Color.WHITE;

    facePositionPaint = new Paint();
    facePositionPaint.setColor(selectedColor);

    faceP = new Paint();
    faceP.setColor(Color.GREEN);

    int numColors = COLORS.length;
    idPaints = new Paint[numColors];
    boxPaints = new Paint[numColors];
    labelPaints = new Paint[numColors];
    for (int i = 0; i < numColors; i++) {
      idPaints[i] = new Paint();
      idPaints[i].setColor(COLORS[i][0] /* text color */);
      idPaints[i].setTextSize(ID_TEXT_SIZE);

      boxPaints[i] = new Paint();
      boxPaints[i].setColor(COLORS[i][1] /* background color */);
      boxPaints[i].setStyle(Paint.Style.STROKE);
      boxPaints[i].setStrokeWidth(BOX_STROKE_WIDTH);

      labelPaints[i] = new Paint();
      labelPaints[i].setColor(COLORS[i][1] /* background color */);
      labelPaints[i].setStyle(Paint.Style.FILL);
    }
  }

  /** Draws the face annotations for position on the supplied canvas. */
  @Override
  public void draw(Canvas canvas) {
    Face face = this.face;
    if (face == null) {
      return;
    }



    // Draws a circle at the position of the detected face, with the face's track id below.
    float x = translateX(face.getBoundingBox().centerX());
    float y = translateY(face.getBoundingBox().centerY());
    canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);

    // Calculate positions.
    float left = x - scale(face.getBoundingBox().width() / 2.0f);
    float top = y - scale(face.getBoundingBox().height() / 2.0f);
    float right = x + scale(face.getBoundingBox().width() / 2.0f);
    float bottom = y + scale(face.getBoundingBox().height() / 2.0f);
    float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
    float yLabelOffset = (face.getTrackingId() == null) ? 0 : -lineHeight;

    // Decide color based on face ID
    int colorID = (face.getTrackingId() == null) ? 0 : Math.abs(face.getTrackingId() % NUM_COLORS);

    // Calculate width and height of label box
    float textWidth = idPaints[colorID].measureText("ID: " + face.getTrackingId());
    if (face.getSmilingProbability() != null) {
      yLabelOffset -= lineHeight;
      textWidth =
              Math.max(
                      textWidth,
                      idPaints[colorID].measureText(
                              String.format(Locale.US, "Happiness: %.2f", face.getSmilingProbability())));
    }
    if (face.getLeftEyeOpenProbability() != null) {
      yLabelOffset -= lineHeight;
      textWidth =
              Math.max(
                      textWidth,
                      idPaints[colorID].measureText(
                              String.format(
                                      Locale.US, "Left eye open: %.2f", face.getLeftEyeOpenProbability())));
    }
    if (face.getRightEyeOpenProbability() != null) {
      yLabelOffset -= lineHeight;
      textWidth =
              Math.max(
                      textWidth,
                      idPaints[colorID].measureText(
                              String.format(
                                      Locale.US, "Right eye open: %.2f", face.getRightEyeOpenProbability())));
    }

    yLabelOffset = yLabelOffset - 3 * lineHeight;
    textWidth =
            Math.max(
                    textWidth,
                    idPaints[colorID].measureText(
                            String.format(Locale.US, "EulerX: %.2f", face.getHeadEulerAngleX())));
    textWidth =
            Math.max(
                    textWidth,
                    idPaints[colorID].measureText(
                            String.format(Locale.US, "EulerY: %.2f", face.getHeadEulerAngleY())));
    textWidth =
            Math.max(
                    textWidth,
                    idPaints[colorID].measureText(
                            String.format(Locale.US, "EulerZ: %.2f", face.getHeadEulerAngleZ())));
    // Draw labels
    canvas.drawRect(
            left - BOX_STROKE_WIDTH,
            top + yLabelOffset,
            left + textWidth + (2 * BOX_STROKE_WIDTH),
            top,
            labelPaints[colorID]);
    yLabelOffset += ID_TEXT_SIZE;
    canvas.drawRect(left, top, right, bottom, boxPaints[colorID]);
    if (face.getTrackingId() != null) {
      canvas.drawText("ID: " + face.getTrackingId(), left, top + yLabelOffset, idPaints[colorID]);
      yLabelOffset += lineHeight;
    }

    // Draws all face contours.
//    0-35	Face oval
//    36-40	Left eyebrow (top)
//    41-45	Left eyebrow (bottom)
//    46-50	Right eyebrow (top)
//    51-55	Right eyebrow (bottom)
//    56-71	Left eye
//    72-87	Right eye
//    88-96	Upper lip (bottom)
//    97-105	Lower lip (top)
//    106-116	Upper lip (top)
//    117-125	Lower lip (bottom)
//    126, 127	Nose bridge
//    128-130	Nose bottom (note that the center point is at index 128)
//    131	Left cheek (center)
//    132	Right cheek (center)

    float num = -1;

    float difference= 0 ;

    ArrayList<PointF> ovel = new ArrayList<PointF>();

    for (FaceContour contour : face.getAllContours()) {

      for (PointF point : contour.getPoints()) {
        num+=1;
        if (num<35){
          ovel.add(point);
        }





        canvas.drawCircle(
                translateX(point.x), translateY(point.y), FACE_POSITION_RADIUS, faceP);
        canvas.drawText(String.valueOf(num),translateX(point.x),translateY(point.y),facePositionPaint);
      }
    }

    for(int i=0;i<ovel.size()-1;i++){
      difference += pdistance(ovel.get(i), ovel.get(i+1) );

    }







    // focus on screen
    // 0 no stress
    // 1 neutral
    // 2 high stress
    // 3 medium stress
    // 4 low stress
    String focus = null;
    if (face.getSmilingProbability() != null && face.getRightEyeOpenProbability()!=null && face.getLeftEyeOpenProbability()!=null) {

      float smile = face.getSmilingProbability();
      float lEye = face.getLeftEyeOpenProbability();
      float rEye = face.getRightEyeOpenProbability();
      focus = focus(smile,lEye,rEye);

    }





    // Draws smiling and left/right eye open probabilities.
    if (face.getSmilingProbability() != null) {
      canvas.drawText(
              "Smiling: " + String.format(Locale.US, "%.2f", face.getSmilingProbability()),
              left,
              top + yLabelOffset,
              idPaints[colorID]);
      yLabelOffset += lineHeight;
    }

    FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
    if (face.getLeftEyeOpenProbability() != null) {
      canvas.drawText(
              "Left eye open: " + String.format(Locale.US, "%.2f", face.getLeftEyeOpenProbability()),
              left,
              top + yLabelOffset,
              idPaints[colorID]);
      yLabelOffset += lineHeight;
    }
    if (leftEye != null) {
      float leftEyeLeft =
              translateX(leftEye.getPosition().x) - idPaints[colorID].measureText("Left Eye") / 2.0f;
      canvas.drawRect(
              leftEyeLeft - BOX_STROKE_WIDTH,
              translateY(leftEye.getPosition().y) + ID_Y_OFFSET - ID_TEXT_SIZE,
              leftEyeLeft + idPaints[colorID].measureText("Left Eye") + BOX_STROKE_WIDTH,
              translateY(leftEye.getPosition().y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
              labelPaints[colorID]);
      canvas.drawText(
              "Left Eye",
              leftEyeLeft,
              translateY(leftEye.getPosition().y) + ID_Y_OFFSET,
              idPaints[colorID]);
    }


    FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);


    //distance
    if (leftEye!=null && rightEye!=null) {
      float deltaX = Math.abs(leftEye.getPosition().x - rightEye.getPosition().x);
      float deltaY = Math.abs(leftEye.getPosition().y - rightEye.getPosition().y);
      setDeltaX(deltaX);
      setDeltaY(deltaY);
    }
    float F = getF();
    float sx = getSensorX();
    float sy = getSensorY();
    float deltax = getDeltaX();
    float deltay = getDeltaY();
    float distance;
    if (deltax >= deltay) {
      distance = F * (AVERAGE_EYE_DISTANCE / sx) * (IMAGE_WIDTH / deltax);
    } else {
      distance = F * (AVERAGE_EYE_DISTANCE / sy) * (IMAGE_HEIGHT / deltay);
    }

    // distance end








    if (face.getRightEyeOpenProbability() != null) {
      canvas.drawText(
              "Right eye open: " + String.format(Locale.US, "%.2f", face.getRightEyeOpenProbability()),
              left,
              top + yLabelOffset,
              idPaints[colorID]);
    }
    if (rightEye != null) {
      float rightEyeLeft =
              translateX(rightEye.getPosition().x) - idPaints[colorID].measureText("Right Eye") / 2.0f;
      canvas.drawRect(
              rightEyeLeft - BOX_STROKE_WIDTH,
              translateY(rightEye.getPosition().y) + ID_Y_OFFSET - ID_TEXT_SIZE,
              rightEyeLeft + idPaints[colorID].measureText("Right Eye") + BOX_STROKE_WIDTH,
              translateY(rightEye.getPosition().y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
              labelPaints[colorID]);
      canvas.drawText(
              "Right Eye",
              rightEyeLeft,
              translateY(rightEye.getPosition().y) + ID_Y_OFFSET,
              idPaints[colorID]);
      yLabelOffset += lineHeight;
    }






    assert leftEye != null;
    assert rightEye != null;








    int dis = (int) (distance/10);
    canvas.drawText(
            "Stress: " +focus, left, top + yLabelOffset, idPaints[colorID]);
    yLabelOffset += lineHeight;
    canvas.drawText(
            "Distance :" + dis+"cm", left, top + yLabelOffset, idPaints[colorID]);
    yLabelOffset += lineHeight;
    canvas.drawText(
            "circum :" + difference, left, top + yLabelOffset, idPaints[colorID]);
    yLabelOffset += lineHeight;
    /*
    canvas.drawText(
        "EulerY: " + face.getHeadEulerAngleY(), left, top + yLabelOffset, idPaints[colorID]);
    yLabelOffset += lineHeight;
    canvas.drawText(
        "distance: " +dis+"cm", left, top + yLabelOffset, idPaints[colorID]);



     */


    // Draw facial landmarks
    drawFaceLandmark(canvas, FaceLandmark.LEFT_EYE);
    drawFaceLandmark(canvas, FaceLandmark.RIGHT_EYE);
    drawFaceLandmark(canvas, FaceLandmark.LEFT_CHEEK);
    drawFaceLandmark(canvas, FaceLandmark.RIGHT_CHEEK);

    // angle pose
    double rotX = face.getHeadEulerAngleX();
    double pitch = DATA.pitchangle;
    double tilt = DATA.tiltangle;


    int orientation = getApplicationContext().getResources().getConfiguration().orientation;
    if(orientation == Configuration.ORIENTATION_PORTRAIT){
      double mobiletilt;
      if((pitch>=0 && pitch<90)||(pitch>=90 && pitch<180)){
        mobiletilt = pitch-90.0;
      } else{
        if((pitch>=-180 && pitch<-90)){
          mobiletilt = 180.0+pitch;
        }else{
          mobiletilt = -1*(90.0+pitch);
        }
      }

      double neckangle;
      if(mobiletilt<=rotX+2.0 && mobiletilt>=rotX-2.0)
        neckangle = mobiletilt;
      else
        neckangle = mobiletilt + rotX;


      String posture = "";

      if(Math.abs(neckangle)>=0 && Math.abs(neckangle)<=30)
        posture = "Good";
      else if(Math.abs(neckangle)>30 && Math.abs(neckangle)<=60)
        posture = "Bad";
      else
        posture = "Worst";


      canvas.drawText(
              "Posture: " + posture, left, top + yLabelOffset, idPaints[colorID]);
      yLabelOffset += lineHeight;


    } else if(orientation == Configuration.ORIENTATION_LANDSCAPE){


      double mobiletilt;
      if (tilt<=0 && tilt>=-90.0) {
        if (Math.abs(pitch)>=0 && Math.abs(pitch)<=90) {

          mobiletilt = -1.0 * (90.0 + tilt);
        } else {
          mobiletilt = 90.0 + tilt;
        }
      } else {
        {
          if (Math.abs(pitch)>=0 && Math.abs(pitch)<=90)
          {
            mobiletilt = -90.0 + tilt;
          } else {
            mobiletilt = (-1.0 *( (-90.0) + (tilt)));
          }
        }

      }



      double neckangle;
      if(mobiletilt<=rotX+2.0 && mobiletilt>=rotX-2.0)
        neckangle = mobiletilt;
      else
        neckangle = mobiletilt + rotX;


      String posture = "";

      if(Math.abs(neckangle)>=0 && Math.abs(neckangle)<=30)
        posture = "Good";
      else if(Math.abs(neckangle)>30 && Math.abs(neckangle)<=60)
        posture = "Bad";
      else
        posture = "Worst";

      canvas.drawText(
              "Posture: " + posture, left, top + yLabelOffset, idPaints[colorID]);
      yLabelOffset += lineHeight;
    }
    // angle pose end


  }

  private void drawFaceLandmark(Canvas canvas, @LandmarkType int landmarkType) {
    FaceLandmark faceLandmark = face.getLandmark(landmarkType);
    if (faceLandmark != null) {
      canvas.drawCircle(
              translateX(faceLandmark.getPosition().x),
              translateY(faceLandmark.getPosition().y),
              FACE_POSITION_RADIUS,
              facePositionPaint);
    }
  }

  public static float getDeltaX() {
    return deltaX;
  }

  public static float getDeltaY() {
    return deltaY;
  }

  public static void setDeltaX(float deltaX) {
    FaceGraphic.deltaX = deltaX;
  }

  public static void setDeltaY(float deltaY) {
    FaceGraphic.deltaY = deltaY;
  }

  public PointF centroid(ArrayList<PointF> knots)  {
    PointF center = new PointF();
    double centroidX = 0, centroidY = 0;

    for(PointF knot : knots) {
      centroidX += knot.x;
      centroidY += knot.y;
    }
    center.x = (float) (centroidX / knots.size());
    center.y = (float) (centroidY / knots.size());

    return center;
  }

  public float pdistance(PointF a,PointF b){
    float l = b.x - a.x;
    float r = b.y - a.y;
    float diff = (float) Math.sqrt(l*l + r*r);
    return  diff;
  }
  
  // 0 no stress 
  // 1 neutral 
  // 2 high stress
  // 3 medium stress 
  // 4 low stress

  public String focus(float smile, float leye, float reye){
    // 0 no stress
    // 1 neutral
    // 2 high stress
    // 3 medium stress
    // 4 low stress
    if(smile>0.8 ){
      return "GOOD";
    }
    else if(smile <0.8 && smile >0.5){
      return "neutral";
    }
    else {
      if (leye < 0.2 && leye > 0.0 || reye < 0.2 && reye > 0.0){
        // high stress
        return "High Stress";
      }
      else if(leye < 0.5 && leye > 0.2 || reye < 0.5 && reye > 0.2 ){
        // medium stress
        return "Medium Stress";
      }
      else if(leye < 0.8 && leye > 0.5 || reye < 0.8 && reye > 0.5){
        // low stress
        return "Low Stress";
      }
      else {
        //no stress
        return "Good";
      }
    }
  }


}
