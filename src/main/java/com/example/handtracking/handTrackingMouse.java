package com.example.handtracking;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.SurfaceTexture;
import android.icu.text.Transliterator;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.glutil.EglManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.Math;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.security.AccessController.getContext;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.mediapipe.components.*;
import com.google.mediapipe.components.CameraHelper.CameraFacing;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.framework.*;
import com.google.mediapipe.glutil.EglManager;
import java.util.*;
import kotlin.math.*;

public class handTrackingMouse extends Activity {
    // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
    // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
    // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
    // corner, whereas MediaPipe in general assumes the image origin is at top-left.
    private static final boolean FLIP_FRAMES_VERTICALLY = true;
    private static final String INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands";
    private static final int NUM_HANDS = 2;
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks";
    private ImageView mImageView;
    private ArrayList<Button> allButtons;
    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private FrameProcessor processor;
    private ArrayList<Float> positionsX = new ArrayList<>();
    private ArrayList<Float> positionsY = new ArrayList<>();
    //Params
    private ConstraintLayout.LayoutParams params;
    private Float previous_mouse_x = 0F;
    private Float previous_mouse_y = 0F;
    private Boolean clicked = false;
    private Button previous_button = null;
    private int gestureHand = 0;
    //    Setting MediaPipe environment and call handlandmarks
    public void init(ImageView mmImageView, ArrayList<Button> aallButtons,
                     EglManager eeglManager, FrameProcessor pprocessor,
                     ConstraintLayout.LayoutParams pparams){
        mImageView = mmImageView;
        allButtons = aallButtons;
        eglManager = eeglManager;
        processor = pprocessor;
        params = pparams;
    }
    public void satus(){
        processor
                .getVideoSurfaceOutput()
                .setFlipY(FLIP_FRAMES_VERTICALLY);

        PermissionHelper.checkAndRequestCameraPermissions(this);
        AndroidPacketCreator packetCreator = processor.getPacketCreator();
        Map<String, Packet> inputSidePackets = new HashMap<>();
        inputSidePackets.put(INPUT_NUM_HANDS_SIDE_PACKET_NAME, packetCreator.createInt32(NUM_HANDS));
        processor.setInputSidePackets(inputSidePackets);

        processor.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME,
                (packet) -> {
                    List<NormalizedLandmarkList> multiHandLandmarks =
                            PacketGetter.getProtoVector(packet, NormalizedLandmarkList.parser());
                    Log.d(
                            "MYTAG",
                            "[TS:"
                                    + packet.getTimestamp()
                                    + "] "
                                    + getMultiHandLandmarksDebugString(multiHandLandmarks));
                    if (multiHandLandmarks.size() != 0 ) {
                        NormalizedLandmarkList landmarks = multiHandLandmarks.get(0);
//            if (noHand(landmarks)) {
                        ArrayList<Float> landmarksX = new ArrayList<>();
                        ArrayList<Float> landmarksY = new ArrayList<>();
                        for (NormalizedLandmark landmark : landmarks.getLandmarkList()){
                            landmarksX.add(landmark.getX());
                            landmarksY.add(landmark.getY());
                        }
//                Area definition
                        ArrayList<Boolean> fingerState = new ArrayList<>();
                        fingerState = defineFingerState(landmarksX, landmarksY);
                        Float currentMouseX = landmarks.getLandmarkList().get(0).getX();
                        Float currentMouseY = landmarks.getLandmarkList().get(0).getY();
                        Log.d(
                                "MYTAG",
                                "MINMAX" + currentMouseX+currentMouseY);
                        currentMouseX = Math.max(0.3F, currentMouseX);
                        currentMouseX = Math.min(0.84F, currentMouseX);
                        currentMouseY = Math.max(0.3F, currentMouseY);
                        currentMouseY = Math.min(0.84F, currentMouseY);
                        currentMouseX = mappingPoint(0.84F, 0.3F, getScreenWidth(), 0, currentMouseX);
                        currentMouseY = mappingPoint(0.84F, 0.3F, getScreenHeight(), 0, currentMouseY);
                        Button targetButton = IsInButton(currentMouseX, currentMouseY);
                        Log.d("MYTAG", "Area " + area(landmarksX,landmarksY) );
                        Log.d("MYTAG", "state " + gestureHand(fingerState.get(0), fingerState.get(1), fingerState.get(2), fingerState.get(3)) );
                        if(area(landmarksX, landmarksY)> 0.02){
                            gestureHand =  gestureHand(fingerState.get(0), fingerState.get(1), fingerState.get(2), fingerState.get(3));
                            
                        }
                    }
                });
    }
    public int getSatusHand(){
        return gestureHand;
    }
    public void setupProcess(Context ccontext) {
//        eglManager = new EglManager(null);
//        processor = new FrameProcessor(
//                this,
//                eglManager.getNativeContext(),
//                BINARY_GRAPH_NAME,
//                INPUT_VIDEO_STREAM_NAME,
//                OUTPUT_VIDEO_STREAM_NAME);
        processor
                .getVideoSurfaceOutput()
                .setFlipY(FLIP_FRAMES_VERTICALLY);

        PermissionHelper.checkAndRequestCameraPermissions(this);
        AndroidPacketCreator packetCreator = processor.getPacketCreator();
        Map<String, Packet> inputSidePackets = new HashMap<>();
        inputSidePackets.put(INPUT_NUM_HANDS_SIDE_PACKET_NAME, packetCreator.createInt32(NUM_HANDS));
        processor.setInputSidePackets(inputSidePackets);

        processor.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME,
                (packet) -> {
                    List<NormalizedLandmarkList> multiHandLandmarks =
                            PacketGetter.getProtoVector(packet, NormalizedLandmarkList.parser());
                    Log.d(
                            "MYTAG",
                            "[TS:"
                                    + packet.getTimestamp()
                                    + "] "
                                    + getMultiHandLandmarksDebugString(multiHandLandmarks));
                    if (multiHandLandmarks.size() != 0 ) {
                        NormalizedLandmarkList landmarks = multiHandLandmarks.get(0);
//            if (noHand(landmarks)) {
                        ArrayList<Float> landmarksX = new ArrayList<>();
                        ArrayList<Float> landmarksY = new ArrayList<>();
                        for (NormalizedLandmark landmark : landmarks.getLandmarkList()){
                            landmarksX.add(landmark.getX());
                            landmarksY.add(landmark.getY());
                        }
//                Area definition
                        ArrayList<Boolean> fingerState = new ArrayList<>();
                        fingerState = defineFingerState(landmarksX, landmarksY);
                        Float currentMouseX = landmarks.getLandmarkList().get(0).getX();
                        Float currentMouseY = landmarks.getLandmarkList().get(0).getY();
                        Log.d(
                                "MYTAG",
                                "MINMAX" + currentMouseX+currentMouseY);
                        currentMouseX = Math.max(0.3F, currentMouseX);
                        currentMouseX = Math.min(0.84F, currentMouseX);
                        currentMouseY = Math.max(0.3F, currentMouseY);
                        currentMouseY = Math.min(0.84F, currentMouseY);
                        currentMouseX = mappingPoint(0.84F, 0.3F, getScreenWidth(), 0, currentMouseX);
                        currentMouseY = mappingPoint(0.84F, 0.3F, getScreenHeight(), 0, currentMouseY);
                        Button targetButton = IsInButton(currentMouseX, currentMouseY);
                        Log.d("MYTAG", "Area " + area(landmarksX,landmarksY) );
                        Log.d("MYTAG", "state " + gestureHand(fingerState.get(0), fingerState.get(1), fingerState.get(2), fingerState.get(3)) );
                        if(area(landmarksX, landmarksY)> 0.02){
                            switch ( gestureHand(fingerState.get(0), fingerState.get(1), fingerState.get(2), fingerState.get(3)) ) {
                                case 1 :
                                    if (positionsX.size() == 4 && positionsY.size() == 4) {
                                        positionsX.remove(0);
                                        positionsY.remove(0);
                                    }
                                    positionsX.add(currentMouseX);
                                    positionsY.add(currentMouseY);
//                            Log.d("MYTAG", "PositionX " + positionsX.toString());
//                            Log.d("MYTAG", "PositionY " + positionsY.toString());
                                    Float targetX = averagePosition(positionsX);
                                    Float targetY = averagePosition(positionsY);
                                    Log.d("MYTAG", "TargetX " + targetX);
                                    Log.d("MYTAG", "TargetY " + targetY);
                                    drawMouse(targetX, targetY);
                                    previous_mouse_x = targetX;
                                    previous_mouse_y = targetY;
                                    clicked = false;
                                    break;
                                case 2 :
                                    if (clicked == false && targetButton != null) {
                                        clicked = true;
                                        drawMouseClick(previous_mouse_x, previous_mouse_y);
//                                        displayText(targetButton);
                                        previous_mouse_x = currentMouseX;
                                        previous_mouse_y = currentMouseY;
                                        previous_button = targetButton;
                                    }
                                    else drawMouseClick(previous_mouse_x, previous_mouse_y);
                                    break;
                                default:
                                    if(clicked == true) drawMouseClick(previous_mouse_x, previous_mouse_y);
                                    else drawMouse(previous_mouse_x, previous_mouse_y);
                            }
                        }
                    }
                });
    }
    private void drawMouse(Float x, Float y ){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                params = new ConstraintLayout.LayoutParams(100, 100);
                mImageView.setImageResource(R.drawable.ic_launcher_movecursor);
                mImageView.setX(x);
                mImageView.setY(y);
                mImageView.setLayoutParams(params);
            }
        });
    }
    private void drawMouseClick(Float x, Float y){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //params
                params = new ConstraintLayout.LayoutParams(100, 100);
                mImageView.setImageResource(R.drawable.ic_launcher_clickcursor);
                Log.d("POSITIONMOUSE", "xy: ${x.toInt()} ${y.toInt()}");
                mImageView.setX(x);
                mImageView.setY(y);
                mImageView.setLayoutParams(params);
            }
        });
    }
    private Float averagePosition(ArrayList<Float> li){
        Float total = new Float(0);
        Float avg = new Float(0);
        for(int i = 0; i < li.size(); i++){
            total += li.get(i);
            avg = total / li.size(); // finding ther average value
        }
        return avg;
    }
    private int gestureHand(Boolean INDEXFINGER, Boolean MIDDLEFINGER, Boolean RINGFINGER, Boolean LITTLEFINGER){
        if (!MIDDLEFINGER && !RINGFINGER && !LITTLEFINGER){
            return 2;
        }
        else if(MIDDLEFINGER && RINGFINGER && LITTLEFINGER){
            return 1;
        }
        else{
            return 0;
        }
    }
    private String getMultiHandLandmarksDebugString(List<NormalizedLandmarkList> multiHandLandmarks) {
        if (multiHandLandmarks.isEmpty()) {
            return "No hand landmarks";
        }
        String multiHandLandmarksStr = "Number of hands detected: " + multiHandLandmarks.size() + "\n";
        int handIndex = 0;
        for (NormalizedLandmarkList landmarks : multiHandLandmarks) {
            multiHandLandmarksStr +=
                    "\t#Hand landmarks for hand[" + handIndex + "]: " + landmarks.getLandmarkCount() + "\n";
            int landmarkIndex = 0;
            for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
                multiHandLandmarksStr +=
                        "\t\tLandmark ["
                                + landmarkIndex
                                + "]: ("
                                + landmark.getX()
                                + ", "
                                + landmark.getY()
                                + ", "
                                + landmark.getZ()
                                + ")\n";
                ++landmarkIndex;
            }
            ++handIndex;
        }
        return multiHandLandmarksStr;
    }
    private Float area(ArrayList<Float> landmarksX, ArrayList<Float> landmarksY ){
        Float area =new Float (0);
        int numVertices = landmarksX.size();
        for (int i =0; i< numVertices - 1;i++) {
            area += landmarksX.get(i) * landmarksY.get(i + 1) - landmarksX.get(i + 1) * landmarksY.get(i);
        }
        area += landmarksX.get(numVertices - 1) * landmarksY.get(0)- landmarksX.get(0) * landmarksY.get(numVertices - 1);
        area = Math.abs(area) / 2.0f;
        return area;
    }
    private Button IsInButton(Float xP, Float yP){
        int mouseX = Math.round(xP);
        int mouseY = Math.round(yP);
        int mouseW = mImageView.getWidth();
        int mouseH = mImageView.getHeight();
        for(Button aButton : allButtons){
            Point pointxy = getPointOfView(aButton);
            int buttonX = pointxy.x;
            int buttonY =  pointxy.y;
            int buttonW = aButton.getWidth();
            int buttonH = aButton.getHeight();
            if(!(mouseX + mouseW < buttonX || mouseY + mouseH < buttonY
                    || mouseX > buttonX + buttonW || mouseY > buttonY + buttonH))
            {
                return aButton;
            }
        }
        return null;
    }
    private Float euclideanDistance(Float aX, Float bX, Float aY, Float bY){
        double result = 0F;
        result = (float) Math.pow((aX-bX), 2) + Math.pow((aY-bY), 2);
        result = sqrt(result);
        return (float) result;
    }
    private ArrayList<Boolean> defineFingerState(ArrayList<Float> axisX, ArrayList<Float> axisY){
        Boolean INDEXFINGER = false;
        Boolean MIDDLEFINGER = false;
        Boolean RINGFINGER = false;
        Boolean LITTLEFINGER = false;
        ArrayList<Boolean> fingerState = new ArrayList<>();
//      Index finger
        Log.d("FingerState", "Index: ${euclideanDistance(axisX[0], axisX[8], axisY[0], axisY[8])}");
        if(euclideanDistance(axisX.get(0), axisX.get(8), axisY.get(0), axisY.get(8))>euclideanDistance(
                axisX.get(0), axisX.get(5), axisY.get(0), axisY.get(5))){
            INDEXFINGER = true;
        }
//      Middle finger
        Log.d(
                "FingerState",
                "middle: ${euclideanDistance(axisX[0], axisX[12], axisY[0], axisY[12])}"
        );
        if(euclideanDistance(axisX.get(0), axisX.get(12), axisY.get(0), axisY.get(12))> euclideanDistance(
                axisX.get(0), axisX.get(9), axisY.get(0), axisY.get(9))){
            MIDDLEFINGER = true;
        }
//      Ring finger
        Log.d("FingerState", "Ring: ${euclideanDistance(axisX[0], axisX[16], axisY[0], axisY[16])}");
        if(euclideanDistance(axisX.get(0), axisX.get(16), axisY.get(0), axisY.get(16))> euclideanDistance(
                axisX.get(0), axisX.get(13), axisY.get(0), axisY.get(13))){
            RINGFINGER = true;
        }
//      Little finger
        Log.d(
                "FingerState",
                "Little: ${euclideanDistance(axisX[0], axisX[20], axisY[0], axisY[20])}"
        );
        if(euclideanDistance(axisX.get(0), axisX.get(20), axisY.get(0), axisY.get(20))> euclideanDistance(
                axisX.get(0), axisX.get(17), axisY.get(0), axisY.get(17))){
            LITTLEFINGER = true;
        }
        fingerState.add(INDEXFINGER);
        fingerState.add(MIDDLEFINGER);
        fingerState.add(RINGFINGER);
        fingerState.add(LITTLEFINGER);
        return fingerState;
    }
    private Float  mappingPoint( Float OldMax, Float OldMin, int NewMax, int NewMin, Float OldValue){
        Float result = 0F;
        result = (OldValue - OldMin) * (NewMax - NewMin);
        result = (result  / (OldMax - OldMin)) + NewMin;
        return  result;
    }
    private int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }
    private int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
    private Point getPointOfView(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new Point(location[0],location[1]);
    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
