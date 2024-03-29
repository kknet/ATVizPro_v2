package com.atsoft.screenrecord.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.atsoft.screenrecord.R;
import com.atsoft.screenrecord.adapter.PhotoAdapter;
import com.atsoft.screenrecord.controllers.settings.SettingManager2;
import com.atsoft.screenrecord.model.PhotoModel;
import com.atsoft.screenrecord.ui.activities.MainActivity;
import com.atsoft.screenrecord.utils.AdsUtil;
import com.atsoft.screenrecord.utils.OnSingleClickListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import me.relex.circleindicator.CircleIndicator3;

public class GuidelineLiveStreamFragment extends Fragment {
    ViewPager2 viewPager2;
    CircleIndicator3 circleIndicator3;
    PhotoAdapter photoAdapter;
    TextView btnContinue;
    TextView tvSkip;
    ImageView btnBack;
    int i = 0;
    boolean isFirstTime = true;
    private Activity mParentActivity = null;
    private FragmentManager mFragmentManager;
    private TextView tvDecs;
    boolean fromSettings = false;
    public GuidelineLiveStreamFragment(boolean fromSettings) {
        this.fromSettings = fromSettings;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mFragmentManager = getParentFragmentManager();
        this.mParentActivity = (MainActivity) context;
    }

    private View mViewRoot;
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mViewRoot = inflater.inflate(R.layout.fragment_guideline, container, false);
        isFirstTime  = SettingManager2.getFirstTimeLiveStream(requireContext());
        SettingManager2.setFirstTimeLiveStream(requireContext(), false);
        return mViewRoot;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.title);
        tvTitle.setText(getString(R.string.how_to_livestream));
        btnBack =  view.findViewById(R.id.img_btn_back_header);

        viewPager2 = view.findViewById(R.id.view_pager_img_tutorial);
        circleIndicator3 = view. findViewById(R.id.circle_indicator);
        btnContinue =  view.findViewById(R.id.btn_continue_);
        tvSkip =  view.findViewById(R.id.tv_btn_skip);

        if (isFirstTime && !fromSettings) {
            btnBack.setVisibility(View.GONE);
            tvSkip.setVisibility(View.VISIBLE);
        } else {
            btnBack.setVisibility(View.VISIBLE);
            tvSkip.setVisibility(View.GONE);
        }
        tvDecs =  view.findViewById(R.id.tv_decs);

        photoAdapter = new PhotoAdapter(getContext(), getListPhoto());
        viewPager2.setAdapter(photoAdapter);
        circleIndicator3.setViewPager(viewPager2);

        viewPager2.setClipToPadding(false);
        viewPager2.setClipChildren(false);
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        compositePageTransformer.addTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float r = 1 - Math.abs(position);
                page.setScaleY(0.85f + r*0.15f);
            }
        });

        viewPager2.setPageTransformer(compositePageTransformer);
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == getListPhoto().size()-1) {
                    btnContinue.setText(getString(R.string.done_));
                    if (isFirstTime) {
                        tvSkip.setText(getString(R.string.done_));
                    }
                } else {
                    btnContinue.setText(getString(R.string.continue_));
                    if (isFirstTime) {
                        tvSkip.setText(getString(R.string.skip));
                    }
                }
                setDecs(position);
                i = position;
            }
        });
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                i = i +1;
                if (i == getListPhoto().size() - 1){
                    btnContinue.setText(getString(R.string.done_));
                    if (isFirstTime) {
                        tvSkip.setText(getString(R.string.done_));
                    }
                }
                if (i >= getListPhoto().size()) {
                    mFragmentManager.popBackStack();
                    return;
                }
                viewPager2.setCurrentItem(i);
                setDecs(i);
            }
        });
        tvSkip.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                mFragmentManager.popBackStack();
                ((MainActivity) mParentActivity).showLiveStreamFragment();
            }
        });
        btnBack.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                mFragmentManager.popBackStack();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        SettingManager2.setFirstTimeLiveStream(requireContext(), false);
    }

    @Override
    public void onResume() {
        super.onResume();
        RelativeLayout mAdview = mViewRoot.findViewById(R.id.adView);
        new AdsUtil(getContext(), mAdview).loadBanner();
    }

    private void setDecs(int i) {
        if (i == 0) tvDecs.setText(getString(R.string.guideline_live_step_1));
        if (i == 1) tvDecs.setText(getString(R.string.guideline_live_step_2));
        if (i == 2) tvDecs.setText(getString(R.string.guideline_live_step_3));
    }

    public List<PhotoModel> getListPhoto(){
        List<PhotoModel> mListPhoto;
        mListPhoto = new ArrayList<>();
        mListPhoto.add(new PhotoModel(R.drawable.how_to_livestream_1));
        mListPhoto.add(new PhotoModel(R.drawable.how_to_livestream_2));
        mListPhoto.add(new PhotoModel(R.drawable.how_to_livestream_3));
        return mListPhoto;
    }
}