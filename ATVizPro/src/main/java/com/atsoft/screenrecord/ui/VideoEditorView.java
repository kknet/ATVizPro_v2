package com.atsoft.screenrecord.ui;

import static com.atsoft.screenrecord.ui.VideoTrimmerUtil.MAX_COUNT_RANGE;
import static com.atsoft.screenrecord.ui.VideoTrimmerUtil.MAX_SHOOT_DURATION;
import static com.atsoft.screenrecord.ui.VideoTrimmerUtil.RECYCLER_VIEW_PADDING;
import static com.atsoft.screenrecord.ui.VideoTrimmerUtil.THUMB_WIDTH;
import static com.atsoft.screenrecord.ui.VideoTrimmerUtil.VIDEO_FRAMES_WIDTH;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atsoft.screenrecord.R;
import com.atsoft.screenrecord.adapter.VideoOptionAdapter;
import com.atsoft.screenrecord.utils.AdsUtil;
import com.atsoft.screenrecord.utils.OnSingleClickListener;
import com.google.android.gms.ads.AdListener;

import java.util.ArrayList;

import iknow.android.utils.callback.SingleCallback;
import iknow.android.utils.thread.BackgroundExecutor;
import iknow.android.utils.thread.UiThreadExecutor;

/**
 * Author：J.Chou
 * Date：  2016.08.01 2:23 PM
 * Email： who_know_me@163.com
 * Describe:
 */
public class VideoEditorView extends FrameLayout implements IVideoCustomView, VideoOptionAdapter.VideoOptionListener {

    private static final String TAG = VideoEditorView.class.getSimpleName();

    private int mMaxWidth = VIDEO_FRAMES_WIDTH;
    private Context mContext;
    private RelativeLayout mLinearVideo;
    private VideoView mVideoView;
    private ImageView mPlayView;
    private RecyclerView mVideoThumbRecyclerView, rcVideoOptions;
    private RangeSeekBarView mRangeSeekBarView;
    private LinearLayout mSeekBarLayout;
    private ImageView mRedProgressIcon;
    private TextView mVideoShootTipTv;
    private float mAverageMsPx;//每毫秒所占的px
    private float averagePxMs;//每px所占用的ms毫秒
    private Uri mSourceUri;
    private IVideoStreamView IVideoStreamView;
    private int mDuration = 0;
    private VideoThumbAdapter mVideoThumbAdapter;
    private boolean isFromRestore = false;
    //new
    private long mLeftProgressPos, mRightProgressPos;
    private long mRedProgressBarPos = 0;
    private long scrollPos = 0;
    private int mScaledTouchSlop;
    private int lastScrollX;
    private boolean isSeeking;
    private boolean isOverScaledTouchSlop;
    private int mThumbsTotalCount;
    private ValueAnimator mRedProgressAnimator;
    private Handler mAnimationHandler = new Handler();

    private TextView btn_cancel, btn_save, number_countdown;
    private LinearLayout layoutCountdown;

    private ArrayList<String> videoOptions = new ArrayList<>();
    private VideoOptionAdapter mAdapter;

    private RelativeLayout mAdview;
    private MediaPlayer mediaPlayer;

    public interface VideoEditorListener {
        void onClickVideoOption(String opt);
    }

    private VideoEditorListener videoEditorListener;

    public void setVideoEditorListener(VideoEditorListener videoEditorListener){
        this.videoEditorListener = videoEditorListener;
    }

    public VideoEditorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        this.mContext = context;
        LayoutInflater.from(context).inflate(R.layout.video_editor_view, this, true);

        mLinearVideo = findViewById(R.id.screenVideo);
        layoutCountdown = findViewById(R.id.ln_countdown);
        number_countdown = findViewById(R.id.tv_number_countdown);
        btn_cancel = findViewById(R.id.tv_btn_cancel);
        btn_save = findViewById(R.id.tv_btn_done);
        mVideoView = findViewById(R.id.video_loader);
        MediaController mediaController = new MediaController(getContext());
        mVideoView.setMediaController(mediaController);
        mSeekBarLayout = findViewById(R.id.seekBarLayout);
        mRedProgressIcon = findViewById(R.id.positionIcon);

