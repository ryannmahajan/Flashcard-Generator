package com.ryannm.android.ankimate;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CustomFragment extends Fragment {
    private static final String LAYOUT_RES_ID = "LayoutResId";
    @LayoutRes int mLayoutId;

    public static CustomFragment newInstance(@LayoutRes int layoutId) {
        CustomFragment fragment = new CustomFragment();
        Bundle args = new Bundle();
        args.putInt(LAYOUT_RES_ID, layoutId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLayoutId = getArguments().getInt(LAYOUT_RES_ID);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(mLayoutId, container, false);
    }
}
