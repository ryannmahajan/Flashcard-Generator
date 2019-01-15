package com.ryannm.android.ankimate;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.asyncclient.EvernoteCallback;
import com.evernote.client.android.asyncclient.EvernoteNoteStoreClient;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Tag;
import com.evernote.thrift.TException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ryannm.android.ankimate.Dao.BlackList;
import com.ryannm.android.ankimate.Dao.Configuration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.evernote.edam.limits.Constants;


public class EverHelper {

    private static final EvernoteSession.EvernoteService EVERNOTE_SERVICE = EvernoteSession.EvernoteService.PRODUCTION ;
    //private static final String CONSUMER_KEY = "YOUR_KEY";
    //private static final String CONSUMER_SECRET = "SECRET";

    private static EverHelper sEverHelper;
    private static Context mContext;
    private EvernoteSession mSession;
    private static List<Tag> mTagList;
    HashMap<Long, List<String[]>> fieldMap = new HashMap<>();
    HashMap<Long, List<Set<String>>> tagMap = new HashMap<>();
    private static ExecutorService sExecutorService;
    private static List<Notebook> mNotebookList;
    private static Long sLastDownloadedTagsMilli = 0l;

    public static EverHelper get(Context context) {
        if (sEverHelper ==null) {
            sEverHelper = new EverHelper(context);
        }
        return sEverHelper;
    }

    public void updateDownloadedTags() {
        if (mSession.isLoggedIn()) {
            getNoteStoreClient().listTagsAsync(new EvernoteCallback<List<Tag>>() {
                @Override
                public void onSuccess(List<Tag> result) {
                    mTagList = result;
                    QueryPreferences.setEvernoteTags(mContext, ConfigurationLab.join(getTagNames(result)));
                   // Toast.makeText(mContext, "Done " + mTagList.get(0).getName(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onException(Exception exception) {
                    exception.printStackTrace();
                }
            });
        }
    }

    private List<String> getTagNames(List<Tag> tags) {
        List<String> names = new ArrayList<>();

        for (Tag tag : tags) {
            names.add(tag.getName());
        }
        return names;
    }

    public EvernoteSession getSession() {
        return mSession;
    }

    private EverHelper(Context context) {
        mContext = context.getApplicationContext();
        mSession = new EvernoteSession.Builder(mContext)
                .setEvernoteService(EVERNOTE_SERVICE)
                .setSupportAppLinkedNotebooks(false)
                .build(CONSUMER_KEY, CONSUMER_SECRET)
                .asSingleton();
        sExecutorService = Executors.newSingleThreadExecutor();
    }


    private EvernoteNoteStoreClient getNoteStoreClient() {
        return mSession.getEvernoteClientFactory().getNoteStoreClient();
    }
    
    public static String getTagGuid(String name) {
        for (Tag tag : mTagList) {
            if (tag.getName().equalsIgnoreCase(name)) {
                return tag.getGuid();
            }
        }
        return null;
    }

    public static String getTagName(String guid) {
        if(guid!=null) {
            for (Tag tag : mTagList) {
                if (tag.getGuid().equals(guid)) {
                    return tag.getName();
                }
            }
        }
        return null;
    }

