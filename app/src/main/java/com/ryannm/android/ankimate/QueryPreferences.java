package com.ryannm.android.ankimate;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class QueryPreferences {
    private static final String PREF_EVERNOTE_TAGS = "evernoteTags";
    private static final String PREF_LAST_SYNCED_DATE = "lastSyncedDate";
    private static final String PREF_LAST_SYNCED_STATE = "lastSyncedState";
    private static final String PREF_LAST_VERSION_NAME = "lastVersionName";
    private static final String PREF_INTERNAL_LAST_SYNC_MILLISECONDS = "lastSyncMilli";
    private static final String PREF_SAVED_NOTES_META = "savedNotesMeta";

    public static String getEvernoteTags(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_EVERNOTE_TAGS, null);
    }

    public static void setEvernoteTags(Context context, String tagsJoined) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_EVERNOTE_TAGS, tagsJoined)
                .apply();
    }

    public static String getLastSyncedDate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_LAST_SYNCED_DATE, null);
    }

    public static void setLastSyncedDate(Context context, String formattedDate) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_SYNCED_DATE, formattedDate)
                .apply();
    }

    public static int getLastSyncedState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_LAST_SYNCED_STATE, 0);
    }

    public static void setLastSyncedState(Context context, int syncState) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(PREF_LAST_SYNCED_STATE, syncState)
                .apply();
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.enable_service),true);
    }

    public static void setEnabled (Context context , boolean setOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.enable_service), setOn)
                .apply();
    }

    public static boolean isMediaAllowed(Context context) { // Todo : Handle this while sorting notes
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.enable_media_entry_title),false);
    }

    public static void setMediaAllowed (Context context , boolean setOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.enable_media_entry_title), setOn)
                .apply();
    }

    public static long getSyncInterval(Context context) {
        return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.sync_interval_title), "10800000"));
    }

    public static boolean isWifiOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.use_wifi_only_title),true);
    }

    public static void setLastVersionName(Context context, String name) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_VERSION_NAME, name)
                .apply();
    }

    public static String getLastVersionName(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_LAST_VERSION_NAME, null);
    }

    public static void setInternalLastSyncMilliseconds(Context context, Long val) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(PREF_INTERNAL_LAST_SYNC_MILLISECONDS, val)
                .apply();
    }

    public static long getInternalLastSyncMilliseconds(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(PREF_INTERNAL_LAST_SYNC_MILLISECONDS, 0);
    }

    public static void setSavedNotesMeta(Context context,Set<String> val) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(PREF_SAVED_NOTES_META, val)
                .apply();
    }

    public static Set<String> getSavedNotesMeta(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet(PREF_SAVED_NOTES_META, new HashSet<String>());
    }







}
