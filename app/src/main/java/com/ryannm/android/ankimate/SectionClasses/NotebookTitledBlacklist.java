package com.ryannm.android.ankimate.SectionClasses;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.ryannm.android.ankimate.BlackListLab;
import com.ryannm.android.ankimate.Dao.BlackList;
import com.ryannm.android.ankimate.R;

import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

public class NotebookTitledBlacklist extends StatelessSection {
    String mNotebookTitle;
    List<BlackList> mBlackLists;
    Context mContext;

    public NotebookTitledBlacklist(String notebookTitle, List<BlackList> blackLists, Context context) {
        super(R.xml.section, R.xml.blacklist_individual_layout);

        mNotebookTitle = notebookTitle;
        mBlackLists = blackLists;
        mContext = context.getApplicationContext();
    }

    @Override
    public int getContentItemsTotal() {
        return mBlackLists.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new BlackListHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new NotebookTitleHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        ((NotebookTitleHolder)holder).bindHolder(mNotebookTitle);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((BlackListHolder) holder).bindHolder(mBlackLists.get(position));
    }

    private class BlackListHolder extends RecyclerView.ViewHolder {
        private BlackList mBlackList;
        private TextView mTitleView;
        private EditText mCardsEditView;

        public BlackListHolder(View itemView) {
            super(itemView);
            mTitleView = (TextView) itemView.findViewById(R.id.note_title);
            mCardsEditView = (EditText) itemView.findViewById(R.id.notes_added);
        }

        public void bindHolder(BlackList blackList) {
            mBlackList = blackList;
            mTitleView.setText(blackList.getNoteTitle());
            mCardsEditView.setText(Integer.toString(blackList.getCardsAdded()));

            mCardsEditView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (TextUtils.isEmpty(s.toString().trim())) {
                        s = "0";
                    }

                    if ( TextUtils.isDigitsOnly(s) ) {
                        Integer i = Integer.parseInt(s.toString());
                        if (i<0) {
                            mCardsEditView.setError(mContext.getResources().getString(R.string.invalid_input));
                        } else {
                            mBlackList.setCardsAdded(i);
                            BlackListLab.get(mContext).insertOrReplaceBlacklist(mBlackList);
                        }

                    } else {
                        mCardsEditView.setError(mContext.getResources().getString(R.string.invalid_input));
                    }

                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private class NotebookTitleHolder extends RecyclerView.ViewHolder {
        private TextView mNotebookTitleTextView;

        public NotebookTitleHolder(View itemView) {
            super(itemView);

            mNotebookTitleTextView = (TextView) itemView.findViewById(R.id.section_text);
        }

        public void bindHolder(String notebookTitle) {
            mNotebookTitleTextView.setText(notebookTitle);
        }
    }


}