    void performSync(boolean excludeRecentNotes, boolean userManualSync, boolean fromPeriodicJob) throws ExecutionException, InterruptedException {
            if (!ConfigurationLab.get(mContext).getConfigurations().isEmpty()) {
                if (!getSession().isLoggedIn()) {
                    showNotif(EDAMErrorCode.AUTH_EXPIRED);
                    return;
                }

                if (isQuotaReached()) {
                    showNotif(EDAMErrorCode.QUOTA_REACHED);
                    return;
                }

                ArrayList<NoteMetadata> storedNotes = new ArrayList<>();
                Gson gson = new GsonBuilder().create();
                for (String str : QueryPreferences.getSavedNotesMeta(mContext)) {
                    storedNotes.add(gson.fromJson(str, NoteMetadata.class));
                }

                QueryPreferences.setSavedNotesMeta(mContext, new HashSet<String>());

                if (fromPeriodicJob || userManualSync) {
                    int onlineState = sExecutorService.submit(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return EverHelper.get(mContext).getSyncState();
                        }
                    }).get();

                    if (!storedNotes.isEmpty() || onlineState > QueryPreferences.getLastSyncedState(mContext)) {
                        QueryPreferences.setLastSyncedState(mContext, onlineState);

                        if (EverHelper.get(mContext).getSession().isLoggedIn()) {
                            EverHelper.get(mContext).setTagList(getNoteStoreClient().listTagsAsync(null).get());
                            EverHelper.get(mContext).setNotebookList(getNoteStoreClient().listNotebooksAsync(null).get());
                            String noteFilterString = "";

                            NoteFilter filter = new NoteFilter();
                            String guid = EverHelper.getTagGuid("anki");
                            filter.addToTagGuids(guid);
                            filter.setOrder(NoteSortOrder.UPDATED.getValue());
                            filter.setOrderIsSet(true);
                            if (QueryPreferences.getLastSyncedDate(mContext) != null) {
                                noteFilterString = noteFilterString + "updated:" + QueryPreferences.getLastSyncedDate(mContext);
                            }
                          /*  if (excludeRecentNotes) {
                                long mins = TimeUnit.MILLISECONDS.toMinutes(new GregorianCalendar(TimeZone.getTimeZone("GMT")).getTimeInMillis());
                                noteFilterString = noteFilterString.concat(" -updated:"+ConfigurationLab.getDatePreferencesString(TimeUnit.MINUTES.toMillis(mins-15)));
                            } */
                            if (!noteFilterString.equals("")) filter.setWords(noteFilterString);

                            NotesMetadataResultSpec resultSpec = new NotesMetadataResultSpec();
                            resultSpec.setIncludeTitle(true);
                            resultSpec.setIncludeTagGuids(true);
                            resultSpec.setIncludeUpdated(true);
                            resultSpec.setIncludeNotebookGuid(true);

                            int offset = 0;
                            int pageSize = 10;
                            NotesMetadataList notesMetadataList = new NotesMetadataList();
                            List<NoteMetadata> notes;
                            do {
                                try {

                                    notesMetadataList = getNoteStoreClient().findNotesMetadataAsync(filter, offset, pageSize, resultSpec, null).get();
                                    QueryPreferences.setInternalLastSyncMilliseconds(mContext, new GregorianCalendar(TimeZone.getTimeZone("GMT")).getTimeInMillis());
                                    // We're trying to budget findNotes()

                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                                notes = notesMetadataList.getNotes();
                                for (NoteMetadata note : notes) {
                                    for (int k = 0; k < storedNotes.size(); k++) {
                                        if (storedNotes.get(k).getGuid().equals(note.getGuid()))
                                            storedNotes.remove(k); // Remove old stored versions if we have new versions
                                    }
                                }
                                offset += notes.size();

                                if (!storedNotes.isEmpty()) {
                                    notes.addAll((Collection<? extends NoteMetadata>) storedNotes.clone());
                                    storedNotes.clear();
                                }

                                if (excludeRecentNotes) {
                                    sortDocuments(excludeRecent(notes), userManualSync);
                                    QueryPreferences.setLastSyncedDate(mContext, ConfigurationLab.getDatePreferencesString());
                                }
                                else {
                                    sortDocuments(notes, userManualSync);
                                    QueryPreferences.setLastSyncedDate(mContext, ConfigurationLab.getDatePreferencesString());
                                }

                            } while (notesMetadataList.getTotalNotes() > offset);

                           // if (userManualSync) Toast.makeText(mContext, mContext.getString(R.string.sync_successful) + ": "+notes.size()+" notes", Toast.LENGTH_SHORT).show();

                        }
                    } else if (userManualSync) {
                        Toast.makeText(mContext, mContext.getString(R.string.notes_already_synced), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    sortDocuments(storedNotes, userManualSync);
                }
            } else if (userManualSync)
                Toast.makeText(mContext, mContext.getString(R.string.no_configs_to_sync), Toast.LENGTH_SHORT).show();

    }

    private boolean isQuotaReached() throws ExecutionException, InterruptedException {
        long monthlyQuota = sExecutorService.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return getSession().getEvernoteClientFactory().getUserStoreClient().getUser().getAccounting().getUploadLimit();
            }
        }).get();


        long usedSoFar = sExecutorService.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return getNoteStoreClient().getSyncState().getUploaded();
            }
        }).get();

        return (usedSoFar > monthlyQuota) || ((monthlyQuota - usedSoFar) <= 20);
    }

    private void sortDocuments(final List<NoteMetadata> notes, final boolean userManualSync) {
        try {
            List<Configuration> configurations = ConfigurationLab.get(mContext).getConfigurations();

            if (isAllConfigurationsValid(configurations)) {

                for (int i = 0; i < notes.size(); i++) {
                    final NoteMetadata note = notes.get(i);

                    /*BlackList blackList = BlackListLab.get(mContext).getBlacklistById(note.getGuid());
                    if ( blackList == null) {
                        blackList = new BlackList(note.getGuid(), note.getTitle(), 0, getNotebookTitle(note.getNotebookGuid()));
                        BlackListLab.get(mContext).insertOrReplaceBlacklist(blackList);
                    } */ // Commented out cos blacklist shouldn't be reqd. if we use bookmark
                    BlackList blackList = new BlackList(note.getGuid(), note.getTitle(), 0, getNotebookTitle(note.getNotebookGuid()));

                    final NotePreacher preacher = new NotePreacher(note, blackList, mContext);

                    final List<String> lastFieldNominees = new ArrayList<>();

                    boolean firstRun = false; // First run for a specific note

                    for (Configuration configuration : configurations) {
                        if (preacher.ifConfigurationInNote(configuration)) {

                            if (preacher.getNoteContentDoc() == null) {
                                String content = null;
                                try {
                                    content = (sExecutorService.submit(new Callable<String>() {
                                        @Override
                                        public String call() throws Exception {
                                            return getNoteStoreClient().getNoteContent(note.getGuid());
                                        }
                                    }).get());
                                } catch (Exception e) {
                                    handleEvernoteError(e, notes, i, userManualSync);
                                }
                                preacher.setNoteContentDoc(Jsoup.parse(content, "", Parser.xmlParser()));
                                preacher.deleteNoteContentBeforeText(mContext.getString(R.string.bookmark)).run();
                            }

                            List<String[]> fieldLists = new ArrayList<>();
                            // Removed for now               List<List<String>> addLaterLists = new ArrayList<>();

                            try {
                                if (preacher.getBlacklistSet() == null) {
                                    preacher.setBlacklistSet(sExecutorService.submit(preacher.gatherBlackListSet()).get());
                                }

                                if (!preacher.isAllFieldsBlacklisted()) {
                                    fieldLists = sExecutorService.submit(preacher.getFields(configuration)).get();
                                    //    Removed for now                 addLaterLists = sExecutorService.submit(preacher.getNotesToAdd(configuration)).get();
                                    if (!fieldLists.isEmpty()) firstRun = true;

                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }

                            if (!fieldLists.isEmpty())
                                Collections.addAll(lastFieldNominees, fieldLists.get(fieldLists.size() - 1));

                            List<Set<String>> tagLists = new ArrayList<>(fieldLists.size());
                            Set<String> tagSet = preacher.getTags(configuration);
                            for (int j = 0; j < fieldLists.size(); j++) {
                                tagLists.add(tagSet);
                            }

                            //Set<String> tagAddLaterSet = preacher.getTags(configuration); WE can just use tagSet

                   /* Removed for now         List<AnkiNote> ankiNotes = new ArrayList<>( addLaterLists.size() );
                            for (int i=0; i < addLaterLists.size(); i++) {
                                AnkiNote ankiNote = new AnkiNote();
                                ankiNote.setConfigurationId(configuration.getId());
                                ankiNote.setFields(ConfigurationLab.join(addLaterLists.get(i)));
                                ankiNote.setDeckId(configuration.getDeckId());
                                ankiNote.setTags(ConfigurationLab.join(new ArrayList<String>(tagSet)));
                                ankiNote.setNotebookTitle(getNotebookTitle(note.getNotebookGuid()));
                                ankiNote.setNoteTitle(note.getTitle());
                                ankiNotes.add(ankiNote);
                            }
                            AnkiNoteLab.get(mContext).insertOrReplaceAnkiNotes(ankiNotes); // To the "notes to add" screen

                            blackList.setCardsAdded( blackList.getCardsAdded() + fieldLists.size() + addLaterLists.size()); */

                        /*    blackList.setCardsAdded( blackList.getCardsAdded() + fieldLists.size() );
                            blackList.setNoteTitle(note.getTitle());
                            blackList.setNotebook(getNotebookTitle(note.getNotebookGuid()));
                            BlackListLab.get(mContext).updateBlacklist(blackList); Last 4 lines Commented out cos blacklist shouldn't be reqd. if we use bookmark*/

                            if (fieldMap.get(configuration.getId()) == null || tagMap.get(configuration.getId()) == null) {
                                if (fieldMap.get(configuration.getId()) == null) {
                                    fieldMap.put(configuration.getId(), fieldLists);
                                }

                                if (tagMap.get(configuration.getId()) == null) {
                                    tagMap.put(configuration.getId(), tagLists);
                                }
                            } else {
                                fieldLists.addAll(fieldMap.get(configuration.getId()));
                                tagLists.addAll(tagMap.get(configuration.getId()));
                                fieldMap.put(configuration.getId(), fieldLists);
                                tagMap.put(configuration.getId(), tagLists);
                            }
                        }

                    }
                    if (firstRun) {

                       /* sExecutorService.submit(*/
                        preacher.destroyTextFromOriginal(mContext.getString(R.string.bookmark), true);
                        Log.d("EverHelper", "destroyed bookmark");
                        Note originalNote = null;

                        try {
                            originalNote = sExecutorService.submit(new Callable<Note>() {
                                @Override
                                public Note call() throws Exception {
                                    return getNoteStoreClient().getNote(note.getGuid(), false, false, false, false);
                                }
                            }).get();
                        } catch (Exception e) {
                            handleEvernoteError(e, notes, i, userManualSync);
                        }

                        if (originalNote != null) {
                            Document doc = preacher.writeBookmarkToOriginal(lastFieldNominees);
                            String newContent = doc.outerHtml();
                            if (!newContent.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
                                newContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newContent;
                            newContent = newContent.replace("<!DOCTYPE en-note \"http://xml.evernote.com/pub/enml2.dtd\">", "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">");
                            Log.d("Everhelper", newContent);

                            originalNote.setContent(newContent);
                            originalNote.setContentIsSet(true);

                            final Note finalOriginalNote = originalNote;
                            try {
                                Log.d("EverHelper", "Success: " +
                                        sExecutorService.submit(new Callable<Note>() {
                                            @Override
                                            public Note call() throws Exception {
                                                return getNoteStoreClient().updateNote(finalOriginalNote);
                                            }
                                        }).get().getContent());
                            } catch (Exception e) {
                                handleEvernoteError(e, notes, i, userManualSync);
                            }
                        }

                        /* getNoteStoreClient().getNoteAsync(note.getGuid(), false, false, false, false, new EvernoteCallback<Note>() {
                            @Override
                            public void onSuccess(Note originalNote) {
                                 // get in "Note" format and set its content to the the content with bookmark written on it
                                Document doc = preacher.writeBookmarkToOriginal(lastFieldNominees);
                                String newContent = doc.outerHtml();
                                if (!newContent.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) newContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newContent;
                                newContent = newContent.replace("<!DOCTYPE en-note \"http://xml.evernote.com/pub/enml2.dtd\">", "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">");
                                Log.d("Everhelper", newContent);

                                originalNote.setContent(newContent);
                                originalNote.setContentIsSet(true);

                                getNoteStoreClient().updateNoteAsync(originalNote, new EvernoteCallback<Note>() {
                                    @Override
                                    public void onSuccess(Note success) {
                                        Log.d("EverHelper", "Success: " + success.getContent());
                                    }

                                    @Override
                                    public void onException(Exception exception) {
                                        handleEvernoteError(exception, notes, finalI, userManualSync);
                                    }
                                });
                            }

                            @Override
                            public void onException(Exception exception) {
                                handleEvernoteError(exception, notes, finalI, userManualSync);
                            }
                        }); */
                    }


                } // Save NoteMetaData

                // After parsing for every note is done
                for (Configuration configuration : configurations) {
                    AnkiHelper.get(mContext).save(configuration.getDeckId(), configuration.getModelId(), fieldMap.get(configuration.getId()), tagMap.get(configuration.getId()));
                    if (userManualSync)
                    Toast.makeText(mContext, mContext.getString(R.string.sync_successful) + ": " + notes.size() + " notes", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (DeliberateException e) { // thrown inside handleEvernoteError()
           if (userManualSync) Toast.makeText(mContext, R.string.sync_error, Toast.LENGTH_SHORT).show();
        }

    }

    private void handleEvernoteError(Exception e, List<NoteMetadata> notes, int currentNoteIndex, boolean userManualSync) {
        if (e instanceof EDAMUserException) {
            showNotif(((EDAMUserException) e).getErrorCode());
            saveForLater(notes.subList(currentNoteIndex, notes.size() - 1));
        } else if (e instanceof EDAMSystemException) {
            if ((((EDAMSystemException) e).getErrorCode().equals(EDAMErrorCode.RATE_LIMIT_REACHED))) NewSingleJob.scheduleJob(mContext, TimeUnit.SECONDS.toMinutes(((EDAMSystemException) e).getRateLimitDuration()));
            showNotif(((EDAMSystemException) e).getErrorCode());
            saveForLater(notes.subList(currentNoteIndex, notes.size() - 1));
        }

        e.printStackTrace();

        throw new DeliberateException();
    }

    private void showNotif(EDAMErrorCode errorCode) {
        String message = null;
        PendingIntent pendingIntent = null;

        if (errorCode.equals(EDAMErrorCode.AUTH_EXPIRED))  {
            message = mContext.getString(R.string.auth_expired);
            Intent i = SettingsActivity.newIntent(mContext,true);
            pendingIntent = PendingIntent.getActivity(mContext, 0, i, 0);

        }

        else if (errorCode.equals(EDAMErrorCode.RATE_LIMIT_REACHED)) {
            message = App.explainRateLimit(mContext).toString();
        }

        else if (errorCode.equals(EDAMErrorCode.QUOTA_REACHED)) {
            try {
                long hoursLeft = TimeUnit.MILLISECONDS.toHours(new Date(getSession().getEvernoteClientFactory().getUserStoreClient().getUser().getAccounting().getUploadLimitEnd()).getTime());
                String timeLeft = "" + hoursLeft + " hours";
                if (hoursLeft <=1) timeLeft = "" + TimeUnit.HOURS.toMinutes(hoursLeft) + " minutes";
                else if (hoursLeft>=24) timeLeft = "" + TimeUnit.HOURS.toDays(hoursLeft) + " days";

                message = mContext.getResources().getString(R.string.quota_reached,timeLeft);

                if (getSession().getEvernoteClientFactory().getUserStoreClient().getUser().getPremiumInfo().isPremiumUpgradable()) {
                    pendingIntent = PendingIntent.getActivity(mContext, 0, upgradeToPremiumIntent(), 0);
                    message = message + mContext.getString(R.string.click_here_to_do_that);
                }

              /*  else if (getSession().getEvernoteClientFactory().getUserStoreClient().getUser().getPremiumInfo().isCanPurchaseUploadAllowance())
                   todo: increase upload allowance,if already premium. Increase allowance link reqd. for this
                  pendingIntent = PendingIntent.getActivity(mContext,0, increaseUploadAllowanceIntent(),0); */

            } catch (EDAMUserException | EDAMSystemException | TException e) {
                e.printStackTrace();
                if (e instanceof EDAMUserException) if (((EDAMUserException) e).getErrorCode()!=EDAMErrorCode.QUOTA_REACHED) showNotif(((EDAMUserException) e).getErrorCode());
                if (e instanceof EDAMSystemException) if (((EDAMSystemException) e).getErrorCode()!=EDAMErrorCode.QUOTA_REACHED) showNotif(((EDAMSystemException) e).getErrorCode());
            }
        }// todo: finish sending these notifications

       /* else if (errorCode.equals(EDAMErrorCode.PERMISSION_DENIED)) {
            sendEmailToDev();
        } */

        Resources resources = mContext.getResources();
        android.support.v4.app.NotificationCompat.Builder request = new
                NotificationCompat.Builder(mContext)
                //    .setTicker(String.format(resources.getString(R.string.invalid_deck),configuration.getName()))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(resources.getString(R.string.sync_error))
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        if (pendingIntent!=null) request.setContentIntent(pendingIntent);
        NotificationManagerCompat manager = NotificationManagerCompat.from(mContext);
        manager.notify(0, request.build());


    }

    private Intent upgradeToPremiumIntent() {
        String url = "https://www.evernote.com/subscriptions/upgrade?offer=perm_nav_account_banner_basic";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setData(Uri.parse(url));
        return i;
    }

    private void saveForLater(List<NoteMetadata> metas) {
        Set<String> set = QueryPreferences.getSavedNotesMeta(mContext);
        Gson gson = new  GsonBuilder().create();
        for (NoteMetadata meta : metas) {

            set.add(gson.toJson(meta));
        }
        QueryPreferences.setSavedNotesMeta(mContext, set);
    }

    private boolean isAllConfigurationsValid(List<Configuration> configurations) {
        Set<Long> deckIds = AnkiHelper.get(mContext).getDeckIds();
        Set<Long> modelIds = AnkiHelper.get(mContext).getModelIds();

        boolean allConfigurationsValid = true;

        for (Configuration configuration: configurations) {

            if (!deckIds.contains(configuration.getDeckId())) {
                ConfigurationLab.get(mContext).createDeckNotification(configuration); }
            if (!modelIds.contains(configuration.getModelId())) { ConfigurationLab.get(mContext).createModelNotification(configuration); }
            String[] fieldNames = AnkiHelper.get(mContext).getFieldList(configuration.getModelId());
            boolean fieldsAreSame = Arrays.equals(fieldNames, ConfigurationLab.split(configuration.getFields()).toArray());
            if(!fieldsAreSame) { ConfigurationLab.get(mContext).createFieldsNotification(configuration); }

            if (allConfigurationsValid) { allConfigurationsValid = (deckIds.contains(configuration.getDeckId()) && modelIds.contains(configuration.getModelId()) && fieldsAreSame ); }

        }
        return allConfigurationsValid;
    }

    private void getAndHandleFieldLists(Document document, final Configuration configuration, Set mBlacklist, final NoteMetadata note, final FieldsTagsCallback callback) {
            ENMLhelper.getFieldLists(document, configuration, ":", mBlacklist, new ENMLhelper.ENMLFieldListCallbacks() {
                @Override
                public void onParseComplete(List<String[]> lists) {
                    if (lists == null) {
                        lists = new ArrayList<String[]>();
                    }

                    List<Set<String>> tagLists = new ArrayList<Set<String>>(lists.size());

                    if (configuration.getTagsToSave() != null) {
                        if (configuration.getTagsToSave().equals(ConfigurationLab.SAVE_TAGS_FROM_EVERNOTE)) {
                            List<String> tagGuids = note.getTagGuids();
                            Set<String> tagNames = new HashSet<String>();
                            for (String tagGuid : tagGuids) {
                                tagNames.add(getTagName(tagGuid));
                                // TODO: Add this tag at first run
                                tagNames.remove("anki");
                                tagNames.remove("Anki");
                                tagNames.remove("ANKI");
                            }
                            // New add from here (the for loop)
                            for (int i =0; i < lists.size(); i++) {
                                tagLists.add(tagNames);
                            }
                        } else {
                            Set<String> tagNames = new HashSet<String>(ConfigurationLab.split(configuration.getTagsToSave()));
                            for (int i =0; i < lists.size(); i++) {
                                tagLists.add(tagNames);
                            }
                        }
                    }
                    callback.onResult(lists, tagLists);
                }
            });

    }


    public void addTag (final String tagName) {
        Tag tag = new Tag();
        tag.setName(tagName);

        getNoteStoreClient().createTagAsync(tag, new EvernoteCallback<Tag>() {
            @Override
            public void onSuccess(Tag result) {
               // updateDownloadedTags();
                mTagList.add(result);
            }

            @Override
            public void onException(Exception exception) {
                exception.printStackTrace();
                if (!tagName.equalsIgnoreCase("anki")) Toast.makeText(mContext, "Error occured while creating tag: "+tagName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static String getTagNameRegex() {
        return Constants.EDAM_TAG_NAME_REGEX;
    }

    public void setTagList(List<Tag> tagList) {
        mTagList = tagList;
        QueryPreferences.setEvernoteTags(mContext, ConfigurationLab.join(getTagNames(mTagList)));
    }

    public int getSyncState() throws TException, EDAMUserException, EDAMSystemException {
        return getNoteStoreClient().getSyncState().getUpdateCount();
    }

    public void setNotebookList(List<Notebook> notebookList) {
        mNotebookList = notebookList;
    }

    public static String getNotebookTitle(String notebookGuid) {
        for (Notebook notebook : mNotebookList) {
            if (notebook.getGuid().equals(notebookGuid)) return notebook.getName();
        }
        return "Default";
    }

    public List<BlackList> refreshAllAnkiTaggedNotes() throws ExecutionException, InterruptedException, TException, EDAMUserException, EDAMSystemException {

            return sExecutorService.submit(new Callable<List<BlackList>>() {
                @Override
                public List<BlackList> call() throws Exception {
                    return newBlacklists();
                }
            }).get();

    }

    // Returns the blacklists that haven't yet had a run-in to AnkiMate's filtering system (i.e created after last update) but contain "Anki" tag
    private List<BlackList> newBlacklists() {
        List<BlackList> result = new ArrayList<>();
        try {
            if (!ConfigurationLab.get(mContext).getConfigurations().isEmpty()) {

                    //QueryPreferences.setLastSyncedState(mContext, EverHelper.get(mContext).getSyncState());

                    if (EverHelper.get(mContext).getSession().isLoggedIn()) {
                        EverHelper.get(mContext).setTagList(getNoteStoreClient().listTagsAsync(null).get());
                        EverHelper.get(mContext).setNotebookList(getNoteStoreClient().listNotebooksAsync(null).get());

                        NoteFilter filter = new NoteFilter();
                        String guid = EverHelper.getTagGuid("anki");
                        filter.addToTagGuids(guid);
                        filter.setOrder(NoteSortOrder.CREATED.getValue());
                        filter.setOrderIsSet(false);
                        if (QueryPreferences.getLastSyncedDate(mContext) != null) {
                            filter.setWords("created:" + QueryPreferences.getLastSyncedDate(mContext));
                        }

                        NotesMetadataResultSpec resultSpec = new NotesMetadataResultSpec();
                        resultSpec.setIncludeTitle(true);
                        resultSpec.setIncludeTagGuids(true);
                       // resultSpec.setIncludeUpdated(true);
                        resultSpec.setIncludeNotebookGuid(true);

                        int offset = 0;
                        int pageSize = 10;
                        NotesMetadataList notesMetadataList = new NotesMetadataList();
                        do {
                            try {
                                notesMetadataList = getNoteStoreClient().findNotesMetadataAsync(filter, offset, pageSize, resultSpec, null).get();
                                //  // Log.d(TAG, notesMetadataList.getNotes().get(2).getTitle());
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                               // if (offset == 0) QueryPreferences.setLastSyncedDate(mContext, ConfigurationLab.getDatePreferencesString());
                            }
                            List<NoteMetadata> notes = notesMetadataList.getNotes();

                            for (NoteMetadata note : notes) {
                                BlackList blackList = new BlackList();
                                blackList.setNoteTitle(note.getTitle());
                                blackList.setCardsAdded(0);
                                blackList.setNotebook(getNotebookTitle(note.getNotebookGuid()));
                                result.add(blackList);
                            }
                            QueryPreferences.setInternalLastSyncMilliseconds(mContext, new GregorianCalendar(TimeZone.getTimeZone("GMT + 0")).getTimeInMillis());

                            offset+=notes.size();

                        } while (notesMetadataList.getTotalNotes() > offset);
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private interface FieldsTagsCallback {
        void onResult(final List<String[]> mFieldLists, final List<Set<String>> mTagLists);
    }

    // This only saves the notes not updated in the last,say 15 minutes
    private List<NoteMetadata> excludeRecent(List<NoteMetadata> notes) {
        Long currentMilli = new GregorianCalendar(TimeZone.getTimeZone("GMT")).getTimeInMillis();
        List<NoteMetadata> result = new ArrayList<>(notes);
        List<NoteMetadata> saveMetas = new ArrayList<>();
     //   Long toSaveMilli = new GregorianCalendar(TimeZone.getTimeZone("GMT")).getTimeInMillis();

        for (NoteMetadata note: notes) {
            if (differenceInMinutesFromMilli(note.getUpdated(), currentMilli) < 15 /*Or whatever the user sets*/) {
                if(result.remove(note)) saveMetas.add(note);
            }
        }
       // QueryPreferences.setLastSyncedDate(mContext, ConfigurationLab.getDatePreferencesString(toSaveMilli-1));
        if (!saveMetas.isEmpty()) saveForLater(saveMetas);

        return result;
    }

    public static long differenceInMinutesFromMilli(Long mill1, Long mill2) {
        long min1 = mill1 / 60000 ;
        long min2 = mill2 / 60000 ;


        if (min1 > min2) {
            return (min1-min2) ;
        } else {
            return (min2-min1);
        }
    }

    public static long getLastDownloadedTags() {
        return sLastDownloadedTagsMilli;
    }

    public static void setLastDownloadedTags(long lastDownloadedTags) {
        EverHelper.sLastDownloadedTagsMilli = lastDownloadedTags;
    }

    interface Callback<T> {
        void onResultReady(T result);
    }

    private class DeliberateException extends RuntimeException {

    }
}
