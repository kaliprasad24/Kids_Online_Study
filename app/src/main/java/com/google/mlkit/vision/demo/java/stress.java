//package com.google.mlkit.vision.demo.java;
//
//import android.annotation.SuppressLint;
//import android.graphics.Bitmap;
//import android.os.AsyncTask;
//import android.util.Log;
//import android.widget.Toast;
//
//
//import com.google.android.gms.tasks.Task;
//import com.google.mlkit.vision.common.InputImage;
//import com.microsoft.projectoxford.face.FaceServiceClient;
//import com.microsoft.projectoxford.face.FaceServiceRestClient;
//import com.microsoft.projectoxford.face.contract.Face;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//import java.util.List;
//
//import org.json.JSONObject;
//
//public class stress  {
//
//    private FaceServiceClient faceServiceClient = new FaceServiceRestClient("https://centralindia.api.cognitive.microsoft.com/face/v1.0/", "de8df0fb1aac4453997d306778c2b400");
//    JSONObject jsonObject,jsonObject1;
//
//
//
//
//    private void detectAndFrame(final Bitmap imageBitmap) {
//
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
//        ByteArrayInputStream inputStream =
//                new ByteArrayInputStream(outputStream.toByteArray());
//
//        @SuppressLint("StaticFieldLeak") AsyncTask<InputStream, String, Face[]> detectTask =
//
//                new AsyncTask<InputStream, String, Face[]>() {
//                    String exceptionMessage = "";
//
//                    @Override
//                    protected Face[] doInBackground(InputStream... params) {
//                        try {
//                            publishProgress("Detecting...");
//                            Face[] result = faceServiceClient.detect(
//                                    params[0],
//                                    true,         // returnFaceId
//                                    false,        // returnFaceLandmarks
//                                    // returnFaceAttributes:
//                                    new FaceServiceClient.FaceAttributeType[] {
//                                            FaceServiceClient.FaceAttributeType.Emotion,
//                                            FaceServiceClient.FaceAttributeType.Gender }
//                            );
//
//                            for (int i=0;i<result.length;i++) {
//                                jsonObject.put("happiness" , result[i].faceAttributes.emotion.happiness);
//                                jsonObject.put("sadness" , result[i].faceAttributes.emotion.sadness);
//                                jsonObject.put("surprise" , result[i].faceAttributes.emotion.surprise);
//                                jsonObject.put("neutral"  , result[i].faceAttributes.emotion.neutral);
//                                jsonObject.put("anger" , result[i].faceAttributes.emotion.anger);
//                                jsonObject.put("contempt" , result[i].faceAttributes.emotion.contempt);
//                                jsonObject.put("disgust" , result[i].faceAttributes.emotion.disgust);
//                                jsonObject.put("fear" , result[i].faceAttributes.emotion.fear);
//                                Log.e("doinback", "doInBackground: "+jsonObject.toString()  );
//
//                                jsonObject1.put(  (String.valueOf(i)),jsonObject);
//                            }
//
//
//
//
//
//                            if (result == null) {
//                                publishProgress(
//                                        "Detection Finished. Nothing detected");
//                                return null;
//                            }
//                            Log.e("TAG", "doInBackground: "+"   "+result.length );
//                            publishProgress(String.format(
//                                    "Detection Finished. %d face(s) detected",
//                                    result.length));
//
//                            return result;
//                        } catch (Exception e) {
//                            exceptionMessage = String.format(
//                                    "Detection failed: %s", e.getMessage());
//                            return null;
//                        }
//                    }
//
//                };
//
//        detectTask.execute(inputStream);
//    }
//
//
//
//}
