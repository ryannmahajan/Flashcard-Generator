package com.ryannm.android.ankimate;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.evernote.client.android.asyncclient.EvernoteNoteStoreClient;

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class NewJob extends Job{
    public static final String TAG = "new_job_tag";
    private static final long A_MIN_POLL_INTERVAL = 1000 * 60;
    private static int jobId;

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        boolean networkAvailable = App.isNetworkConnected(getContext(), QueryPreferences.isWifiOnly(getContext()));

        long minutesSinceLastSync = EverHelper.differenceInMinutesFromMilli(QueryPreferences.getInternalLastSyncMilliseconds(getContext()), new GregorianCalendar(TimeZone.getTimeZone("GMT + 0")).getTimeInMillis());
        if (minutesSinceLastSync > 15 && networkAvailable) {   // We can't run findNotes more than once every 15 mins
            try {
                EverHelper.get(getContext()).performSync(true, false, true);
                cancelAllExceptPeriodic(); // so that if this one runs and a one-off is scheduled, it is cancelled
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        } else if (minutesSinceLastSync <= 15){
            new JobRequest.Builder(NewJob.TAG)
                    .setExecutionWindow(TimeUnit.MINUTES.toMillis(15-minutesSinceLastSync), TimeUnit.MINUTES.toMillis(15-minutesSinceLastSync+60))
                    .setRequiredNetworkType(QueryPreferences.isWifiOnly(getContext())? JobRequest.NetworkType.UNMETERED: JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(false)
                    .setPersisted(true)
                    .build()
                    .schedule();
        } else {
            new JobRequest.Builder(NewJob.TAG)
                    .setExecutionWindow(TimeUnit.MINUTES.toMillis(30), TimeUnit.MINUTES.toMillis(90))
                    .setRequiredNetworkType(QueryPreferences.isWifiOnly(getContext())? JobRequest.NetworkType.UNMETERED: JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(false)
                    .setPersisted(true)
                    .build()
                    .schedule();
        }


    return Result.SUCCESS;
    }

    private void cancelAllExceptPeriodic() {
        for(JobRequest request: JobManager.instance().getAllJobRequestsForTag(NewJob.TAG)) {
            if (!request.isPeriodic()) request.cancelAndEdit();
        }
    }

    public static void scheduleJob(Context context) {
       jobId = new JobRequest.Builder(NewJob.TAG)
                .setPeriodic(QueryPreferences.getSyncInterval(context))
                .setPersisted(true)
                .setRequiredNetworkType(QueryPreferences.isWifiOnly(context)? JobRequest.NetworkType.UNMETERED: JobRequest.NetworkType.CONNECTED)
              //  .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public static void cancelPeriodicJob() {
        JobManager.instance().cancel(jobId);
    }

}
