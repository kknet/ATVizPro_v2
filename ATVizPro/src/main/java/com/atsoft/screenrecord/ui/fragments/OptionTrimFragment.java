package com.atsoft.screenrecord.ui.fragments;

import static com.atsoft.screenrecord.ui.utils.MyUtils.parseLongToTime;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atsoft.screenrecord.R;
import com.atsoft.screenrecord.utils.OnSingleClickListener;
import com.atsoft.screenrecord.utils.FFmpegUtil;

import org.jetbrains.annotations.NotNull;

import it.sephiroth.android.library.rangeseekbar.RangeSeekBar;

public class OptionTrimFragment extends DialogFragmentBase {


    public static final String ARG_PARAM1 = "param1";
    public static final String ARG_PARAM2 = "param2";
    private static final String TAG = OptionTrimFragment.class.getSimpleName();

    public static OptionTrimFragment newInstance(IOptionFragmentListener callback, SeekbarCallback callback2, Bundle args) {
        OptionTrimFragment dialogSelectVideoSource = new OptionTrimFragment(callback, callback2);
        dialogSelectVideoSource.setArguments(args);
        return dialogSelectVideoSource;
    }

    public IOptionFragmentListener callback;

    public interface SeekbarCallback {
        void onSeekTo(long ms);
    }

    private SeekbarCallback seekbarCallback;

    public OptionTrimFragment(IOptionFragmentListener callback, SeekbarCallback callback2) {
        this.callback = callback;
        this.seekbarCallback = callback2;
    }

    @Override
    public int getLayout() {
        return R.layout.option_trim_fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    long videoDuration, startTime, endTime;
    TextView tvStartTime, tvEndTime;
    String video_path;

    int oldLeft, oldRight;
    boolean isMinRange = false;
    boolean isChangeLeft = false;
    boolean isChangeRight = false;
    ImageView btn_done;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        videoDuration = getArguments() != null ? getArguments().getLong("video_duration", 0) : 0;
        video_path = getArguments() != null ? getArguments().getString("video_path", "") : "";

        ImageView btn_close = view.findViewById(R.id.iv_close);
        btn_done = view.findViewById(R.id.iv_done);
        tvStartTime = view.findViewById(R.id.actvStartTime);
        tvEndTime = view.findViewById(R.id.actvEndTime);

        tvStartTime.setText(parseLongToTime(0));
        tvEndTime.setText(parseLongToTime(videoDuration));
        btn_close.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                dismiss();
            }
        });
        btn_done.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                processingTrimming();
            }
        });

        RangeSeekBar rangeSlider = view.findViewById(R.id.sbrvVideoTrim);

        rangeSlider.setMax((int) videoDuration);
        rangeSlider.setProgress(0, (int) videoDuration);
        oldLeft = 0;
        oldRight = (int) videoDuration;
        startTime = oldLeft;
        endTime = oldRight;
        rangeSlider.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {

            @Override
            public void onProgressChanged(RangeSeekBar rangeSeekBar, int i, int i1, boolean b) {
                if (i != oldLeft) {  // change left
                    isChangeLeft = true;
                    isChangeRight = false;
                    if (i1 - i < 1000) {
                        rangeSeekBar.setPressed(false);
                        oldLeft = oldRight - 1000;
                        isMinRange = true;
                    } else {
                        rangeSeekBar.setPressed(true);
                        oldLeft = i;
                        isMinRange = false;
                    }

                    seekbarCallback.onSeekTo((long) i);
                }
                if (i1 != oldRight) {  // change right
                    isChangeLeft = false;
                    isChangeRight = true;
                    if (i1 - i < 1000) {
                        rangeSeekBar.setPressed(false);
                        oldRight = oldLeft + 1000;
                        isMinRange = true;
                    } else {
                        rangeSeekBar.setPressed(true);
                        oldRight = i1;
                        isMinRange = false;
                    }
                    seekbarCallback.onSeekTo((long) i1);
                }

                tvStartTime.setText(parseLongToTime((long) i));
                tvEndTime.setText(parseLongToTime((long) i1));


            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar rangeSeekBar) {
                isChangeLeft = false;
                isChangeRight = false;
                isMinRange = false;
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar rangeSeekBar) {

                if (isChangeLeft) {
                    if (isMinRange) {
                        oldRight = rangeSeekBar.getProgressEnd();
                        oldLeft = oldRight - 1000;
                    }
                    seekbarCallback.onSeekTo((long) oldLeft);
                }
                if (isChangeRight) {
                    if (isMinRange) {
                        oldLeft = rangeSeekBar.getProgressStart();
                        oldRight = oldLeft + 1000;
                    }
                    seekbarCallback.onSeekTo((long) oldRight);
                }

                tvStartTime.setText(parseLongToTime((long) oldLeft));
                tvEndTime.setText(parseLongToTime((long) oldRight));
                startTime = oldLeft;
                endTime = oldRight;
                rangeSeekBar.setProgress(oldLeft, oldRight);
            }
        });


    }

    private void processingTrimming() {
        callback.onClickDone();
        dismiss();
        FFmpegUtil.getInstance().trimVideo(video_path, startTime, endTime, new FFmpegUtil.ITranscoding() {
            @Override
            public void onStartTranscoding(String outPath) {
            }

            @Override
            public void onFinishTranscoding(String code) {
                if (!code.equals("")) callback.onFinishProcess(code);
            }
        });

    }

    @Override
    public void updateUI() {
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow()
                        .setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                getDialog().getWindow().setGravity(Gravity.BOTTOM);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


}
