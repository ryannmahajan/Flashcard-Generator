package com.ryannm.android.ankimate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.ichi2.anki.api.AddContentApi;
import com.ichi2.anki.FlashCardsContract.Note;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION;

public class AnkiHelper {
    private static AnkiHelper sAnkiHelper;
    private final AddContentApi mApi;
    private Map<Long, String> mDeckMap;
    private Map<Long, String> mModelMap;
    private ArrayList<String> mTags;
    private static final String TAG = "AnkiHelper";
    private Context mContext;
    public static final long DEFAULT_DECK_ID = AddContentApi.DEFAULT_DECK_ID;

    public static AnkiHelper get(Context context){
            if (sAnkiHelper == null) {
                sAnkiHelper = new AnkiHelper(context); //Hence calling the private constructor that no other class can access
            }
            return sAnkiHelper;
    }

    public static void askForPermission(Activity callbackActivity, int callbackCode) {
        ActivityCompat.requestPermissions(callbackActivity, new String[]{READ_WRITE_PERMISSION}, callbackCode);
    }

    public static boolean shouldRequestPermission(Context c) {
         if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return false;
            }
            return ContextCompat.checkSelfPermission(c, READ_WRITE_PERMISSION) != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isAnkiInstalled(Context c) {
        PackageManager pm = c.getPackageManager();
        boolean installed ;
        try {
            pm.getPackageInfo("com.ichi2.anki", PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    private AnkiHelper(Context context) {
        mContext = context.getApplicationContext();
        mApi = new AddContentApi(mContext);
        mDeckMap= mApi.getDeckList();
        mModelMap = mApi.getModelList();
        mTags = fetchTags();
    }

    public void send(String front, String back, Long deckId) {
        if (isApiAvailable()) {


            if (deckId==null) {
                deckId = mApi.addNewDeck("Sent from demo");
            }
            long modelId = mApi.addNewBasicModel("Basic model");
            mApi.addNote(modelId, deckId, new String[]{front, back}, null);
        } else {
            Toast.makeText(mContext, "Couldn't access Api, sharing instead", Toast.LENGTH_SHORT).show();
            Intent shareIntent = ShareCompat.IntentBuilder.from((Activity) mContext)
                    .setType("text/plain")
                    .setSubject(front)
                    .setText(back)
                    .getIntent();
            if (shareIntent.resolveActivity(mContext.getPackageManager()) != null) {
                mContext.startActivity(shareIntent);
            }
        }
    }

    boolean isApiAvailable() {
        return AddContentApi.getAnkiDroidPackageName(mContext) != null;
    }

    public Collection<String> getDeckNames() {
        return mDeckMap.values();
    }

    public Collection<String> getModelNames() {
        return mModelMap.values();
    }

    public ArrayList<String> getTags() { return mTags; }

    public long getDeckId(String deckName) {
        Long requiredKey = null;
        for (long key: mDeckMap.keySet()) {

            if ( mDeckMap.get(key).equals(deckName)) {
                requiredKey = key;
                break;
            }
        }
        return requiredKey;
    }

    @Nullable
    public Long getModelId(String modelName) {
        Long requiredKey = null;
        for (long key: mModelMap.keySet()) {

            if ( mModelMap.get(key).equals(modelName)) {
                requiredKey = key;
                break;
            }
        }
        return requiredKey;
    }

    private ArrayList<String> fetchTags() {
        Cursor allTagsCursor = mContext.getContentResolver().query(Note.CONTENT_URI, new String[]{Note.TAGS}, null,null,null);

        if (allTagsCursor==null) return null;

        Set<String> tags = new HashSet<>();
        try {
            while (allTagsCursor.moveToNext()) {
                int tagsIndex = allTagsCursor.getColumnIndexOrThrow(Note.TAGS);
                Collections.addAll(tags, splitTags(allTagsCursor.getString(tagsIndex)));
            }
        } finally {
            allTagsCursor.close();
        }
        ArrayList<String> tagList = new ArrayList<>(tags.size());
        tagList.addAll(tags);
        return tagList;
    }

    // TODO : This isn't safe in the long run. Ask the dev to make Utils.class & this method public
    private static String[] splitTags(String tags) {
        if (tags == null) {
            return null;
        }
        return tags.trim().split("\\s+");
    }

    public String[] getFieldList (long modelId) {
        return mApi.getFieldList(modelId);
    }

    public int getDeckPosition(Long deckId) {
        if (deckId==null) { return 0; }
        ArrayList<Long> ids = new ArrayList<>();
        ids.addAll(mDeckMap.keySet());

        if (!ids.isEmpty()) {
            for (int i = 0; i < ids.size(); i++) {
                if ( ids.get(i).equals(deckId) ) { return i ;}
            }
        } else {
            Log.e(TAG, " Number of deck Ids available is zero");
            return 0;
        }
        return 0;
    }

    public int getModelPosition(Long modeld) {
        if (modeld==null) { return 0; }
        ArrayList<Long> ids = new ArrayList<>();
        ids.addAll(mModelMap.keySet());

        if (!ids.isEmpty()) {
            for (int i = 0; i < ids.size(); i++) {
                if ( ids.get(i).equals(modeld) ) { return i ;}
            }
        } else {
            Log.e(TAG, " Number of model Ids available is zero");
            return 0;
        }
        return 0;
    }


    public Set<Long> getDeckIds() {
        return mDeckMap.keySet();
    }

    public Set<Long> getModelIds() {
        return mModelMap.keySet();
    }

    public void save(Long deckId, Long modelId, List<String[]> fieldLists, List<Set<String>> tagLists) {
        if (isApiAvailable()) {
            if (fieldLists != null) {
                mApi.addNotes(modelId, deckId, fieldLists, tagLists);
            }
        }
    }

    void saveSingle(Long deckId, Long modelId, String[] fieldList, Set<String> tagList) {
        if (isApiAvailable()) {
            if (fieldList != null) {
                mApi.addNote(modelId, deckId, fieldList, tagList);
            }
        }
    }

    void saveSingleWithBasicModel(Long deckId, String[] fieldList, Set<String> tagList) {
        if (isApiAvailable()) {
            if (fieldList != null) {
                Long modelId = getModelId("Basic");
                if (modelId==null) modelId = mApi.addNewBasicModel("Basic");

                String[] fields = new String[2];
                System.arraycopy(fieldList, 0, fields, 0, 2);

                mApi.addNote(modelId, deckId, fields, tagList);
            }
        }
    }

    boolean isDeckAvaiable() {
        return !(mDeckMap == null || mDeckMap.isEmpty());
    }

    boolean isModelAvaiable() {
        return !(mModelMap == null || mModelMap.isEmpty());
    }
}
