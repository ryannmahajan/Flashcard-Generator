package com.ryannm.android.ankimate;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;

public class SendToAnkiService extends IntentService {
    private static final String DECK_KEY="deck";
    private static final String TYPE_KEY="type";
    private static final String TAGS_KEY="tags";
    private static final String TAG = "SendToAnkiService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public SendToAnkiService(String name) {
        super(name);
    }

    public SendToAnkiService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!AnkiHelper.isAnkiInstalled(getApplicationContext()))
            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.anki_not_found), Toast.LENGTH_SHORT).show();
        else if (AnkiHelper.shouldRequestPermission(getApplicationContext())) {
            Intent i = new Intent();
            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
            i.setData(uri);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Notification notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle(getApplicationContext().getString(R.string.permission_required))
                    .setContentText(getApplicationContext().getString(R.string.access_to_anki_database_not_granted_click_here))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0 , intent, 0))
                    .build();
            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
            manager.notify(0, notification);
        }

        else {
            String deckName = intent.getStringExtra(DECK_KEY), typeName = intent.getStringExtra(TYPE_KEY),tags= intent.getStringExtra(TAGS_KEY);
            Long deckId;
            if (tags==null) tags="";
            if (deckName==null) deckId = AnkiHelper.DEFAULT_DECK_ID;
            else deckId = AnkiHelper.get(getApplicationContext()).getDeckId(deckName);
            String dataWithoutScheme = intent.getDataString().replace(intent.getScheme() + ":", "");
            String[] fieldsList = TextUtils.split(dataWithoutScheme,"%=%");

            if (typeName==null) AnkiHelper.get(getApplicationContext()).saveSingleWithBasicModel(deckId,fieldsList ,  new HashSet<>(Arrays.asList(TextUtils.split(tags," "))) );
            else AnkiHelper.get(getApplicationContext()).saveSingle(deckId, AnkiHelper.get(getApplicationContext()).getModelId(typeName), fieldsList, new HashSet<>(Arrays.asList(TextUtils.split(tags," "))));

        }
    }
}
