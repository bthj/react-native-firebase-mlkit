
package com.mlkit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.util.List;

public class RNMlKitModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private FirebaseVisionTextRecognizer textDetector;
  private FirebaseVisionTextRecognizer cloudTextDetector;
  private FirebaseVisionFaceDetector faceDetector;

  public RNMlKitModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @ReactMethod
  public void deviceBarcodeRecognition(String uri, final Promise promise) {
    try {
      FirebaseVisionBarcodeDetectorOptions options =
        new FirebaseVisionBarcodeDetectorOptions.Builder()
        .setBarcodeFormats(FirebaseVisionBarcode. FORMAT_ALL_FORMATS)
        .build();
      FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(this.reactContext, android.net.Uri.parse(uri));
      FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance()
        .getVisionBarcodeDetector(options);
      
      Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
        .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
            @Override
            public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
                WritableArray data = Arguments.createArray();
                WritableMap info = Arguments.createMap();

                for (FirebaseVisionBarcode barcode: barcodes) {
                    info = Arguments.createMap();
                    info.putString("format", barcodeFormat(barcode.getFormat()));
                    info.putString("value", barcode.getRawValue());
                    data.pushMap(info);
                }
                promise.resolve(data);
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                promise.reject(e);
            }
      });
    } catch (IOException e) {
      promise.reject(e);
      e.printStackTrace();
    }
  }

  @ReactMethod
  public void deviceTextRecognition(String uri, final Promise promise) {
      try {
          FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(this.reactContext, android.net.Uri.parse(uri));
          FirebaseVisionTextRecognizer detector = this.getTextRecognizerInstance();
          Task<FirebaseVisionText> result =
                  detector.processImage(image)
                          .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                              @Override
                              public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                  promise.resolve(processDeviceResult(firebaseVisionText));
                              }
                          })
                          .addOnFailureListener(
                                  new OnFailureListener() {
                                      @Override
                                      public void onFailure(@NonNull Exception e) {
                                          e.printStackTrace();
                                          promise.reject(e);
                                      }
                                  });;
      } catch (IOException e) {
          promise.reject(e);
          e.printStackTrace();
      }
  }
  @ReactMethod
  public void deviceFaceRecognition(String uri, final Promise promise){
      try{
          FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(this.reactContext, android.net.Uri.parse(uri));
          FirebaseVisionFaceDetector detector = this.getFaceDetectorInstance();

          Task<List<FirebaseVisionFace>> result = detector.detectInImage(image)
                                                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                                                                @Override
                                                                public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                                                                    promise.resolve(processFaceDetectionResult(firebaseVisionFaces));
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    e.printStackTrace();
                                                                    promise.reject(e);
                                                                }
                                                            });
      }catch (Exception e){
          promise.reject(e);
          e.printStackTrace();
      }
  }
  private FirebaseVisionTextRecognizer getTextRecognizerInstance() {
    if (this.textDetector == null) {
      this.textDetector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    }

    return this.textDetector;
  }
  private FirebaseVisionFaceDetector getFaceDetectorInstance(){
      if(this.faceDetector == null){
          //=====Set options=====
          FirebaseVisionFaceDetectorOptions options =
                  new FirebaseVisionFaceDetectorOptions.Builder()
                          .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                          .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                          .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                          .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                          .setMinFaceSize(0.15f)
                          .enableTracking()
                          .build();
          //=====================
          this.faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);
      }
      return this.faceDetector;
  }

  @ReactMethod
  public void close(final Promise promise) {
    if(this.textDetector != null) {
      try {
        this.textDetector.close();
        this.textDetector = null;
        promise.resolve(true);
      } catch (IOException e) {
        e.printStackTrace();
        promise.reject(e);
      }
    }

    if(this.cloudTextDetector != null) {
      try {
        this.cloudTextDetector.close();
        this.cloudTextDetector = null;
        promise.resolve(true);
      } catch (IOException e) {
        e.printStackTrace();
        promise.reject(e);
      }
    }
      if(this.faceDetector != null) {
          try {
              this.faceDetector.close();
              this.faceDetector = null;
              promise.resolve(true);
          } catch (IOException e) {
              e.printStackTrace();
              promise.reject(e);
          }
      }
  }

  private FirebaseVisionTextRecognizer getCloudTextRecognizerInstance() {
    if (this.cloudTextDetector == null) {
      this.cloudTextDetector = FirebaseVision.getInstance().getCloudTextRecognizer();
    }

    return this.cloudTextDetector;
  }

  @ReactMethod
  public void cloudTextRecognition(String uri, final Promise promise) {
      try {
          FirebaseVisionTextRecognizer detector = this.getCloudTextRecognizerInstance();
          FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(this.reactContext, android.net.Uri.parse(uri));
          Task<FirebaseVisionText> result =
                  detector.processImage(image)
                          .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                              @Override
                              public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                  promise.resolve(processCloudResult(firebaseVisionText));
                              }
                          })
                          .addOnFailureListener(
                                  new OnFailureListener() {
                                      @Override
                                      public void onFailure(@NonNull Exception e) {
                                          e.printStackTrace();
                                          promise.reject(e);
                                      }
                                  });
      } catch (IOException e) {
          promise.reject(e);
          e.printStackTrace();
      }
  }

  private String barcodeFormat(int format) {
      switch (format) {
          case FirebaseVisionBarcode.FORMAT_CODE_128:
              return "CODE_128";
      
          case FirebaseVisionBarcode.FORMAT_CODE_39:
              return "CODE_39";
      
          case FirebaseVisionBarcode.FORMAT_CODE_93:
              return "CODE_93";
            
          case FirebaseVisionBarcode.FORMAT_CODABAR:
              return "CODABAR";

          case FirebaseVisionBarcode.FORMAT_DATA_MATRIX:
              return "DATA_MATRIX";
      
          case FirebaseVisionBarcode.FORMAT_EAN_13:
              return "EAN_13";
      
          case FirebaseVisionBarcode.FORMAT_EAN_8:
              return "EAN_8";
      
          case FirebaseVisionBarcode.FORMAT_ITF:
              return "ITF";
      
          case FirebaseVisionBarcode.FORMAT_QR_CODE:
              return "QR_CODE";
      
          case FirebaseVisionBarcode.FORMAT_UPC_A:
              return "UPC_A";
      
          case FirebaseVisionBarcode.FORMAT_UPC_E:
              return "UPC_E";

          case FirebaseVisionBarcode.FORMAT_PDF417:
              return "PDF417";
      
          case FirebaseVisionBarcode.FORMAT_AZTEC:
              return "AZTEC";
      
          default:
            return "UNKNOWN";
      }
  }

 
  /**
   * Converts firebaseVisionText into a map
   *
   * @param firebaseVisionText
   * @return
   */
  private WritableArray processDeviceResult(FirebaseVisionText firebaseVisionText) {
      WritableArray data = Arguments.createArray();
      WritableMap info = Arguments.createMap();
      WritableMap coordinates = Arguments.createMap();
      List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();

      if (blocks.size() == 0) {
          return data;
      }

      for (int i = 0; i < blocks.size(); i++) {
          List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
          info = Arguments.createMap();
          coordinates = Arguments.createMap();

          Rect boundingBox = blocks.get(i).getBoundingBox();

          coordinates.putInt("top", boundingBox.top);
          coordinates.putInt("left", boundingBox.left);
          coordinates.putInt("width", boundingBox.width());
          coordinates.putInt("height", boundingBox.height());

          info.putMap("blockCoordinates", coordinates);
          info.putString("blockText", blocks.get(i).getText());
          info.putString("resultText", firebaseVisionText.getText());

          for (int j = 0; j < lines.size(); j++) {
              List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
              info.putString("lineText", lines.get(j).getText());

              for (int k = 0; k < elements.size(); k++) {
                  info.putString("elementText", elements.get(k).getText());
              }
          }

          data.pushMap(info);
      }

      return data;
  }

  private WritableArray processCloudResult(FirebaseVisionText firebaseVisionText) {
      WritableArray data = Arguments.createArray();
      WritableMap info = Arguments.createMap();
      WritableMap coordinates = Arguments.createMap();
      List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();

      if (blocks.size() == 0) {
          return data;
      }

      for (int i = 0; i < blocks.size(); i++) {
          List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
          info = Arguments.createMap();
          coordinates = Arguments.createMap();

          Rect boundingBox = blocks.get(i).getBoundingBox();

          coordinates.putInt("top", boundingBox.top);
          coordinates.putInt("left", boundingBox.left);
          coordinates.putInt("width", boundingBox.width());
          coordinates.putInt("height", boundingBox.height());

          info.putMap("blockCoordinates", coordinates);
          info.putString("blockText", blocks.get(i).getText());

          for (int j = 0; j < lines.size(); j++) {
              List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
              info.putString("lineText", lines.get(j).getText());

              for (int k = 0; k < elements.size(); k++) {
                  info.putString("elementText", elements.get(k).getText());
              }
          }

          data.pushMap(info);
      }

      return data;
  }

  private WritableArray processFaceDetectionResult(List<FirebaseVisionFace> firebaseVisionFaces){
      WritableArray data = Arguments.createArray();

      if(firebaseVisionFaces.size() > 0){
          WritableMap info = Arguments.createMap();
          for (FirebaseVisionFace face : firebaseVisionFaces){
              info = Arguments.createMap();
              //face bounding box
              Rect faceBounding = face.getBoundingBox();

              WritableMap faceBox = Arguments.createMap();
              faceBox.putInt("top", faceBounding.top);
              faceBox.putInt("right", faceBounding.right);
              faceBox.putInt("bottom", faceBounding.bottom);
              faceBox.putInt("left", faceBounding.left);

              info.putMap("faceBoundingRect", faceBox);

              // If classification was enabled:
              if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                  float smileProb = face.getSmilingProbability();
                  info.putDouble("smileProbability", (double)smileProb);
              }
              if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                  float rightEyeOpenProb = face.getRightEyeOpenProbability();
                  info.putDouble("rightEyeOpenProbability", (double)rightEyeOpenProb);
              }

              // If face tracking was enabled:
              if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                  int id = face.getTrackingId();
                  info.putInt("faceTrackingId", id);
              }
              //eyes
              info.putArray("leftEyeContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.LEFT_EYE));
              info.putArray("rightEyeContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.RIGHT_EYE));
              //face
              info.putArray("faceContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.FACE));
              //lower lip
              info.putArray("lowerLipBottomContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.LOWER_LIP_BOTTOM));
              info.putArray("lowerLipBottomContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.LOWER_LIP_TOP));
              //upper lip
              info.putArray("upperLipTopContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.UPPER_LIP_TOP));
              info.putArray("upperLipBottomContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.UPPER_LIP_BOTTOM));
              //nose
              info.putArray("noiseBottomContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.NOSE_BOTTOM));
              info.putArray("noiseBridgeContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.NOSE_BRIDGE));
              //left eyebrow
              info.putArray("leftEyeBrowBottomContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM));
              info.putArray("leftEyeBrowTopContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.LEFT_EYEBROW_TOP));
              // right eyebrow
              info.putArray("rightEyeBrowBottomContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM));
              info.putArray("rightEyeBrowTopContourPoints", Utils.getContourPointsWritableArray(face, FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP));



              data.pushMap(info);
          }
      }

      return data;
  }


  ///// Image labeling - https://firebase.google.com/docs/ml-kit/android/label-images

  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
  static {
      ORIENTATIONS.append(Surface.ROTATION_0, 90);
      ORIENTATIONS.append(Surface.ROTATION_90, 0);
      ORIENTATIONS.append(Surface.ROTATION_180, 270);
      ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

  /**
   * Get the angle by which an image must be rotated given the device's current
   * orientation.
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private int getRotationCompensation(String cameraId, Activity activity, Context context)
          throws CameraAccessException {
      // Get the device's current rotation relative to its "native" orientation.
      // Then, from the ORIENTATIONS table, look up the angle the image must be
      // rotated to compensate for the device's rotation.
      int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
      int rotationCompensation = ORIENTATIONS.get(deviceRotation);

      // On most devices, the sensor orientation is 90 degrees, but for some
      // devices it is 270 degrees. For devices with a sensor orientation of
      // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
      CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      int sensorOrientation = cameraManager
              .getCameraCharacteristics(cameraId)
              .get(CameraCharacteristics.SENSOR_ORIENTATION);
      rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

      // Return the corresponding FirebaseVisionImageMetadata rotation value.
      int result;
      switch (rotationCompensation) {
          case 0:
              result = FirebaseVisionImageMetadata.ROTATION_0;
              break;
          case 90:
              result = FirebaseVisionImageMetadata.ROTATION_90;
              break;
          case 180:
              result = FirebaseVisionImageMetadata.ROTATION_180;
              break;
          case 270:
              result = FirebaseVisionImageMetadata.ROTATION_270;
              break;
          default:
              result = FirebaseVisionImageMetadata.ROTATION_0;
              Log.e(activity.getClass().getName(), "Bad rotation value: " + rotationCompensation);
      }
      return result;
  }


  @ReactMethod
  public void deviceImageLabeling(String uri, final Promise promise) {
    try {
      FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(this.reactContext, android.net.Uri.parse(uri));
      FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();

      labeler.processImage(image)
              .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                @Override
                public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                  promise.resolve(processDeviceImageLabelingResult(labels));
                }
              })
              .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                  e.printStackTrace();
                  promise.reject(e);
                }
              });
      
    } catch (IOException e) {
      promise.reject(e);
      e.printStackTrace();
    }
  }

  private WritableArray processDeviceImageLabelingResult(List<FirebaseVisionImageLabel> labels) {
    WritableArray imageLabelsData = Arguments.createArray();
    for( FirebaseVisionImageLabel label : labels ) {
      WritableMap oneLabelMap = Arguments.createMap();

      String text = label.getText();
      String entityId = label.getEntityId();
      float confidence = label.getConfidence();

      oneLabelMap.putString("text", text);
      oneLabelMap.putString("entityId", entityId);
      oneLabelMap.putDouble("confidence", confidence);

      imageLabelsData.pushMap( oneLabelMap );
    }
    return imageLabelsData;
  }


  // TODO: from byte buffer


  @Override
  public String getName() {
    return "RNMlKit";
  }
}
