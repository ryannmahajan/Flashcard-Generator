package com.ryannm.android.ankimate;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.fabric.sdk.android.Fabric;

public class App extends Application {
    final static Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    @Override
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new JobCreator() {
            @Override
            public Job create(String tag) {
                switch (tag) {
                    case NewJob.TAG :
                        return new NewJob();
                    case NewSingleJob.TAG :
                        return new NewSingleJob();
                    default:
                        return null;
                }
            }
        });

        Fabric.with(this, new Crashlytics());
    }
    
    public static DrawerBuilder returnDrawerBuilder(final Activity activity) {

        return new DrawerBuilder()
                .withActivity(activity)
                .withActionBarDrawerToggle(false)
                .withTranslucentStatusBar(false)
                .withHeader(R.layout.material_drawer_header_logo)
                .withDrawerLayout(R.layout.drawer_layout)
                .withCloseOnClick(true)
                .withDelayOnDrawerClose(-1)
              //  .withHeaderHeight()
                .addDrawerItems(
                        new SecondaryDrawerItem().withName(R.string.configurations),
                        //new SecondaryDrawerItem().withName(R.string.added_cards),
                       // new SecondaryDrawerItem().withName(R.string.notes_to_add),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withIcon(R.drawable.ic_settings_black).withName(R.string.title_activity_settings),
                        new SecondaryDrawerItem().withIcon(R.drawable.ic_like_black).withName(R.string.feedback).withSelectable(false),
                        new SecondaryDrawerItem().withIcon(R.drawable.ic_tutorial).withName(R.string.tutorial).withSelectable(false)
                        //new SecondaryDrawerItem().withName("Sync")
                )

                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        switch (position) {
                            case 1 :
                                activity.startActivity(ListActivity.newIntent(activity));
                                return true;

                         /*   case 2 :
                                activity.startActivity(NotesAddedActivity.newIntent(activity));
                                return true;

                     /*       case 3 :
                                activity.startActivity(NotesToAddListActivity.newIntent(activity));
                                return true; */

                            case 2 :
                                return false; // DividerDrawer

                            case 3 :
                                activity.startActivity(SettingsActivity.newIntent(activity,false));
                                return true;

                            case 4 :
                                // Feedback
                                Intent email = new Intent(Intent.ACTION_SEND);
                                email.putExtra(Intent.EXTRA_EMAIL, new String[]{activity.getString(R.string.dev_email)});
                                email.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.ankimate_feedback));
                                email.putExtra(Intent.EXTRA_TEXT, getDebugInfo());

                                //need this to prompts email client only
                                email.setType("message/rfc822");

                                activity.startActivity(Intent.createChooser(email, activity.getString(R.string.choose_email_client)));
                                return true;
                            case 5 :
                                // Tutorial
                                openTutorial(activity);
                                return true;

                        /*    case 6 :
                                try {
                                    int syncState = QueryPreferences.getLastSyncedState(activity);
                                    QueryPreferences.setLastSyncedState(activity, syncState - 1);
                                    EverHelper.get(activity).performSync(false, true,false);
                                    Toast.makeText(activity, "Sync initiated", Toast.LENGTH_SHORT).show();
                                 //   QueryPreferences.setLastSyncedState(activity, syncState);
                                } catch (TException | EDAMUserException | ExecutionException | EDAMSystemException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return true; */

                            default:
                                return false;
                        }
                    }
                });
    }

    private static String getDebugInfo() {
        String s="\n \n Debug-infos:";
        s += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
        s += "\n OS API Level: " + android.os.Build.VERSION.SDK_INT;
        s += "\n Device: " + android.os.Build.DEVICE;
        s += "\n Model (and Product): " + android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")";
        return s;
    }

    public static DrawerBuilder returnDrawerBuilder(final Activity activity, Toolbar toolbar) {
        return returnDrawerBuilder(activity).withToolbar(toolbar).withActionBarDrawerToggle(true).withTranslucentStatusBar(true);
    }

    @LayoutRes
    public static int returnRelevantEmptyLayout(boolean toolbarNeeded) {
        if (toolbarNeeded) { return R.layout.empty_layout_with_message_toolbar_present; }
        else { return R.layout.empty_layout_with_message; }
    }

    public static String returnTrimmedString(String s) {
        return s.replaceAll("(^(\\u00a0|\\s)+)|((\\u00a0|\\s)+$)","");
    }

    static void showRateLimitDialog(Context c) {
        new AlertDialog.Builder(c)
                .setMessage(explainRateLimit(c))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    public static CharSequence explainRateLimit(Context c) {
        long milli = JobManager.instance().getJobRequest(NewSingleJob.jobId).getStartMs();
        String text = c.getString(R.string.rate_limit_explain);
        if (milli > 60000) text = text + TimeUnit.MILLISECONDS.toMinutes(milli) + " minutes.";
        else text = text + TimeUnit.MILLISECONDS.toSeconds(milli) + " seconds.";

        return text;
    }

    public static void openTutorial(Context c) {
        String url = "https://www.youtube.com/watch?v=BM-mGffZLJE";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        c.startActivity(i);
    }

    public static boolean isNetworkConnected(Context c ,boolean wifiOnly) {
        ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (wifiOnly) return (activeNetworkInfo!=null && (activeNetworkInfo.getType()==ConnectivityManager.TYPE_WIFI));
        else return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }


    public static String escapeSpecialChars(String s) {
        return SPECIAL_REGEX_CHARS.matcher(s).replaceAll("\\\\$0");
    }
}
