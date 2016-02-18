package me.heron.safefoodscanner.ui.camera;

/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.StringDef;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.heron.safefoodscanner.Constants;

@SuppressWarnings("deprecation")
public class CameraSource implements Camera.AutoFocusCallback {

    @SuppressLint("InlinedApi")
    public static final int CAMERA_FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    @SuppressLint("InlinedApi")
    public static final int CAMERA_FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private static final String TAG = Constants.LOG_PREFIX + "OpenCameraSource";

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        Log.d(TAG, "Focal Length = " + camera.getParameters().getFocalLength());
        Log.d(TAG, String.format("Auto focus success=%s. Focus mode: '%s'. Focused on: %s. Count: %d.",
                success,
                camera.getParameters().getFocusMode(),
                camera.getParameters().getFocusAreas().get(0).rect.toString(),
                camera.getParameters().getFocusAreas().size()));
    }

    @StringDef({
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
            Camera.Parameters.FOCUS_MODE_AUTO,
            Camera.Parameters.FOCUS_MODE_EDOF,
            Camera.Parameters.FOCUS_MODE_FIXED,
            Camera.Parameters.FOCUS_MODE_INFINITY,
            Camera.Parameters.FOCUS_MODE_MACRO
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface FocusMode {}

    @StringDef({
            Camera.Parameters.FLASH_MODE_ON,
            Camera.Parameters.FLASH_MODE_OFF,
            Camera.Parameters.FLASH_MODE_AUTO,
            Camera.Parameters.FLASH_MODE_RED_EYE,
            Camera.Parameters.FLASH_MODE_TORCH
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface FlashMode {}


    private Context mContext;

    private final Object mCameraLock = new Object();

    private Camera mCamera;
    private Rect mSurfaceFrame;

    private int mFacing = CAMERA_FACING_BACK;

    private Size mPreviewSize;
    private int mRotation;

    private float mRequestedFps = Constants.BARCODE_DEFAULT_REQUESTED_FPS;
    private int mRequestedPreviewWidth = Constants.BARCODE_DEFAULT_PICTURE_WIDTH;
    private int mRequestedPreviewHeight = Constants.BARCODE_DEFAULT_PICTURE_HEIGHT;


    private String mFocusMode = null;
    private String mFlashMode = null;

    private Thread mProcessingThread;
    private FrameProcessingRunnable mFrameProcessor;

    private Map<byte[], ByteBuffer> mBytesToByteBuffer = new HashMap<>();


    public void focusOnTouch(MotionEvent event) {

        Log.d(TAG, "touch on: " + event.getX() + "," + event.getY());

        if (mCamera != null ) {

            List<Camera.Area> meteringAreas = new ArrayList<>();
            List<Camera.Area> focusAreas = new ArrayList<>();

            meteringAreas.add(getTapAreaOnCoordinate(event.getX(), event.getY()));
            focusAreas.add(getTapAreaOnCoordinate(event.getX(), event.getY()));

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            parameters.setFocusAreas(focusAreas);

            if (parameters.getMaxNumMeteringAreas() > 0){
                parameters.setMeteringAreas(meteringAreas);
            } else {
                Log.d(TAG, "metering or focusing area selection is unsupported");
            }

            mCamera.setParameters(parameters);
            mCamera.autoFocus(this);

        }

    }

    private Camera.Area getTapAreaOnCoordinate(float x, float y) {
        Rect rect = calculateFocusArea(x, y);
        return new Camera.Area(rect, Constants.FOCUS_AREA_DEFAULT_WEIGHT);
    }

    private Rect calculateFocusArea(float x, float y) {
        int areaSize = Constants.FOCUS_AREA_SIZE;
        int left = clamp((int) x - areaSize / 2, 0, mSurfaceFrame.width() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, mSurfaceFrame.height() - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }


    //==============================================================================================
    // Builder
    //==============================================================================================

    public static class Builder {

        private final Detector<?> mDetector;
        private CameraSource mCameraSource = new CameraSource();

        public Builder(Context context, Detector<?> detector) {

            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }
            if (detector == null) {
                throw new IllegalArgumentException("No detector supplied.");
            }

            mDetector = detector;
            mCameraSource.mContext = context;

        }

        public Builder setRequestedFps(float fps) {
            if (fps <= 0) {
                throw new IllegalArgumentException("Invalid fps: " + fps);
            }
            mCameraSource.mRequestedFps = fps;
            return this;
        }

        public Builder setFocusMode(@FocusMode String mode) {
            mCameraSource.mFocusMode = mode;
            return this;
        }

        public Builder setFlashMode(@FlashMode String mode) {
            mCameraSource.mFlashMode = mode;
            return this;
        }

        public Builder setRequestedPreviewSize(int width, int height) {

            final int MAX = 1000000;
            if ((width <= 0) || (width > MAX) || (height <= 0) || (height > MAX)) {
                throw new IllegalArgumentException("Invalid preview size: " + width + "x" + height);
            }
            mCameraSource.mRequestedPreviewWidth = width;
            mCameraSource.mRequestedPreviewHeight = height;

            return this;
        }

        public Builder setFacing(int facing) {
            if ((facing != CAMERA_FACING_BACK) && (facing != CAMERA_FACING_FRONT)) {
                throw new IllegalArgumentException("Invalid camera: " + facing);
            }
            mCameraSource.mFacing = facing;
            return this;
        }

        public CameraSource build() {
            mCameraSource.mFrameProcessor = mCameraSource.new FrameProcessingRunnable(mDetector);
            return mCameraSource;
        }

    }

    //==============================================================================================
    // Bridge Functionality for the Camera1 API
    //==============================================================================================

    public interface ShutterCallback {
        void onShutter();
    }

    public interface PictureCallback {
        void onPictureTaken(byte[] data);
    }

    public interface AutoFocusCallback {
        void onAutoFocus(boolean success);
    }

    public interface AutoFocusMoveCallback {
        void onAutoFocusMoving(boolean start);
    }

    //==============================================================================================
    // Public
    //==============================================================================================

    public void release() {
        synchronized (mCameraLock) {
            stop();
            mFrameProcessor.release();
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public CameraSource start(SurfaceHolder surfaceHolder) throws IOException {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                return this;
            }

            mCamera = createCamera();
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
            mSurfaceFrame = surfaceHolder.getSurfaceFrame();

            mProcessingThread = new Thread(mFrameProcessor);
            mFrameProcessor.setActive(true);
            mProcessingThread.start();
        }
        return this;
    }

    public void stop() {
        synchronized (mCameraLock) {
            mFrameProcessor.setActive(false);
            if (mProcessingThread != null) {
                try {
                    mProcessingThread.join();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Frame processing thread interrupted on release.");
                }
                mProcessingThread = null;
            }

            mBytesToByteBuffer.clear();

            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallbackWithBuffer(null);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mCamera.setPreviewTexture(null);

                    } else {
                        mCamera.setPreviewDisplay(null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to clear camera preview: " + e);
                }
                mCamera.release();
                mCamera = null;
            }
        }
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public int getCameraFacing() {
        return mFacing;
    }

    @Nullable
    @FocusMode
    public String getFocusMode() {
        return mFocusMode;
    }

    public boolean setFocusMode(@FocusMode String mode) {

        synchronized (mCameraLock) {
            if (mCamera != null && mode != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getSupportedFocusModes().contains(mode)) {
                    parameters.setFocusMode(mode);
                    mCamera.setParameters(parameters);
                    mFocusMode = mode;
                    return true;
                }
            }

            return false;
        }
    }

    @Nullable
    @FlashMode
    public String getFlashMode() {
        return mFlashMode;
    }

    public boolean setFlashMode(@FlashMode String mode) {
        synchronized (mCameraLock) {
            if (mCamera != null && mode != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getSupportedFlashModes().contains(mode)) {
                    parameters.setFlashMode(mode);
                    mCamera.setParameters(parameters);
                    mFlashMode = mode;
                    return true;
                }
            }

            return false;
        }
    }

    public void autoFocus(@Nullable AutoFocusCallback cb) {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                CameraAutoFocusCallback autoFocusCallback = null;
                if (cb != null) {
                    autoFocusCallback = new CameraAutoFocusCallback();
                    autoFocusCallback.mDelegate = cb;
                }
                mCamera.autoFocus(autoFocusCallback);
            }
        }
    }

    public void cancelAutoFocus() {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                mCamera.cancelAutoFocus();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public boolean setAutoFocusMoveCallback(@Nullable AutoFocusMoveCallback cb) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return false;
        }

        synchronized (mCameraLock) {
            if (mCamera != null) {
                CameraAutoFocusMoveCallback autoFocusMoveCallback = null;
                if (cb != null) {
                    autoFocusMoveCallback = new CameraAutoFocusMoveCallback();
                    autoFocusMoveCallback.mDelegate = cb;
                }
                mCamera.setAutoFocusMoveCallback(autoFocusMoveCallback);
            }
        }

        return true;
    }

    //==============================================================================================
    // Private
    //==============================================================================================

    private CameraSource() {
    }

    private class CameraAutoFocusCallback implements Camera.AutoFocusCallback {
        private AutoFocusCallback mDelegate;

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onAutoFocus(success);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private class CameraAutoFocusMoveCallback implements Camera.AutoFocusMoveCallback {
        private AutoFocusMoveCallback mDelegate;

        @Override
        public void onAutoFocusMoving(boolean start, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onAutoFocusMoving(start);
            }
        }
    }

    @SuppressLint("InlinedApi")
    private Camera createCamera() {

        int requestedCameraId = getIdForRequestedCamera(mFacing);
        if (requestedCameraId == -1) {
            throw new RuntimeException("Could not find requested camera.");
        }
        Camera camera = Camera.open(requestedCameraId);

        SizePair sizePair = selectSizePair(camera, mRequestedPreviewWidth, mRequestedPreviewHeight);
        if (sizePair == null) {
            throw new RuntimeException("Could not find suitable preview size.");
        }
        Size pictureSize = sizePair.pictureSize();

        int[] previewFpsRange = selectPreviewFpsRange(camera, mRequestedFps);
        if (previewFpsRange == null) {
            throw new RuntimeException("Could not find suitable preview frames per second range.");
        }

        Camera.Parameters parameters = camera.getParameters();

        int width = Constants.BARCODE_DEFAULT_PICTURE_WIDTH;
        int height = Constants.BARCODE_DEFAULT_PICTURE_HEIGHT;

        mPreviewSize = sizePair.previewSize();
        parameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        parameters.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        parameters.setPreviewFpsRange(
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        parameters.setPreviewFormat(ImageFormat.NV21);

        setRotation(camera, parameters, requestedCameraId);

        if (mFocusMode != null) {
            if (parameters.getSupportedFocusModes().contains(mFocusMode)) {
                parameters.setFocusMode(mFocusMode);
            } else {
                Log.i(TAG, "Camera focus mode: " + mFocusMode + " is not supported on this device.");
                final String macroFocusMode = Camera.Parameters.FOCUS_MODE_MACRO;
                if (parameters.getSupportedFocusModes().contains(macroFocusMode)) {
                    parameters.setFocusMode(macroFocusMode);
                    Log.i(TAG, "Use macro focus mode instead.");
                }
            }
        }

        mFocusMode = parameters.getFocusMode();

        if (mFlashMode != null) {
            if (parameters.getSupportedFlashModes().contains(
                    mFlashMode)) {
                parameters.setFlashMode(mFlashMode);
            } else {
                Log.i(TAG, "Camera flash mode: " + mFlashMode + " is not supported on this device.");
            }
        }

        mFlashMode = parameters.getFlashMode();

        camera.setParameters(parameters);

        // Four frame buffers are needed for working with the camera:
        //
        //   one for the frame that is currently being executed upon in doing detection
        //   one for the next pending frame to process immediately upon completing detection
        //   two for the frames that the camera uses to populate future preview images
        camera.setPreviewCallbackWithBuffer(new CameraPreviewCallback());
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize));
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize));
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize));
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize));

        return camera;
    }

    private static int getIdForRequestedCamera(int facing) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return i;
            }
        }
        return -1;
    }

    private static SizePair selectSizePair(Camera camera, int desiredWidth, int desiredHeight) {
        List<SizePair> validPreviewSizes = generateValidPreviewSizeList(camera);

        SizePair selectedPair = null;
        int minDiff = Integer.MAX_VALUE;
        for (SizePair sizePair : validPreviewSizes) {
            Size size = sizePair.previewSize();
            int diff = Math.abs(size.getWidth() - desiredWidth) +
                    Math.abs(size.getHeight() - desiredHeight);
            if (diff < minDiff) {
                selectedPair = sizePair;
                minDiff = diff;
            }
        }

        return selectedPair;
    }

    private static class SizePair {
        private Size mPreview;
        private Size mPicture;

        public SizePair(android.hardware.Camera.Size previewSize,
                        android.hardware.Camera.Size pictureSize) {
            mPreview = new Size(previewSize.width, previewSize.height);
            mPicture = new Size(pictureSize.width, pictureSize.height);
        }

        public Size previewSize() {
            return mPreview;
        }

        @SuppressWarnings("unused")
        public Size pictureSize() {
            return mPicture;
        }
    }

    private static List<SizePair> generateValidPreviewSizeList(Camera camera) {

        Camera.Parameters parameters = camera.getParameters();

        List<android.hardware.Camera.Size> supportedPreviewSizes =
                parameters.getSupportedPreviewSizes();
        List<android.hardware.Camera.Size> supportedPictureSizes =
                parameters.getSupportedPictureSizes();
        List<SizePair> validPreviewSizes = new ArrayList<>();

        for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {

            int width = Constants.BARCODE_DEFAULT_PICTURE_WIDTH, height = Constants.BARCODE_DEFAULT_PICTURE_HEIGHT;
            Camera.Size pictureSize = camera.new Size(width, height);
            validPreviewSizes.add(new SizePair(previewSize, pictureSize));

        }

        if (validPreviewSizes.size() == 0) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size");
            for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
                validPreviewSizes.add(new SizePair(previewSize, null));
            }
        }

        return validPreviewSizes;
    }

    private int[] selectPreviewFpsRange(Camera camera, float desiredPreviewFps) {
        int desiredPreviewFpsScaled = (int) (desiredPreviewFps * 1000.0f);

        int[] selectedFpsRange = null;
        int minDiff = Integer.MAX_VALUE;
        List<int[]> previewFpsRangeList = camera.getParameters().getSupportedPreviewFpsRange();
        for (int[] range : previewFpsRangeList) {
            int deltaMin = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
            int deltaMax = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
            int diff = Math.abs(deltaMin) + Math.abs(deltaMax);
            if (diff < minDiff) {
                selectedFpsRange = range;
                minDiff = diff;
            }
        }
        return selectedFpsRange;

    }

    private void setRotation(Camera camera, Camera.Parameters parameters, int cameraId) {
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int degrees = 0;
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                Log.e(TAG, "Bad rotation value: " + rotation);
        }

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int angle;
        int displayAngle;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360;
            displayAngle = (360 - angle); // compensate for it being mirrored
        } else {  // back-facing
            angle = (cameraInfo.orientation - degrees + 360) % 360;
            displayAngle = angle;
        }

        // This corresponds to the rotation constants in {@link Frame}.
        mRotation = angle / 90;

        camera.setDisplayOrientation(displayAngle);
        parameters.setRotation(angle);
    }

    private byte[] createPreviewBuffer(Size previewSize) {
        int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        long sizeInBits = previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
        int bufferSize = (int) Math.ceil(sizeInBits / 8.0d) + 1;

        byte[] byteArray = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        if (!buffer.hasArray() || (buffer.array() != byteArray)) {
            throw new IllegalStateException("Failed to create valid buffer for camera source.");
        }

        mBytesToByteBuffer.put(byteArray, buffer);
        return byteArray;
    }

    //==============================================================================================
    // Frame processing
    //==============================================================================================

    private class CameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            mFrameProcessor.setNextFrame(data, camera);
        }
    }

    private class FrameProcessingRunnable implements Runnable {
        private Detector<?> mDetector;
        private long mStartTimeMillis = SystemClock.elapsedRealtime();

        private final Object mLock = new Object();
        private boolean mActive = true;

        private long mPendingTimeMillis;
        private int mPendingFrameId = 0;
        private ByteBuffer mPendingFrameData;

        FrameProcessingRunnable(Detector<?> detector) {
            mDetector = detector;
        }

        @SuppressLint("Assert")
        void release() {
            assert (mProcessingThread.getState() == Thread.State.TERMINATED);
            mDetector.release();
            mDetector = null;
        }

        void setActive(boolean active) {
            synchronized (mLock) {
                mActive = active;
                mLock.notifyAll();
            }
        }

        void setNextFrame(byte[] data, Camera camera) {
            synchronized (mLock) {
                if (mPendingFrameData != null) {
                    camera.addCallbackBuffer(mPendingFrameData.array());
                    mPendingFrameData = null;
                }

                mPendingTimeMillis = SystemClock.elapsedRealtime() - mStartTimeMillis;
                mPendingFrameId++;
                mPendingFrameData = mBytesToByteBuffer.get(data);

                mLock.notifyAll();
            }
        }

        @Override
        public void run() {
            Frame outputFrame;
            ByteBuffer data;

            while (true) {
                synchronized (mLock) {
                    if (mActive && (mPendingFrameData == null)) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }

                    if (!mActive) {
                        return;
                    }

                    outputFrame = new Frame.Builder()
                            .setImageData(mPendingFrameData, mPreviewSize.getWidth(),
                                    mPreviewSize.getHeight(), ImageFormat.NV21)
                            .setId(mPendingFrameId)
                            .setTimestampMillis(mPendingTimeMillis)
                            .setRotation(mRotation)
                            .build();

                    data = mPendingFrameData;
                    mPendingFrameData = null;
                }

                try {
                    mDetector.receiveFrame(outputFrame);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception thrown from receiver.", t);
                } finally {
                    mCamera.addCallbackBuffer(data.array());
                }
            }
        }
    }
}
