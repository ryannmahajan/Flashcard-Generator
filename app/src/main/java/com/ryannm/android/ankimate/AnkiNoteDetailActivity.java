package com.ryannm.android.ankimate;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.ryannm.android.ankimate.Dao.AnkiNote;

public class AnkiNoteDetailActivity extends SingleFragmentActivity implements AnkiNoteDetailFragment.Callbacks{

    private static final String EXTRA_ANKINOTE_ID = "com.ryannm.android.autoanki.extra_ankinote_id";

    public static Intent newIntent(Context context, Long ankiNoteId) {
        Intent i = new Intent(context, AnkiNoteDetailActivity.class);
        i.putExtra(EXTRA_ANKINOTE_ID, ankiNoteId);
        return i;
    }

    @Override
    protected Fragment createFragment() {
        return AnkiNoteDetailFragment.getInstance(getIntent().getLongExtra(EXTRA_ANKINOTE_ID, 0));
    }

    @Override
    public void nextNote(AnkiNote ankiNote) {
        super.onNavigateUp();
        AnkiNoteLab.get(this).deleteAnkiNote(ankiNote.getId());
    }
}
