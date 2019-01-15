package com.ryannm.android.ankimate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.ryannm.android.ankimate.Dao.AnkiNote;
import com.ryannm.android.ankimate.SectionClasses.EverNoteTitledAnkiNote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class NotesToAddListActivity extends AppCompatActivity implements AnkiNoteDetailFragment.Callbacks{
    private List<AnkiNote> mTotalAnkiNotes;
    private List<AnkiNote> mAnkiNotes; // These are set after a notebook is selected
    private List<CharSequence> mNotebookTitles;
    private boolean mTwoPane;
    private RecyclerView recyclerView;
    Spinner spinner;

  //  private List<AnkiNote> list;
    private HashMap<String, List<AnkiNote>> mUniqueNotesHash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTotalAnkiNotes = AnkiNoteLab.get(this).getAnkiNotes();
        if (!mTotalAnkiNotes.isEmpty()) {
            setContentView(R.layout.fragment_notes_to_add_main_list);
           /* toolbar = (Toolbar) findViewById(R.id.activity_toolbar);
            if (toolbar != null) {
                toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
                setSupportActionBar(toolbar);
            } */

        }
        else {
            showEmptyLayout();
        }

       /* Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getTitle()); See if your motives are still achieved.
        } */

        if (findViewById(R.id.notes_to_add_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        recyclerView = (RecyclerView) findViewById(R.id.notes_to_add_list);
        assert recyclerView != null;
        //updateRecyclerView(recyclerView);

     /*   if (toolbar!=null) { new App().returnDrawerBuilder(this, toolbar).build(); }
        else { So that only the listActivity has the hamburger icon*/
        new App().returnDrawerBuilder(this).build();
       // }

        setTitle(R.string.notes_to_add);
    }

    private void showEmptyLayout() {
        setContentView(new App().returnRelevantEmptyLayout(false));
        /*Toolbar toolbar;
        toolbar = (Toolbar) findViewById(R.id.activity_toolbar);
        if (toolbar != null) {
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
            setSupportActionBar(toolbar);
        } */

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mTotalAnkiNotes.isEmpty()) {
            getMenuInflater().inflate(R.menu.activity_notes_to_add, menu);

            MenuItem item = menu.findItem(R.id.spinner);
            spinner = (Spinner) MenuItemCompat.getActionView(item);
            spinner.setPopupBackgroundDrawable(getResources().getDrawable(R.drawable.menu_spinner_popup_background));
            if (mTotalAnkiNotes.isEmpty()) spinner.setVisibility(View.GONE);

            mNotebookTitles = generateNotebookTitleList(mTotalAnkiNotes);

            updateNotebookSpinner();
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void updateNotebookSpinner() {
        @SuppressWarnings("ResourceType") ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, R.xml.simple_spinner_item_text_color_white, mNotebookTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (mTotalAnkiNotes.isEmpty()) {
            spinner.setVisibility(View.GONE);
            mUniqueNotesHash = null;
        } else {
            spinner.setAdapter(adapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mAnkiNotes = AnkiNoteLab.get(NotesToAddListActivity.this).getAnkiNotesByNotebookTitle(parent.getItemAtPosition(position).toString());
                    mUniqueNotesHash = generateUniqueNoteTitleHashmap(mAnkiNotes);
                    updateRecyclerView(recyclerView);
                    if (mTwoPane && mAnkiNotes != null && !mAnkiNotes.isEmpty() && getSupportFragmentManager().findFragmentById(R.id.notes_to_add_detail_container) == null) showInDetailPane(mAnkiNotes.get(0).getId());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private List<CharSequence> generateNotebookTitleList(List<AnkiNote> ankiNotes) {
        List<CharSequence> notebookTitles = new ArrayList<>();
        for (AnkiNote ankiNote : ankiNotes) {
            if (!notebookTitles.contains(ankiNote.getNotebookTitle())) notebookTitles.add(ankiNote.getNotebookTitle());
        }
        return notebookTitles;
    }

    private HashMap<String, List<AnkiNote>> generateUniqueNoteTitleHashmap(List<AnkiNote> ankiNotes) {
        HashMap<String, List<AnkiNote>> result = new HashMap<>();
        ArrayList<AnkiNote> forThisLoop = new ArrayList<>();

        for (AnkiNote ankiNote: ankiNotes) {
            forThisLoop.add(ankiNote);
            if (result.get(ankiNote.getNoteTitle())!=null) forThisLoop.addAll(result.get(ankiNote.getNoteTitle()));
            result.put(ankiNote.getNoteTitle(), (List<AnkiNote>) forThisLoop.clone());
            forThisLoop.clear();
        }
        return result;
    }

    private void updateRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Section adapter
        SectionedRecyclerViewAdapter adapter = new SectionedRecyclerViewAdapter();

        adapter = updateSections(adapter);

        if (recyclerView.getAdapter()==null) {
            recyclerView.setAdapter(adapter);
        } else {
            recyclerView.setAdapter(adapter); // // TODO: When dealing with sections,don't use swapAdapter. Note this down
        }
    }

    private SectionedRecyclerViewAdapter updateSections(SectionedRecyclerViewAdapter adapter) {
        if ( mUniqueNotesHash==null || mUniqueNotesHash.isEmpty() ) {
            // No notes to add are there
            showEmptyLayout();
        } else {
            // Set up the expandable sections
            for (String noteTitle : mUniqueNotesHash.keySet()) {
                adapter.addSection(new EverNoteTitledAnkiNote(noteTitle, mUniqueNotesHash.get(noteTitle), new Callbacks() {
                    @Override
                    public void onAnkiNoteSelected(Long ankiNoteId) {
                        if (mTwoPane) {
                            showInDetailPane(ankiNoteId);
                        } else {
                            startActivity(AnkiNoteDetailActivity.newIntent(NotesToAddListActivity.this, ankiNoteId));
                        }
                    }
                }));
            }
        }
        return adapter;
    }

    private void showInDetailPane(Long ankiNoteId) {
        Fragment fragment = AnkiNoteDetailFragment.getInstance(ankiNoteId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.notes_to_add_detail_container, fragment)
                .commit();
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, NotesToAddListActivity.class);
    }

    @Override
    public void nextNote(AnkiNote ankiNote) {
        if (mTotalAnkiNotes.size()==1) {
            showEmptyLayout(); // Cos now they're empty
            new App().returnDrawerBuilder(this).build();
        } else {
            mAnkiNotes.remove(ankiNote);
            if (mAnkiNotes.size()==1) {

                mNotebookTitles.remove(ankiNote.getNotebookTitle());
                updateNotebookSpinner();
                //spinner.setSelection(0); See if this is required
            } else {
                mUniqueNotesHash = generateUniqueNoteTitleHashmap(mAnkiNotes);
                updateRecyclerView(recyclerView);
            }

        }
        AnkiNoteLab.get(this).deleteAnkiNote(ankiNote.getId());
    }

    // Interface to communicate b/w the section adapter and NotesToAddListActivity which AnkiNote was selected
    public interface Callbacks {
        void onAnkiNoteSelected(Long ankiNoteId);
    }

}
