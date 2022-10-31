package com.examples.atscreenrecord_test.ui.services.recording;

import static com.examples.atscreenrecord_test.ui.activities.MainActivity.KEY_PATH_VIDEO;
import static com.examples.atscreenrecord_test.ui.services.ExecuteService.ACTION_STOP_SERVICE;
import static com.examples.atscreenrecord_test.ui.services.ExecuteService.KEY_VIDEO_PATH;
import static com.examples.atscreenrecord_test.ui.utils.MyUtils.DEBUG;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import com.examples.atscreenrecord_test.App;
import com.examples.atscreenrecord_test.controllers.encoder.MediaAudioEncoder;
import com.examples.atscreenrecord_test.controllers.encoder.MediaEncoder;
import com.examples.atscreenrecord_test.controllers.encoder.MediaMuxerWrapper;
import com.examples.atscreenrecord_test.controllers.encoder.MediaScreenEncoderHard;
import com.examples.atscreenrecord_test.controllers.settings.SettingManager2;
import com.examples.atscreenrecord_test.controllers.settings.VideoSetting2;
import com.examples.atscreenrecord_test.ui.activities.PopUpResultVideoTranslucentActivity;
import com.examples.atscreenrecord_test.ui.services.BaseService;
import com.examples.atscreenrecord_test.ui.services.ControllerService;
import com.examples.atscreenrecord_test.ui.utils.MyUtils;

import java.io.IOException;

public class RecordingService extends BaseService {
    private final IBinder mIBinder = new RecordingBinder();

    private static final String TAG = RecordingService.class.getSimpleName();
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private Intent mScreenCaptureIntent;
    private int mScreenCaptureResultCode;
    private int mScreenWidth, mScreenHeight, mScreenDensity;
    private MediaMuxerWrapper mMuxer;
    private static final Object sSync = new Object();
    private VideoSetting2 mCurrentVideoSetting;
    private VideoSetting2 mResultVideo;

    public class RecordingBinder extends Binder {
        public RecordingService getService() {
            return RecordingService.this;
        }
    }

    public RecordingService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);

    }

    private void getScreenSize() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mScreenDensity = metrics.densityDpi;
//        int width = metrics.widthPixels;
//        int height = metrics.heightPixels;
//        if (width > height) {
//            final float scale_x = width / 1920f;
//            final float scale_y = height / 1080f;
//            final float scale = Math.max(scale_x, scale_y);
//            width = (int) (width / scale);
//            height = (int) (height / scale);
//        } else {
//            final float scale_x = width / 1080f;
//            final float scale_y = height / 1920f;
//            final float scale = Math.max(scale_x, scale_y);
//            width = (int) (width / scale);
//            height = (int) (height / scale);
//        }
//        mScreenWidth = width;
//        mScreenHeight = height;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "RecordingService: onBind()");
        mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        mScreenCaptureResultCode = mScreenCaptureIntent.getIntExtra(MyUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, MyUtils.RESULT_CODE_FAILED);
        Log.i(TAG, "onBind: " + mScreenCaptureIntent);
        return mIBinder;
    }

    @Override
    public void openPerformService() {
    }

    @Override
    public void startPerformService() {
        Log.i(TAG, "startPerformService: from RecordingService");
        startRecording();
    }

    @Override
    public void stopPerformService() {
        mResultVideo = stopRecording();
    }

    @Override
    public void closePerformService() {

    }

    public void startRecording() {
        synchronized (sSync) {
            if (mMuxer == null) {
                getScreenSize();
                mMediaProjection = mMediaProjectionManager.getMediaProjection(mScreenCaptureResultCode, mScreenCaptureIntent);
                DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
                Display defaultDisplay;
                if (dm != null) {
                    defaultDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY);
                } else {
                    throw new IllegalStateException("Cannot display manager?!?");
                }
                if (defaultDisplay == null) {
                    throw new RuntimeException("No display found.");
                }


                if (DEBUG) Log.i(TAG, "startStreaming:");
                try {
                    mMuxer = new MediaMuxerWrapper(this, ".mp4");    // if you record audio only, ".m4a" is also OK.

                    mCurrentVideoSetting = SettingManager2.getVideoProfile(getApplicationContext());
                    new MediaScreenEncoderHard(mMuxer, mMediaEncoderListener, mMediaProjection, mCurrentVideoSetting, mScreenDensity);
                    new MediaAudioEncoder(mMuxer, mMediaEncoderListener);

                    mMuxer.prepare();
                    mMuxer.startRecording();
                } catch (final IOException e) {
                    Log.e(TAG, "startScreenRecord:", e);
                }
            }
        }
    }

    public void pauseScreenRecord() {
        synchronized (sSync) {
            if (mMuxer != null) {
                mMuxer.pauseRecording();
            }
        }
    }

    public void resumeScreenRecord() {
        synchronized (sSync) {
            if (mMuxer != null) {
                mMuxer.resumeRecording();
            }
        }
    }

    String outputFile;
    //Return output file
    public VideoSetting2 stopRecording() {
        if (DEBUG) Log.v(TAG, "stopStreaming:mMuxer=" + mMuxer);

        outputFile = "";

        synchronized (sSync) {
            if (mMuxer != null) {
                outputFile = mMuxer.getOutputPath();
                mCurrentVideoSetting.setOutputPath(outputFile);
                mMuxer.stopRecording();
                mMuxer = null;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showResultActivity(outputFile);
                    }
                }, 1000);
                // you should not wait here
            }
        }
        return mCurrentVideoSetting;
    }

    public void showResultActivity(String finalVideoCachePath) {
//        fff
//        Intent intent = new Intent(getApplicationContext(), ResultVideoFinishActivity.class);
//        intent.putExtra(KEY_PATH_VIDEO, finalVideoCachePath);
//        intent.setAction(MyUtils.ACTION_END_RECORD);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);

        App.ignoreOpenAd = true;
        Intent myIntent = new Intent(getApplicationContext(), PopUpResultVideoTranslucentActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myIntent.setAction(ACTION_STOP_SERVICE);
        myIntent.putExtra(KEY_VIDEO_PATH, finalVideoCachePath);
        startActivity(myIntent);

        Intent intent2 = new Intent(getApplicationContext(), ControllerService.class);
        intent2.putExtra(KEY_PATH_VIDEO, finalVideoCachePath);
        intent2.setAction(MyUtils.ACTION_END_RECORD);
    }

    public void insertVideoToGallery() {
        Log.i(TAG, "insertVideoToGallery: ");
        String outputFile = mResultVideo.getOutputPath();
        if (TextUtils.isEmpty(outputFile))
            return;

        //send video to gallery
        ContentResolver cr = getContentResolver();

        ContentValues values = new ContentValues(2);

        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, outputFile);

        // Add a new record (identified by uri) without the video, but with the values just set.
        Uri uri = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        Log.i(TAG, "insertVideoToGallery: " + uri.getPath());

        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    }


    private static final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.i(TAG, "onPrepared:encoder=" + encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.i(TAG, "onStopped:encoder=" + encoder);
        }
    };
}
