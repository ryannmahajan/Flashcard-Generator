package com.ryannm.android.ankimate.SectionClasses;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.ryannm.android.ankimate.ConfigurationLab;
import com.ryannm.android.ankimate.Dao.AnkiNote;
import com.ryannm.android.ankimate.NotesToAddListActivity;
import com.ryannm.android.ankimate.R;

import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

public class EverNoteTitledAnkiNote extends StatelessSection{
    private String mNoteTitle;
    private List<AnkiNote> mAnkiNotes; // Remember not to use this as an item. Only as a result
    private NotesToAddListActivity.Callbacks mCalledOnAnkiNoteSelect;

   // private HashMap<Configuration, List<AnkiNote>> mUniqueConfigurations;

    public EverNoteTitledAnkiNote(String noteTitle, List<AnkiNote> ankiNotes, NotesToAddListActivity.Callbacks toBeCalledInItemOnClick) {
        super(R.xml.section, android.R.layout.simple_expandable_list_item_1);

        mNoteTitle = noteTitle;
        mAnkiNotes = ankiNotes;
        mCalledOnAnkiNoteSelect = toBeCalledInItemOnClick;
    }

    @Override
    public int getContentItemsTotal() {
        return mAnkiNotes.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new FirstFieldHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((FirstFieldHolder) holder).bindHolder(position);
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new NoteTitleHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        ((NoteTitleHolder)holder).bindHolder(mNoteTitle);
    }

    private class NoteTitleHolder extends RecyclerView.ViewHolder{
        private TextView mNoteTitleTextView;

        public NoteTitleHolder(View itemView) {
            super(itemView);

            mNoteTitleTextView = (TextView) itemView.findViewById(R.id.section_text);
        }

        public void bindHolder(String noteTitle) {
            mNoteTitleTextView.setText(noteTitle);
        }
    }

    public class FirstFieldHolder extends RecyclerView.ViewHolder{
        private TextView mFirstFieldTextView;

        public FirstFieldHolder(View itemView) {
            super(itemView);

            mFirstFieldTextView = (TextView) itemView.findViewById(android.R.id.text1);
        }

        public void bindHolder(final int position) {

            // Setting first field of AnkiNote as the identifier
            mFirstFieldTextView.setText(ConfigurationLab.split(mAnkiNotes.get(position).getFields()).get(0));
            mFirstFieldTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCalledOnAnkiNoteSelect.onAnkiNoteSelected(mAnkiNotes.get(position).getId());
                }
            });
        }
    }

    /*private HashMap<Configuration, List<AnkiNote>> generateUniqueConfigurationsHashmap(List<AnkiNote> ankiNotes) {
        HashMap<Configuration, List<AnkiNote>> map = new HashMap<>();
        ArrayList<AnkiNote> forThisLoop = new ArrayList<>();

        for (AnkiNote ankiNote: ankiNotes) {
            forThisLoop.add(ankiNote);
            Configuration configuration = ConfigurationLab.get(mContext).getConfiguration(ankiNote.getConfigurationId());
            if (map.get(configuration)!=null) forThisLoop.addAll(map.get(configuration));
            map.put(configuration, (List<AnkiNote>) forThisLoop.clone());
            forThisLoop.clear();
        }
        return map;
    }*/
}
