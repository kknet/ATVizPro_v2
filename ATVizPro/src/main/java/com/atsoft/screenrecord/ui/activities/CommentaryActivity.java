package com.atsoft.screenrecord.ui.activities;

import static com.atsoft.screenrecord.ui.activities.MainActivity.KEY_PATH_VIDEO;
import static com.atsoft.screenrecord.ui.fragments.PopupConfirm.KEY_NEGATIVE;
import static com.atsoft.screenrecord.ui.fragments.PopupConfirm.KEY_POSITIVE;
import static com.atsoft.screenrecord.ui.fragments.PopupConfirm.KEY_TITLE;
import static com.atsoft.screenrecord.ui.utils.MyUtils.ACTION_FOR_COMMENTARY;
import static com.atsoft.screenrecord.ui.utils.MyUtils.hideStatusBar;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.atsoft.screenrecord.Core;
import com.atsoft.screenrecord.R;
import com.atsoft.screenrecord.model.VideoProfileExecute;
import com.atsoft.screenrecord.ui.fragments.IConfirmPopupListener;
import com.atsoft.screenrecord.ui.fragments.PopupConfirm;
import com.atsoft.screenrecord.ui.services.ExecuteService;
import com.atsoft.screenrecord.ui.utils.MyUtils;
import com.atsoft.screenrecord.utils.AdsUtil;
import com.atsoft.screenrecord.utils.FirebaseUtils;
import com.atsoft.screenrecord.utils.StorageUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommentaryActivity extends AppCompatActivity implements View.OnClickListener {

    VideoView videoView;
    private ImageView btnRetake;
    private ProgressBar progressBar;
    ObjectAnimator animationProgressBar;
    private TextView tvDurationCounter, btnNext;
    int timeCounter = 0;
    private TextView number_countdown;
    private LinearLayout layoutCountdown;
    private ImageView toggleReactCam, thumbVideo;
    private boolean hasAudioFile = false;
    private int endTime = 0;
    private AdsUtil mAdManager;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_react_cam);
        hideStatusBar(this);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        LottieAnimationView animationView = findViewById(R.id.animation_view);
        animationView.setVisibility(View.GONE);

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(getString(R.string.commentary));

        ImageView btnInfo = findViewById(R.id.img_btn_info);
        btnInfo.setVisibility(View.GONE);
        toggleReactCam = findViewById(R.id.img_btn_react_cam);
        thumbVideo = findViewById(R.id.thumbnail_video);
        btnRetake = findViewById(R.id.img_btn_discard);
        btnNext = findViewById(R.id.tv_next_react_cam);
        btnRetake.setVisibility(View.GONE);
        btnRetake.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        tvDurationCounter = findViewById(R.id.tv_count_duration);
        toggleReactCam.setOnClickListener(this);

        btn_back = findViewById(R.id.img_btn_back_header);
        btn_back.setOnClickListener(this);

        progressBar = findViewById(R.id.progressBar);
        animationProgressBar = ObjectAnimator.ofInt(progressBar, "progress", 0, 500); // see this max value coming back here, we animate towards that value
        animationProgressBar.setInterpolator(new LinearInterpolator());
        progressBar.setProgress(0);
        tvDurationCounter.setText("");

        layoutCountdown = findViewById(R.id.ln_countdown);
        number_countdown = findViewById(R.id.tv_number_countdown);
        prepareAudioRecorder();
        addVideoView();

        RelativeLayout mAdview = findViewById(R.id.adView);
        mAdManager = new AdsUtil(this, mAdview);
        if (mAdManager.getAdView() != null)
            mAdManager.getAdView().setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    if (mediaPlayer != null) {
                        checkHasChangeVideoCamView();
                    }
                }
            });
        mAdManager.createInterstitialAdmob();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdManager.loadBanner();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private MediaRecorder mediaRecorder;
    private String cacheAudioFilePath;

    // this process must be done prior to the start of recording
    private void prepareAudioRecorder() {
        cacheAudioFilePath = StorageUtil.getCacheDir() + "/CacheAudio_" + getTimeStamp() + ".mp3";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(16 * 96000);
        mediaRecorder.setAudioSamplingRate(96000);
        mediaRecorder.setOutputFile(cacheAudioFilePath);
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    MediaPlayer mediaPlayer;
    String videoFile = "";
    int videoDuration = 0;

    private void addVideoView() {

        videoView = findViewById(R.id.video_main1);
        videoFile = getIntent().getStringExtra(KEY_PATH_VIDEO);
        if (!videoFile.equals("")) {
            videoView.setVideoPath(videoFile);
            Glide.with(this)
                    .load(videoFile)
                    .into(thumbVideo);
        }
        videoView.requestFocus();
        videoView.setOnPreparedListener(mp -> {
            videoDuration = mp.getDuration();
            animationProgressBar.setDuration(videoDuration);
            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            videoPrepared(mp);
            mediaPlayer = mp;
            videoView.setOnCompletionListener(mediaPlayer -> getEndCommentary());
        });
    }


    RelativeLayout screenVideo;
    boolean hasChangeViewPos = false;

    private void videoPrepared(MediaPlayer mp) {
        ViewGroup.LayoutParams lpVideo = videoView.getLayoutParams();
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();
        double videoRatio = (double) videoWidth / (double) videoHeight;

        screenVideo = findViewById(R.id.screenVideo);
        int screenWidth = screenVideo.getWidth();
        int screenHeight = screenVideo.getHeight();
        double screenRatio = (double) screenWidth / (double) screenHeight;

        double diffRatio = videoRatio / screenRatio - 1;

        if (Math.abs(diffRatio) > 0.01) {
            if (diffRatio > 0) {
                //closed width
                lpVideo.width = screenWidth;
                lpVideo.height = (int) (lpVideo.width / videoRatio) + 1;
            } else {
                //closed height
                lpVideo.height = screenHeight + 1;
                lpVideo.width = (int) (lpVideo.height * videoRatio);
            }
        }
        videoView.setLayoutParams(lpVideo);
        videoView.seekTo(100);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                videoView.setAlpha(1);
            }
        }, 1000);
    }

    ImageView btn_back;

    @Override
    protected void onPause() {
        super.onPause();
//        mCounterUpdateHandler.removeCallbacks(mUpdateCounter);
        if (!hasEndCommentary) {
            getEndCommentary();
        } else {
            countDownTimer.cancel();
            layoutCountdown.setVisibility(View.GONE);
            thumbVideo.setVisibility(View.VISIBLE);
            pauseRecord = true;
            toggleReactCam.setEnabled(true);
        }

    }

    private void checkHasChangeVideoCamView() {
        hasChangeViewPos = false;
        if (screenVideo == null) {
            return;
        }
        int oldScreenWidth = screenVideo.getWidth();
        int oldScreenHeight = screenVideo.getHeight();
        screenVideo.post(() -> {
            if (oldScreenWidth != screenVideo.getWidth()
                    || oldScreenHeight != screenVideo.getHeight()) {
                hasChangeViewPos = true;
                videoPrepared(mediaPlayer);
            }
        });
    }

    @SuppressLint("SimpleDateFormat")
    public String getTimeStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    private final CountDownTimer countDownTimer = new CountDownTimer(2900, 1000) {
        @SuppressLint("SetTextI18n")
        public void onTick(long millisUntilFinished) {
            layoutCountdown.setVisibility(View.VISIBLE);
            number_countdown.setText("" + (millisUntilFinished / 1000 + 1));
        }

        public void onFinish() {
            layoutCountdown.setVisibility(View.GONE);
            toggleReactCam.setEnabled(true);
            getStartCommentary();
        }
    };

    private boolean pauseRecord = true;

    public void onClick(View v) {

        if (v == findViewById(R.id.img_btn_discard)) {
//            showDialogConfirm("Retake this video?", "Start over");
            showPopupConfirm("Retake this video?", "Start over", "Cancel", false);
        }

        if (v == findViewById(R.id.tv_next_react_cam)) {
//            showDialogConfirmExecute();
            showPopupConfirm(getString(R.string.confirm_execute_react_cam), null, null, true);
        }

        if (v == findViewById(R.id.img_btn_react_cam)) {
            toggleReactCam.setEnabled(false);
            if (hasAudioFile) return;
            if (!pauseRecord) {
                //
                getEndCommentary();
            } else {
                countDownTimer.start();
            }
        }

        if (v == findViewById(R.id.img_btn_back_header)) {
            if (pauseRecord) { // khi da bam stop hoac chua thuc su start
                if (hasAudioFile) { // neu da co video ok
                    showPopupConfirm("Discard the last clip?", "Discard", "Cancel", false);
                } else finish();
            } else { // dang trong tien trinh ghi
                showPopupConfirm("Do you want to cancel processing?", "Yes", "No", false);
            }
        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        if (pauseRecord) { // khi da bam stop hoac chua thuc su start
            if (hasAudioFile) { // neu da co video ok
                showPopupConfirm("Discard the last clip?", "Discard", "Cancel", false);
            } else finish();
        } else { // dang trong tien trinh ghi
            showPopupConfirm("Do you want to cancel processing?", "Yes", "No", false);
        }
    }

    private void showPopupConfirm(String title, String pos, String neg, boolean requiredFinish) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_POSITIVE, pos);
        bundle.putString(KEY_NEGATIVE, neg);
        PopupConfirm.newInstance(new IConfirmPopupListener() {
            @Override
            public void onClickPositiveButton() {
                if (requiredFinish) {
                    showInterstitialAd();
                    finish();
                } else {
                    doPositiveButton(pos);
                }
            }

            @Override
            public void onClickNegativeButton() {
            }
        }, bundle).show(getSupportFragmentManager(), "");
    }

    public void retakeVideo() {
        progressBar.setProgress(0);
        tvDurationCounter.setText("");
        thumbVideo.setVisibility(View.VISIBLE);
        progressBar.setBackgroundResource(R.drawable.ic_play_react_svg);
        if (hasAudioFile) {
            boolean deleteAudioCache = new File(cacheAudioFilePath).delete();
            cacheAudioFilePath = "";
            hasAudioFile = false;
        }
        btnRetake.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
        toggleReactCam.setEnabled(true);
        prepareAudioRecorder();
    }

    public void discardVideo() {
        if (hasAudioFile) {
            boolean deleteAudioCache = new File(cacheAudioFilePath).delete();
        }
        finish();
    }

    private void startExecuteService() {
        VideoProfileExecute videoProfile = new VideoProfileExecute(videoFile, cacheAudioFilePath,
                0, 0, 0, 0, 0, false, false);
        Bundle bundle = new Bundle();
        bundle.putSerializable(MyUtils.KEY_SEND_PACKAGE_VIDEO, videoProfile);
        Intent intent = new Intent(this, ExecuteService.class);
        intent.setAction(ACTION_FOR_COMMENTARY);
        intent.putExtras(bundle);
        intent.putExtra("bundle_video_execute_time", (long) ((endTime + videoDuration) / 4));
        startService(intent);
    }

    public FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdClicked() {
        }

        @Override
        public void onAdDismissedFullScreenContent() {

            AdsUtil.lastTime = (new Date()).getTime();
            mAdManager.createInterstitialAdmob();

            startExecuteService();
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
            startExecuteService();
        }

        @Override
        public void onAdImpression() {
        }

        @Override
        public void onAdShowedFullScreenContent() {

            if (Core.countAdsShown == 5) {
                FirebaseUtils.logEventShowInterstitialAd(mFirebaseAnalytics, "Commentary");
                Core.countAdsShown = 0;
            }
            Core.countAdsShown++;
        }
    };

    public void showInterstitialAd() {
        if (mAdManager.interstitialAdAlready()) {
            mAdManager.showInterstitialAd(fullScreenContentCallback);
        } else {
            startExecuteService();
        }
    }

    private final Handler mCounterUpdateHandler = new Handler();
    private final Runnable mUpdateCounter = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(() -> tvDurationCounter.setText(parseTime(timeCounter / 1000)));
            timeCounter = timeCounter + 50;
            mCounterUpdateHandler.postDelayed(this, 50);
        }
    };

    @SuppressLint("DefaultLocale")
    private String parseTime(int timeCounter) {
        int hh = timeCounter / 3600;
        int mm = timeCounter / 60;
        int ss = timeCounter % 60;
        if (hh == 0) return String.format("%02d:%02d", mm, ss);
        return String.format("%d:%02d:%02d", hh, mm, ss);
    }

    private void doPositiveButton(String action) {

        if (action == null) return;
        if (action.equals("Start over")) retakeVideo();
        if (action.equals("Discard")) discardVideo();
        if (action.equals("Yes")) doCancelCommentary();
    }

    private void doCancelCommentary() {
        if (!pauseRecord && !hasAudioFile && mediaRecorder != null) {    // dang trong tien trinh ghi
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        finish();
    }

    boolean hasEndCommentary = true;

    private void getEndCommentary() {
        if (videoView.isPlaying()) videoView.pause();
        endTime = timeCounter;
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
        }
        mediaRecorder = null;
        progressBar.clearAnimation();
        progressBar.setBackgroundResource(R.drawable.ic_play_react_svg_pause);
        animationProgressBar.cancel();
        mCounterUpdateHandler.removeCallbacks(mUpdateCounter);
        btnRetake.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.VISIBLE);
        hasAudioFile = true;
        pauseRecord = true;
        hasEndCommentary = true;
    }

    private void getStartCommentary() {
        thumbVideo.setVisibility(View.GONE);
        mediaRecorder.start();
        videoView.start();
        timeCounter = 0;
        animationProgressBar.start();
        mCounterUpdateHandler.post(mUpdateCounter);
        hasAudioFile = false;
        pauseRecord = false;
        hasEndCommentary = false;
    }

}