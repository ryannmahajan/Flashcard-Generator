package com.ryannm.android.ankimate;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.ryannm.android.ankimate.Dao.Configuration;
import com.ryannm.android.ankimate.Dao.ConfigurationDao;
import com.ryannm.android.ankimate.Dao.DaoMaster;
import com.ryannm.android.ankimate.Dao.DaoSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class ConfigurationLab {

    public static final String SAVE_TAGS_FROM_EVERNOTE = "blah.SAVE_TAGS_FROM_EVERNOTE.blah";
    public static final String EMPTY_FIELD_KEYWORD = "blah.soEmpty.blah";

    private static final String DELIMITER = "%,%";
    private static ConfigurationLab sConfigurationLab;
    private ConfigurationDao mConfigurationDao;
    private Context mContext;

    private ConfigurationLab(Context context) {
        mContext = context.getApplicationContext();
        updateSession();
    }

    public void updateSession() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "configurations-db", null);
        DaoSession session = new DaoMaster(helper.getWritableDatabase()).newSession();
        mConfigurationDao = session.getConfigurationDao();
    }

    public static ConfigurationLab get(Context context) {
        if (sConfigurationLab==null) {
            sConfigurationLab = new ConfigurationLab(context);
        }
        return sConfigurationLab;
    }

    public static List<String> split(String string) {
        ArrayList<String> stringList = new ArrayList<>();
        if (string==null) {
            return null;
        }
        Collections.addAll(stringList, TextUtils.split(string, DELIMITER));
        return stringList;
    }

    public static String join(List<String> strings) {
        if (strings==null) { return null; }
        return TextUtils.join(DELIMITER, strings);
    }

    public long insertOrReplaceConfiguration(Configuration configuration) {
       return mConfigurationDao.insertOrReplace(configuration);
    }

    public long insertConfiguration(Configuration configuration) {
        return mConfigurationDao.insert(configuration);
    }


    public List<Configuration> getConfigurations() {
       return mConfigurationDao.loadAll();
    }

    public Configuration getConfiguration(long key) {
        return mConfigurationDao.load(key);
    }

    public Configuration getConfigurationFromIndex(long index) {
        return mConfigurationDao.loadByRowId(index);
    }

    public void deleteConfiguration(Configuration configuration) {
        mConfigurationDao.delete(configuration);
    }

    public void showConfigurationsToast(Context context) {
        List<Configuration> loadAll = mConfigurationDao.loadAll();
        for (int i = 0; i < loadAll.size(); i++) {
            Configuration configuration = loadAll.get(i);
            Toast.makeText(context, "" + i + "-->" + configuration.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    public static Configuration unNullifyFieldKeywords(Configuration configuration) {
        List<String> fieldKeywords = ConfigurationLab.split(configuration.getFieldKeywords());
        for (int i=0; i < fieldKeywords.size(); i++) {
            if (fieldKeywords.get(i)==null) {
                fieldKeywords.set(i,EMPTY_FIELD_KEYWORD);
            } else if (fieldKeywords.get(i).trim().isEmpty()) {
                fieldKeywords.set(i,EMPTY_FIELD_KEYWORD);
            } else if (fieldKeywords.get(i).equals("null")) {
                fieldKeywords.set(i, EMPTY_FIELD_KEYWORD);
            }
        }
        configuration.setFieldKeywords(ConfigurationLab.join(fieldKeywords));
        return configuration;
    }

    public static boolean isFieldKeywordsValid(String fieldKeywords) {
        if (fieldKeywords==null) { return false; }

        List<String> keywords = ConfigurationLab.split(fieldKeywords);
        for (String keyword: keywords) {
            if (keyword==null) {return false; }
            else if (keyword.trim().isEmpty() || keyword.equals(EMPTY_FIELD_KEYWORD)) {
                return false;
            }

        }
        return true;
    }

    public static String[] StringArrayFromListKeywords (List<String> keywordsList, int size) {
        String[] stringArray = new String[ size ];

        for (int i=0 ; i < size ; i++) {
            if (keywordsList.get(i)==null) {
                stringArray[i] = EMPTY_FIELD_KEYWORD;
            } else {
                stringArray[i] = EMPTY_FIELD_KEYWORD;
            }
        }
        return stringArray;
    }

    public void createDeckNotification(Configuration configuration) {
        Intent i = ConfigurationActivity.newIntent(mContext, configuration.getId(), ConfigurationActivity.EDIT_FRAGMENT);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, i, 0);
        Resources resources = mContext.getResources();
        Notification notification = new
                NotificationCompat.Builder(mContext)
            //    .setTicker(String.format(resources.getString(R.string.invalid_deck),configuration.getName()))
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle(resources.getString(R.string.invalid_input))
                .setContentText(String.format(resources.getString(R.string.invalid_deck),configuration.getName()))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        NotificationManagerCompat manager = NotificationManagerCompat.from(mContext);
        manager.notify(0, notification);
    }

    // Use it to store date in preferences
    public static String getDatePreferencesString() {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        String string = (String) DateFormat.format("yyyyMMddTHHmmss", calendar);
      //  String timeString = (String) DateFormat.format("HHmmss", calendar);
        return string + "Z";
    }

    public static String getDatePreferencesString(Long toSaveMilliseconds) {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(toSaveMilliseconds);
        String string = (String) DateFormat.format("yyyyMMddTHHmmss", calendar);
       // String timeString = (String) DateFormat.format("HHmmss", calendar);
        return string + "Z";
    }

    public void createModelNotification(Configuration configuration) {
        Intent i = ConfigurationActivity.newIntent(mContext, configuration.getId(), ConfigurationActivity.EDIT_FRAGMENT);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, i, 0);
        Resources resources = mContext.getResources();
        Notification notification = new
                NotificationCompat.Builder(mContext)
                //    .setTicker(String.format(resources.getString(R.string.invalid_deck),configuration.getName()))
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle(resources.getString(R.string.invalid_input))
                .setContentText(String.format(resources.getString(R.string.invalid_model),configuration.getName()))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        NotificationManagerCompat manager = NotificationManagerCompat.from(mContext);
        manager.notify(0, notification);
    }

    public void createFieldsNotification(Configuration configuration) {
        Intent i = ConfigurationActivity.newIntent(mContext, configuration.getId(), ConfigurationActivity.EDIT_FRAGMENT);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, i, 0);
        Resources resources = mContext.getResources();
        Notification notification = new
                NotificationCompat.Builder(mContext)
                //    .setTicker(String.format(resources.getString(R.string.invalid_deck),configuration.getName()))
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle(resources.getString(R.string.invalid_input))
                .setContentText(String.format(resources.getString(R.string.invalid_fields),configuration.getName()))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .build();
        NotificationManagerCompat manager = NotificationManagerCompat.from(mContext); // todo : Here's how to create a notif
        manager.notify(0, notification);
    }

    public static List<String> getFirstKeywordsList(List<Configuration> configurations) {
        List<String> fieldKeywords = new ArrayList<>(configurations.size());

        for (Configuration configuration:configurations) {
            fieldKeywords.add(split(configuration.getFieldKeywords()).get(0));
        }

        return fieldKeywords;
    }

    public Configuration getCloneConfiguration(Configuration mConfiguration) {
        Configuration configuration = new Configuration();
        configuration.setName( mConfiguration.getName() + " " + mContext.getResources().getString(R.string.copy));
        configuration.setTagsToFetch(mConfiguration.getTagsToFetch());
        configuration.setTagsToSave(mConfiguration.getTagsToSave());
        configuration.setFieldKeywords(mConfiguration.getFieldKeywords());
        configuration.setFields(mConfiguration.getFields());
        configuration.setDeckId(mConfiguration.getDeckId());
        configuration.setModelId(mConfiguration.getModelId());
        //insertOrReplaceConfiguration(configuration);
        return configuration;
    }
}
