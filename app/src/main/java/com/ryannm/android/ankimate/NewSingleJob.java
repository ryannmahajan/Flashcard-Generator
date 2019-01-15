package com.ryannm.android.ankimate;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// This job runs if the regular job (NewJob) was run before 15 minutes by the android system or if an error caused some notes to remain unsynced
public class NewSingleJob extends Job{
    public static int jobId;
    public static final String TAG = "new_job_single_tag";

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        boolean networkAvailable = App.isNetworkConnected(getContext(), QueryPreferences.isWifiOnly(getContext()));
        long minutesSinceLastSync = EverHelper.differenceInMinutesFromMilli(QueryPreferences.getInternalLastSyncMilliseconds(getContext()), new GregorianCalendar(TimeZone.getTimeZone("GMT + 0")).getTimeInMillis());
            if (minutesSinceLastSync > 15 && networkAvailable) {
                try {
                    EverHelper.get(getContext()).performSync(true, false, false);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                return Result.RESCHEDULE;
            }

        return Result.SUCCESS;
    }

    public static void scheduleJob(Context context, Long minutes) {
        Long milli = TimeUnit.MINUTES.toMillis(minutes);
        jobId = new JobRequest.Builder(NewSingleJob.TAG)
                .setPersisted(true)
                .setRequiredNetworkType(QueryPreferences.isWifiOnly(context)? JobRequest.NetworkType.UNMETERED: JobRequest.NetworkType.CONNECTED)
            //    .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .setBackoffCriteria(milli, JobRequest.BackoffPolicy.LINEAR)
                .setExecutionWindow(milli,milli + TimeUnit.MINUTES.toMillis(90))
                .build()
                .schedule();
        Log.d(NewSingleJob.TAG, "A single job to be run b/w "+minutes+" to "+(minutes+90)+" mins");

    }
}
