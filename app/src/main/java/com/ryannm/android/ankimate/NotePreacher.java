package com.ryannm.android.ankimate;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.evernote.edam.notestore.NoteMetadata;
import com.ryannm.android.ankimate.Dao.BlackList;
import com.ryannm.android.ankimate.Dao.Configuration;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class NotePreacher {

    private NoteMetadata mNote;
    private BlackList mBlackList;
    private Set<String> mBlacklistSet;
    private Document mOriginalNoteContentDoc;
    private Document mNoteContentDoc;
    private Context mContext;
    private boolean mAllFieldsBlacklisted;

    public NotePreacher(NoteMetadata note, Context context) {
        mNote = note;
        mContext = context.getApplicationContext();
    }

    public NotePreacher(NoteMetadata note, BlackList blackList, Context context) {
        mNote = note;
        mBlackList = blackList;
        mContext = context.getApplicationContext();
    }

    public boolean ifConfigurationInNote (Configuration configuration) {
        List<String> storedTags = ConfigurationLab.split(QueryPreferences.getEvernoteTags(mContext));
        List<String> configurationTagGuids = new ArrayList<>();

        List<String> tagNames = ConfigurationLab.split(configuration.getTagsToFetch());

        if (tagNames != null) {
            for (String tagName : tagNames) {

                if (storedTags.contains(tagName)) {
                    configurationTagGuids.add(EverHelper.getTagGuid(tagName));
                }
            }
        }
        return  (configurationTagGuids.isEmpty() || mNote.getTagGuids().containsAll(configurationTagGuids)) ;
    }

    private Set<String> getBlackListFields (int size) {
        Set<String> set = new HashSet<>();
        try {

            if (size == 0) return set;
            List<String> firstKeywordsList = ConfigurationLab.getFirstKeywordsList(ConfigurationLab.get(mContext).getConfigurations());
            Elements blocks = mNoteContentDoc.select("div:not(:has(div)):matches((?i)^(\\?\\s|\\?)?(" + TextUtils.join("|", firstKeywordsList) + ")\\s?:)"); // TODO : fix this one to solve notesToAdd sync
            //if (firstKeywordsList.get(0).matches("(?i)^(\\?\\s|\\?)?(W|Q|C)\\s?:"))
            if (size < blocks.size()) {
                for (int i = 0; i < size; i++) {
                    set.add(blocks.get(i).text()/*.split("(?i)^(" + TextUtils.join("|", firstKeywordsList) + ")\\s?:")[1]*/);
                }
            } else {
                if (blocks.size() < size) {
                    size = blocks.size();
                    mBlackList.setCardsAdded(size);
                    BlackListLab.get(mContext).insertOrReplaceBlacklist(mBlackList);
                }
                setAllFieldsBlacklisted(true);
            }
        } catch (Exception e) {
            //Toast.makeText(mContext, blocks.get(i).text(), Toast.LENGTH_SHORT).show();
            Log.e("Personal", "Errorrtcty", e);
        }
        return set;
    }

    public Callable<Set<String>> gatherBlackListSet() {
         return new Callable<Set<String>>() {
            @Override
            public Set<String> call() throws Exception {
                return getBlackListFields(mBlackList.getCardsAdded());
            }
        };
    }

    public Callable<List<String[]>> getFields(final Configuration configuration) {
        return new Callable<List<String[]>>() {
            @Override
            public List<String[]> call() throws Exception {
                return ENMLhelper.returnFieldLists(mNoteContentDoc, configuration, ":", mBlacklistSet);
            }
        };
    }

    public Callable<List<List<String>>> getNotesToAdd(final Configuration configuration) {
        return new Callable<List<List<String>>>() {
            @Override
            public List<List<String>> call() throws Exception {
                return ENMLhelper.returnNotesToAdd(mNoteContentDoc, configuration, "?", ":", mBlacklistSet);
            }
        };
    }

    public Set<String> getTags(Configuration configuration) {

        Set<String> tagNames = new HashSet<String>();
        if (configuration.getTagsToSave() != null) {

            if (configuration.getTagsToSave().equals(ConfigurationLab.SAVE_TAGS_FROM_EVERNOTE)) {
                List<String> tagGuids = mNote.getTagGuids();

                for (String tagGuid : tagGuids) {
                    tagNames.add(EverHelper.getTagName(tagGuid));
                }

                tagNames.remove("anki");
                tagNames.remove("Anki");
                tagNames.remove("ANKI");

            } else {
                tagNames = new HashSet<>(ConfigurationLab.split(configuration.getTagsToSave()));
            }
        }

        return tagNames;
    }

    public NoteMetadata getNote() {
        return mNote;
    }

    public void setNote(NoteMetadata note) {
        this.mNote = note;
    }

    public BlackList getBlackList() {
        return mBlackList;
    }

    public void setBlackList(BlackList blackList) {
        this.mBlackList = blackList;
    }

    public Document getNoteContentDoc() {
        return mNoteContentDoc;
    }

    public void setNoteContentDoc(Document noteContentDoc) {
        mOriginalNoteContentDoc = noteContentDoc;
        //mOriginalNoteContentDoc.outputSettings().prettyPrint(false);
        this.mNoteContentDoc = noteContentDoc.clone();
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public Set<String> getBlacklistSet() {
        return mBlacklistSet;
    }

    public void setBlacklistSet(Set<String> blacklistSet) {
        this.mBlacklistSet = blacklistSet;
    }

    public boolean isAllFieldsBlacklisted() {
        return mAllFieldsBlacklisted;
    }

    public void setAllFieldsBlacklisted(boolean allFieldsBlacklisted) {
        this.mAllFieldsBlacklisted = allFieldsBlacklisted;
    }

    // Deletes note content before & including bookmark element.
    public Runnable deleteNoteContentBeforeText(final String text) {
        return new Runnable() {
            @Override
            public void run() {
                Element bookmark = getNoteContentDoc().getElementsContainingOwnText(text).last();
                if (bookmark==null) return;
                Elements parents = bookmark.parents();
                Elements toRemove = getNoteContentDoc().select("en-note");
                if (getNoteContentDoc().body()!=null) toRemove.add(getNoteContentDoc().body()); // Exclude body and en-note from parents
                parents.removeAll(toRemove);

                List<Node> siblingsOfBookmark = parents.first().childNodes();
                boolean bookmarkNotDeleted = true;
            //    int j= 0;
                do {
                    Node node = siblingsOfBookmark.get(0);
                    node.remove(); // Remove bookmark & its siblings
               //     j++;
                    if (node==bookmark) bookmarkNotDeleted = false;
                } while (bookmarkNotDeleted);

                for (Element parent : parents) {
                    List<Node> siblings = parent.siblingNodes();
                    final int siblingIndex = parent.siblingIndex(); // We store it here cos we are removing preceding elements in coming lines

                    for (int i = 0; i < siblingIndex; i++) {
                        if (siblings.get(i).equals(parent)) continue;
                        siblings.get(i).remove();
                    }
                }
            }
        };

    }

    public void destroyTextFromOriginal(final String text, final boolean destroyLastOnly) {

        Elements searchResults = mOriginalNoteContentDoc.getElementsContainingOwnText(text);
        if (searchResults==null || searchResults.isEmpty()) return;
        Elements toDestroy = new Elements();
        if (destroyLastOnly) toDestroy.add(searchResults.last());
        else toDestroy = searchResults;

        for (Element element : toDestroy) {
            if (text.equals(App.returnTrimmedString(element.text())))
                element.remove();
            else
                element.text((String) TextUtils.replace(element.text(), new String[] {text}, new String[] {""}));
        }
    }

    public Document writeBookmarkToOriginal(final List<String> lastFieldNominees) {
        for (int i = 0; i < lastFieldNominees.size(); i++) lastFieldNominees.set(i,App.escapeSpecialChars(lastFieldNominees.get(i)));

        Element lastField = mOriginalNoteContentDoc.select("div:matches("+TextUtils.join("|", lastFieldNominees) +")").last();

        Element bookmark = new Element(Tag.valueOf("span"), "");
        bookmark.attr("style", mContext.getString(R.string.bookmark_style));
        bookmark.text(mContext.getString(R.string.bookmark));
        lastField.appendChild(bookmark);
        return mOriginalNoteContentDoc;

    }
}