        mAdview = findViewById(R.id.adView);

        videoOptions.add("Trim");
        videoOptions.add("Rotate");
        videoOptions.add("Speed");
        videoOptions.add("Text");
        videoOptions.add("Image");
        rcVideoOptions = findViewById(R.id.recycler_view_position);
        mAdapter = new VideoOptionAdapter(context, videoOptions, this);
        rcVideoOptions.setAdapter(mAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        rcVideoOptions.setLayoutManager(linearLayoutManager);


        mVideoThumbRecyclerView = findViewById(R.id.video_frames_recyclerView);
        mVideoThumbRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        mVideoThumbAdapter = new VideoThumbAdapter(mContext);
        mVideoThumbRecyclerView.setAdapter(mVideoThumbAdapter);
        mVideoThumbRecyclerView.addOnScrollListener(mOnScrollListener);
        setUpListeners();
    }

    public void onPressSave() {
        btn_save.setVisibility(VISIBLE);
    }

    private AdsUtil mAdManager;
    public void showOrHideAdBanner(){
        mAdview = findViewById(R.id.adView);
        mAdManager = new AdsUtil(mContext, mAdview);
        mAdManager.loadBanner();
        if (mAdManager.getAdView() != null) mAdManager.getAdView().setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                if (mediaPlayer != null) {
                    hasChangeVideoView();
                }
            }
        });
    }
    private void hasChangeVideoView() {
        if (mLinearVideo == null) return;
        int screenWidth = mLinearVideo.getWidth();
        int screenHeight = mLinearVideo.getHeight();
        mLinearVideo.post(new Runnable() {
            @Override
            public void run() {
                if (screenWidth != mLinearVideo.getWidth()
                        || screenHeight != mLinearVideo.getHeight()) {
                    updateVideoView(mediaPlayer);
                }
            }
        });
    }

    public long getVideoDuration() {
        return mVideoView.getDuration();
    }


    private void initRangeSeekBarView() {
        mLeftProgressPos = 0;
        if (mDuration <= MAX_SHOOT_DURATION) {
            mThumbsTotalCount = MAX_COUNT_RANGE;
            mRightProgressPos = mDuration;
        } else {
            mThumbsTotalCount = (int) (mDuration * 1.0f / (MAX_SHOOT_DURATION * 1.0f) * MAX_COUNT_RANGE);
            mRightProgressPos = MAX_SHOOT_DURATION;
        }
        if (mRangeSeekBarView != null) {
            mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
            return;
        }

        mVideoThumbRecyclerView.addItemDecoration(new SpacesItemDecoration(RECYCLER_VIEW_PADDING, mThumbsTotalCount));
        mRangeSeekBarView = new RangeSeekBarView(mContext, mLeftProgressPos, mRightProgressPos, 1);
        mRangeSeekBarView.setSelectedMinValue(mLeftProgressPos);
        mRangeSeekBarView.setSelectedMaxValue(mRightProgressPos);
        mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
        mRangeSeekBarView.setMinShootTime(mDuration);
        mRangeSeekBarView.setNotifyWhileDragging(true);
        mRangeSeekBarView.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        mSeekBarLayout.removeView(mRangeSeekBarView);
        mSeekBarLayout.addView(mRangeSeekBarView);
        if (mThumbsTotalCount - MAX_COUNT_RANGE > 0) {
            mAverageMsPx = (mDuration - MAX_SHOOT_DURATION) / (float) (mThumbsTotalCount - MAX_COUNT_RANGE);
        } else {
            mAverageMsPx = 0f;
        }
        averagePxMs = (mMaxWidth * 1.0f / (mRightProgressPos - mLeftProgressPos));
    }

    public void initVideoByURI(final Uri videoURI) {
        mSourceUri = videoURI;
        mVideoView.setVideoURI(videoURI);
        mVideoView.requestFocus();
    }

    private void startShootVideoThumbs(final Context context, final Uri videoUri, int totalThumbsCount, long startPosition, long endPosition) {
        mVideoThumbAdapter.resetBitmap();
        VideoTrimmerUtil.shootVideoThumbInBackground(context, videoUri, totalThumbsCount, startPosition, endPosition,
                new SingleCallback<Bitmap, Integer>() {
                    @Override
                    public void onSingleCallback(final Bitmap bitmap, final Integer interval) {
                        if (bitmap != null) {
                            UiThreadExecutor.runTask("", new Runnable() {
                                @Override
                                public void run() {
                                    mVideoThumbAdapter.addBitmaps(bitmap);
                                }
                            }, 0L);
                        }
                    }
                });
    }

    private void onCancelClicked() {
        if (IVideoStreamView != null)
            IVideoStreamView.onCancel();
    }
    private void updateVideoView(MediaPlayer mp) {
        ViewGroup.LayoutParams lpVideo = mVideoView.getLayoutParams();
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();

        double videoRatio = (double) videoWidth / (double) videoHeight;

        int screenWidth = mLinearVideo.getWidth();
        int screenHeight = mLinearVideo.getHeight();

        double viewRatio = (double) screenWidth / (double) screenHeight;

        double diffRatio = videoRatio/viewRatio - 1;

        if (Math.abs(diffRatio) < 0.01) {
            // very good
        } else {
            if (diffRatio > 0) {
                //fit width
                lpVideo.width = screenWidth;
                lpVideo.height = (int) (lpVideo.width / videoRatio);
            } else {
                //fit height
                lpVideo.height = screenHeight + 2;
                lpVideo.width = (int) (lpVideo.height * videoRatio);

            }
        }
        mVideoView.setLayoutParams(lpVideo);

    }
    private void videoPrepared(MediaPlayer mp) {
        updateVideoView(mp);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoView.setAlpha(1);
            }
        }, 1000);
        mDuration = mVideoView.getDuration();
        if (getRestoreState()) {
            setRestoreState(false);
        }
        seekTo((int) mRedProgressBarPos);
        initRangeSeekBarView();
        startShootVideoThumbs(mContext, mSourceUri, mThumbsTotalCount, 0, mDuration);
    }

    private void videoCompleted() {
        seekTo(mLeftProgressPos);
    }

    private void onVideoReset() {
        mVideoView.pause();
    }

    public void onVideoPause() {
        if (mVideoView.isPlaying()) {
            seekTo(mLeftProgressPos);//复位
            mVideoView.pause();
            mRedProgressIcon.setVisibility(GONE);
        }
    }

    public void setOnEditVideoListener(IVideoStreamView onStreamVideoListener) {
        IVideoStreamView = onStreamVideoListener;
    }

    private void setUpListeners() {
        findViewById(R.id.tv_btn_cancel).setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                onCancelClicked();
            }
        });

        findViewById(R.id.tv_btn_done).setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                onDoneClicked();
            }
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer = mp;
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                videoPrepared(mp);
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoCompleted();
            }
        });
    }

    public void toggleSaveButton(boolean status) {
        findViewById(R.id.tv_btn_done).setEnabled(status);
    }

    private void onDoneClicked() {
        if (IVideoStreamView != null)
        IVideoStreamView.onClickNext();
    }

    public void seekTo(long msec) {
        if (mVideoView != null)
            mVideoView.seekTo((int) msec);
        Log.d(TAG, "seekTo = " + msec);
    }

    private boolean getRestoreState() {
        return isFromRestore;
    }

    public void setRestoreState(boolean fromRestore) {
        isFromRestore = fromRestore;
    }


    private final RangeSeekBarView.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBarView.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBarView bar, long minValue, long maxValue, int action, boolean isMin,
                                                RangeSeekBarView.Thumb pressedThumb) {
            Log.d(TAG, "-----minValue----->>>>>>" + minValue);
            Log.d(TAG, "-----maxValue----->>>>>>" + maxValue);
            mLeftProgressPos = minValue + scrollPos;
            mRedProgressBarPos = mLeftProgressPos;
            mRightProgressPos = maxValue + scrollPos;
            Log.d(TAG, "-----mLeftProgressPos----->>>>>>" + mLeftProgressPos);
            Log.d(TAG, "-----mRightProgressPos----->>>>>>" + mRightProgressPos);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isSeeking = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    isSeeking = true;
                    seekTo((int) (pressedThumb == RangeSeekBarView.Thumb.MIN ? mLeftProgressPos : mRightProgressPos));
                    break;
                case MotionEvent.ACTION_UP:
                    isSeeking = false;
                    seekTo((int) mLeftProgressPos);
                    break;
                default:
                    break;
            }

            mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
        }
    };

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            Log.d(TAG, "newState = " + newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            isSeeking = false;
            int scrollX = calcScrollXDistance();
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < mScaledTouchSlop) {
                isOverScaledTouchSlop = false;
                return;
            }
            isOverScaledTouchSlop = true;
            //初始状态,why ? 因为默认的时候有35dp的空白！
            if (scrollX == -RECYCLER_VIEW_PADDING) {
                scrollPos = 0;
            } else {
                isSeeking = true;
                scrollPos = (long) (mAverageMsPx * (RECYCLER_VIEW_PADDING + scrollX) / THUMB_WIDTH);
                mLeftProgressPos = mRangeSeekBarView.getSelectedMinValue() + scrollPos;
                mRightProgressPos = mRangeSeekBarView.getSelectedMaxValue() + scrollPos;
                Log.d(TAG, "onScrolled >>>> mLeftProgressPos = " + mLeftProgressPos);
                mRedProgressBarPos = mLeftProgressPos;
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                }
                mRedProgressIcon.setVisibility(GONE);
                seekTo(mLeftProgressPos);
                mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
                mRangeSeekBarView.invalidate();
            }
            lastScrollX = scrollX;
        }
    };

    /**
     * 水平滑动了多少px
     */
    private int calcScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mVideoThumbRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

    private void playingRedProgressAnimation() {
        pauseRedProgressAnimation();
        playingAnimation();
        mAnimationHandler.post(mAnimationRunnable);
    }

    private void playingAnimation() {
        if (mRedProgressIcon.getVisibility() == View.GONE) {
            mRedProgressIcon.setVisibility(View.VISIBLE);
        }
        final LayoutParams params = (LayoutParams) mRedProgressIcon.getLayoutParams();
        int start = (int) (RECYCLER_VIEW_PADDING + (mRedProgressBarPos - scrollPos) * averagePxMs);
        int end = (int) (RECYCLER_VIEW_PADDING + (mRightProgressPos - scrollPos) * averagePxMs);
        mRedProgressAnimator = ValueAnimator.ofInt(start, end).setDuration((mRightProgressPos - scrollPos) - (mRedProgressBarPos - scrollPos));
        mRedProgressAnimator.setInterpolator(new LinearInterpolator());
        mRedProgressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.leftMargin = (int) animation.getAnimatedValue();
                mRedProgressIcon.setLayoutParams(params);
                Log.d(TAG, "----onAnimationUpdate--->>>>>>>" + mRedProgressBarPos);
            }
        });
        mRedProgressAnimator.start();
    }

    private void pauseRedProgressAnimation() {
        mRedProgressIcon.clearAnimation();
        if (mRedProgressAnimator != null && mRedProgressAnimator.isRunning()) {
            mAnimationHandler.removeCallbacks(mAnimationRunnable);
            mRedProgressAnimator.cancel();
        }
    }

    private Runnable mAnimationRunnable = new Runnable() {

        @Override
        public void run() {
            updateVideoProgress();
        }
    };

    private void updateVideoProgress() {
        long currentPosition = mVideoView.getCurrentPosition();
        Log.d(TAG, "updateVideoProgress currentPosition = " + currentPosition);
        if (currentPosition >= (mRightProgressPos)) {
            mRedProgressBarPos = mLeftProgressPos;
            pauseRedProgressAnimation();
            onVideoPause();
        } else {
            mAnimationHandler.post(mAnimationRunnable);
        }
    }

    /**
     * Cancel trim thread execut action when finish
     */
    @Override
    public void onDestroy() {
        BackgroundExecutor.cancelAll("", true);
        UiThreadExecutor.cancelAll("");
    }

    //callback click video options
    @Override
    public void onClickItem(String text) {
        if (videoEditorListener != null)
            videoEditorListener.onClickVideoOption(text);
    }
}
