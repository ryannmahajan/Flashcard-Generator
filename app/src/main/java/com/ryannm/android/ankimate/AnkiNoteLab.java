package com.ryannm.android.ankimate;

import android.content.Context;

import com.ryannm.android.ankimate.Dao.AnkiNote;
import com.ryannm.android.ankimate.Dao.AnkiNoteDao;
import com.ryannm.android.ankimate.Dao.DaoMaster;
import com.ryannm.android.ankimate.Dao.DaoSession;

import java.util.ArrayList;
import java.util.List;

public class AnkiNoteLab {
    private AnkiNoteDao mAnkiNoteDao;
    private static AnkiNoteLab sAnkiNoteLab;
    private Context mContext;

    public static AnkiNoteLab get(Context context) {
        if (sAnkiNoteLab==null) sAnkiNoteLab = new AnkiNoteLab(context);
        return sAnkiNoteLab;
    }

    private AnkiNoteLab(Context context) {
        mContext = context.getApplicationContext();
        updateSession();
    }

    public void updateSession() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "anki-notes-db", null);
        DaoSession session = new DaoMaster(helper.getWritableDatabase()).newSession();
        mAnkiNoteDao = session.getAnkiNoteDao();
    }

    public List<AnkiNote> getAnkiNotes() {
        return mAnkiNoteDao.loadAll();
    }

    public AnkiNote getAnkiNoteById(Long id) {
        return mAnkiNoteDao.load(id);
    }

    public void insertOrReplaceAnkiNote(AnkiNote ankiNote) {
        mAnkiNoteDao.insertOrReplace(ankiNote);
    }

    public void insertOrReplaceAnkiNotes(Iterable <AnkiNote> ankiNotes) {
        mAnkiNoteDao.insertOrReplaceInTx(ankiNotes, false);
    }

    public void deleteAnkiNote(Long id) {
        mAnkiNoteDao.deleteByKey(id);
    }

    public List<AnkiNote> getAnkiNotesByNotebookTitle(String notebookTitle) {
        List<AnkiNote> result = new ArrayList<>();

        for (AnkiNote ankiNote : mAnkiNoteDao.loadAll()) {
            if (ankiNote.getNotebookTitle().equals(notebookTitle)) result.add(ankiNote);
        }

        return result;
    }

}
