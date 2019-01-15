package com.ryannm.android.ankimate;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.thrift.TException;
import com.ryannm.android.ankimate.Dao.BlackList;
import com.ryannm.android.ankimate.SectionClasses.NotebookTitledBlacklist;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class NotesAddedFragment extends Fragment {
    private RecyclerView mCardsAddedRecyclerView;
    private SectionedRecyclerViewAdapter sectionAdapter;
    private List<BlackList> mBlacklists;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBlacklists = BlackListLab.get(getActivity()).getBlackLists();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_cards_added, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_button :
                try {
                    if (EverHelper.get(getActivity()).getSyncState() > QueryPreferences.getLastSyncedState(getActivity())) {
                        long minutesSinceLastSync = EverHelper.differenceInMinutesFromMilli(QueryPreferences.getInternalLastSyncMilliseconds(getActivity()), new GregorianCalendar(TimeZone.getTimeZone("GMT + 0")).getTimeInMillis());
                        if (minutesSinceLastSync > 15) {
                                List<BlackList> newBlackLists = EverHelper.get(getActivity()).refreshAllAnkiTaggedNotes(); // Todo : Have a progress dialog for both refresh buttons
                                mBlacklists.addAll(newBlackLists);
                                updateRecyclerView();
                        } else {
                            Toast.makeText(getActivity(), String.format(getResources().getString(R.string.evernote_restriction_15), Long.toString(15-minutesSinceLastSync)), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), R.string.no_new_anki_note_found, Toast.LENGTH_SHORT).show();
                    }
                } catch (ExecutionException | InterruptedException | EDAMUserException | TException | EDAMSystemException e) {
                    e.printStackTrace();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onResume() {
        super.onResume();
      //  updateUI();
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        if (mBlacklists==null || mBlacklists.isEmpty()) {
            return inflater.inflate(R.layout.empty_layout_with_message, parent, false);
        } else {
            View v = inflater.inflate(R.layout.fragment_cards_added, parent, false);

            //Your RecyclerView
            mCardsAddedRecyclerView = (RecyclerView) v.findViewById(R.id.cards_added_recyclerview);
         //   mCardsAddedRecyclerView.setHasFixedSize(true);
            updateRecyclerView();

            getActivity().setTitle(R.string.added_cards);

            return v;
        }
    }

    private void updateRecyclerView() {
        mCardsAddedRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //Your RecyclerView.Adapter
        sectionAdapter = new SectionedRecyclerViewAdapter();

        HashMap<String, List<BlackList>> map = sectionSort(reverseList(mBlacklists)); // Reverse so that last updated on top

        //Sections
        for (String notebookTitle : map.keySet()) {
            sectionAdapter.addSection(new NotebookTitledBlacklist(notebookTitle, map.get(notebookTitle), getActivity()));
        }

        //Apply this adapter to the RecyclerView
        mCardsAddedRecyclerView.setAdapter(sectionAdapter);
    }

    private HashMap<String, List<BlackList>> sectionSort(List<BlackList> mBlackLists) {
        HashMap<String, List<BlackList>> hashMap = new HashMap<>();

        while(!mBlackLists.isEmpty()) {
            List<BlackList> forThisNotebook = new ArrayList<>();
            String notebookTitle = null;
            for (int i=0; i < mBlackLists.size(); i++) {
                BlackList blackList = mBlackLists.get(i);

                if (forThisNotebook.isEmpty()) {
                    notebookTitle = blackList.getNotebook();
                    forThisNotebook.add(blackList);
                    mBlackLists.remove(i);
                } else {
                    if (blackList.getNotebook().equals(notebookTitle)) {
                        forThisNotebook.add(blackList);
                        mBlackLists.remove(i);
                    }
                }
            }
            List<BlackList> previous = hashMap.get(notebookTitle);
            if (previous!=null) forThisNotebook.addAll(previous);
            hashMap.put(notebookTitle, cloneMe(forThisNotebook));
        }

        return hashMap;
    }

    private List<BlackList> reverseList(List<BlackList> blackLists) {
        List<BlackList> reversed = new ArrayList<>(blackLists.size());
        for (int i=0; i < blackLists.size(); i++) {
            reversed.add(blackLists.get(blackLists.size() - 1 - i));
        }
        return reversed;
    }

    private List<BlackList> cloneMe(List<BlackList> blackLists) {
        List<BlackList> clone = new ArrayList<>(blackLists.size());
        for (BlackList blackList : blackLists) {
            clone.add(blackList);
        }
        return clone;
    }

}

