package com.atsoft.screenrecord.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atsoft.screenrecord.R;
import com.atsoft.screenrecord.adapter.BasicAdapter;
import com.atsoft.screenrecord.utils.OnSingleClickListener;
import com.atsoft.screenrecord.utils.FFmpegUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class OptionAddTextFragment extends DialogFragmentBase implements BasicAdapter.BasicAdapterListener {

    private static final String TAG = OptionAddTextFragment.class.getSimpleName();

    public static OptionAddTextFragment newInstance(IOptionFragmentListener callback, Bundle args) {

        OptionAddTextFragment dialogSelectVideoSource = new OptionAddTextFragment(callback);
        dialogSelectVideoSource.setArguments(args);
        return dialogSelectVideoSource;
    }

    public IOptionFragmentListener mCallback = null;

    public OptionAddTextFragment() {

    }

    public OptionAddTextFragment(IOptionFragmentListener mCallback) {
        this.mCallback = mCallback;
    }

    @Override
    public int getLayout() {
        return R.layout.option_text_fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private String video_path;
    private EditText inputText;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        video_path = getArguments() != null ? getArguments().getString("video_path", "") : "";

        ImageView btn_close = view.findViewById(R.id.iv_close);
        ImageView btn_done = view.findViewById(R.id.iv_done);
        inputText = view.findViewById(R.id.edt_input);

        btn_close.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                dismiss();
            }
        });
        btn_done.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                processingAddText();
            }
        });

        SeekBar seekBarOfSize = view.findViewById(R.id.seekbar_size);
        seekBarOfSize.setMax(100);
        seekBarOfSize.setProgress(sizeOfText);
        seekBarOfSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sizeOfText = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        ArrayList<String> listPos = new ArrayList<>();
        listPos.add("TopLeft");
        listPos.add("TopRight");
        listPos.add("Center");
        listPos.add("CenterTop");
        listPos.add("CenterBottom");
        listPos.add("BottomLeft");
        listPos.add("BottomRight");

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_position);
        BasicAdapter basicAdapter = new BasicAdapter(getContext(), listPos, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        recyclerView.setAdapter(basicAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);

    }

    String[] position = {
            "15:15", // top left
            "W-w-15:15", //top right
            "(W-w)/2:(H-h)/2", //center
            "(W-w)/2:15", // top center
            "(W-w)/2:H-h-15", // bottom center
            "15:H-h-15", // bottom left
            "W-w-15:H-h-15" // bottom right
    };
    String posSelected = position[0];

    int sizeOfText = 20;

    private void processingAddText() {
        if (inputText.getText().toString().equals("")) {
            inputText.requestFocus();
            Toast.makeText(requireContext(), "Text is empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        mCallback.onClickDone();
        dismiss();
        FFmpegUtil.getInstance().addText(video_path, inputText.getText().toString(), Color.WHITE, sizeOfText, posSelected, new FFmpegUtil.ITranscoding() {
            @Override
            public void onStartTranscoding(String outPath) {
            }

            @Override
            public void onFinishTranscoding(String code) {
                if (!code.equals(""))
                    mCallback.onFinishProcess(code);
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


    @Override
    public void onClickBasicItem(String text) {
        if (text.equals("TopLeft")) posSelected = position[0];
        if (text.equals("TopRight")) posSelected = position[1];
        if (text.equals("Center")) posSelected = position[2];
        if (text.equals("CenterTop")) posSelected = position[3];
        if (text.equals("CenterBottom")) posSelected = position[4];
        if (text.equals("BottomLeft")) posSelected = position[5];
        if (text.equals("BottomRight")) posSelected = position[6];
    }
}
