package com.ryannm.android.ankimate;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.evernote.client.android.login.EvernoteLoginFragment;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.ryannm.android.ankimate.Dao.Configuration;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

/**
 * An activity representing a list of List<Configuration>. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ConfigurationFragment} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ListActivity extends AppCompatActivity implements ConfigurationFragment.Callbacks, EvernoteLoginFragment.ResultCallback {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private boolean mLoggedIn;
    private int mSelectedPosition = -1;
    private RecyclerView mRecyclerView;

    private List<Configuration> mConfigurations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLoggedIn = EverHelper.get(this).getSession().isLoggedIn();

        showFirstRunDialog();

        if (mLoggedIn) {
            defaultSetup();
        } else {
            setContentView(R.layout.connect_to_everenote);

            Button evernoteConnect = (Button) findViewById(R.id.connect_to_evernote);

            if (evernoteConnect != null) {
                evernoteConnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EverHelper.get(ListActivity.this).getSession().authenticate(ListActivity.this);

                    /*    defaultSetup();
                        showEmptyLayout(); */
                    }
                });
            }
        }

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        if (findViewById(R.id.configuration_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

    }

    private void defaultSetup() {
        setContentView(R.layout.activity_configuration_list);

        buildDrawerWithToolbar(R.id.toolbar);

        setTitle(R.string.configurations);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addConfiguration();
                }
            });
        }

        if (mLoggedIn) showFirstRunDialog();

        mRecyclerView = (RecyclerView) findViewById(R.id.configuration_list);
        assert mRecyclerView != null;


    }

    private void buildDrawerWithToolbar(@IdRes int toolbarResId) {
        Toolbar toolbar = (Toolbar) findViewById(toolbarResId);
        setSupportActionBar(toolbar);
        Drawer d = App.returnDrawerBuilder(this, toolbar).build();
        IDrawerItem configurationsItem = d.getDrawerItems().get(0);
        configurationsItem.withSetSelected(true);
        d.updateItem(configurationsItem);
    }


    public static Intent newIntent(Context context) {
        return new Intent(context, ListActivity.class);
    }

    public void notifyUpdateOrInsertRecyclerView(Configuration c) {

        for (int i=0; i < mConfigurations.size(); i++) {
            if (mConfigurations.get(i).getId().equals(c.getId())) {
                mRecyclerView.getAdapter().notifyItemChanged(i);
                return;
            }
        }

        mConfigurations.add(0, c);
        mRecyclerView.getAdapter().notifyItemInserted(0);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (EverHelper.get(this).getSession().isLoggedIn()) {

            mConfigurations = ConfigurationLab.get(this).getConfigurations();
            Collections.reverse(mConfigurations);                    // so that new additions are on the top

            if (mTwoPane & !mConfigurations.isEmpty()) showInDetailPane(mConfigurations.get(0));
            if (mConfigurations.isEmpty()) {
                showEmptyLayout();
            }

            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            if (mRecyclerView.getAdapter() == null)
                mRecyclerView.setAdapter(new ConfigurationAdapter(mConfigurations));
            if (mRecyclerView.getAdapter() != null)
                mRecyclerView.swapAdapter(new ConfigurationAdapter(mConfigurations), false);

            long cureentMilli = new GregorianCalendar().getTimeInMillis();
            if (EverHelper.differenceInMinutesFromMilli(EverHelper.getLastDownloadedTags(), cureentMilli) >=5) {
                EverHelper.get(this).updateDownloadedTags();
                EverHelper.setLastDownloadedTags(cureentMilli);
            }

        }

    }

    @Override
    public void onSuperBack() {
        super.onBackPressed();
    }

    @Override
    public void onLoginFinished(boolean successful) {
        if (successful) {
            defaultSetup();
            showFirstRunDialog();
            onResume();
            if (AnkiHelper.shouldRequestPermission(ListActivity.this)) AnkiHelper.askForPermission(ListActivity.this, 0);
            EverHelper.get(this).addTag("anki");
        }
    }

    public class ConfigurationAdapter
            extends RecyclerView.Adapter<ConfigurationHolder> {

        private final List<Configuration> mValues;

        public ConfigurationAdapter(List<Configuration> configurations) {
            mValues = configurations;
        }

        @Override
        public ConfigurationHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ConfigurationHolder(view);
        }

        @Override
        public void onBindViewHolder(final ConfigurationHolder holder, int position) {
            holder.bindHolder(mValues.get(position));
            if(mSelectedPosition==position) {
               // holder.itemView.setBackgroundColor(getResources().getColor(R.color.higlighted_item_color));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.itemView.setElevation(-2.0f);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }

    private class ConfigurationHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private TextView mNameTextView;
        private Configuration  mConfiguration;

        public ConfigurationHolder(View itemView) {
            super(itemView);
            mNameTextView = (TextView) itemView;

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setClickable(true);
        }

        @Override
        public void onClick(View v) {
            if (mTwoPane) {
                showInDetailPane(mConfiguration);
            } else {
                startActivity(ConfigurationActivity.newIntent(ListActivity.this,mConfiguration.getId(), ConfigurationActivity.EDIT_FRAGMENT));
            }


            mSelectedPosition = getAdapterPosition();
           // itemView.setBackgroundColor(getResources().getColor(R.color.higlighted_item_color));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                itemView.setElevation(-2.0f);
            }

        }

        public void bindHolder(Configuration configuration) {
            mConfiguration = configuration;
            mNameTextView.setText(configuration.getName());
        }

        @Override
        public boolean onLongClick(View v) {
            new AlertDialog.Builder(ListActivity.this)
                    .setTitle(mConfiguration.getName())
                    .setItems(R.array.long_click_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0 : // Delete
                                    dialog.dismiss();
                                    new AlertDialog.Builder(ListActivity.this)
                                            .setMessage(String.format(getResources().getString(R.string.delete_confirmation),mConfiguration.getName()))
                                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    int position = mConfigurations.indexOf(mConfiguration);
                                                    mConfigurations.remove(position);
                                                    mRecyclerView.getAdapter().notifyItemRemoved(getAdapterPosition());
                                                    if(mConfigurations.isEmpty()) {
                                                        showEmptyLayout();
                                                    } else {
                                                        if (position==mConfigurations.size()) position--;
                                                        Configuration next = mConfigurations.get(position);
                                                        Fragment detailFrag = getSupportFragmentManager().findFragmentById(R.id.configuration_detail_container);
                                                        if(detailFrag!=null && detailFrag instanceof ConfigurationFragment) {
                                                           if (((ConfigurationFragment) detailFrag).mConfiguration.getId().equals(mConfiguration.getId())) showInDetailPane(next);
                                                   // Above statement exists so that next detail is shown only if current detail is being shown
                                                        }
                                                    }

                                                    ConfigurationLab.get(ListActivity.this).deleteConfiguration(mConfiguration);
                                                }
                                            })
                                            .setCancelable(true)
                                            .show();
                                    break;
                                case 1 : // Clone
                                    dialog.dismiss();
                                    Configuration c = ConfigurationLab.get(ListActivity.this).getCloneConfiguration(mConfiguration);
                                    mConfigurations.add(c);
                                  //  refreshRecyclerView(mRecyclerView);
                                    ConfigurationLab.get(ListActivity.this).insertConfiguration(c);
                                    ConfigurationActivity.newIntent(ListActivity.this, c.getId(), ConfigurationActivity.CLONE_FRAGMENT);
                                    break;
                            }
                        }
                    })
                    .show();
            return true;
        }
    }

    private void showInDetailPane(Configuration configuration) {
        ConfigurationFragment fragment = ConfigurationEditFragment.newInstance(configuration.getId());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.configuration_detail_container, fragment)
                .commit();
    }

    // In dual pane
    private void showConnectToEvernote() {
       // setContentView(R.layout.connect_to_everenote);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.configuration_detail_container, CustomFragment.newInstance(R.layout.connect_to_everenote))
                .commitNow();

    }

    // In dual pane
    private void showEmptyLayout() {
       /* if (mTwoPane) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.configuration_detail_container, CustomFragment.newInstance(App.returnRelevantEmptyLayout(false))) // Empty fragment
                    .commit();
        } else {
            setContentView(App.returnRelevantEmptyLayout(true));
            buildDrawerWithToolbar(R.id.toolbar);
        } */ defaultSetup();
    }

    private void addConfiguration() {
        if (AnkiHelper.isAnkiInstalled(this)) { // todo : implement check while syncing too
            if (AnkiHelper.shouldRequestPermission(this)) AnkiHelper.askForPermission(this, 0);
            else { // if permission already granted
                if (!AnkiHelper.get(this).isDeckAvaiable()) {
                    Toast.makeText(this, R.string.error_while_loading_decks, Toast.LENGTH_LONG).show();

                } else if(!AnkiHelper.get(this).isModelAvaiable()) {
                    Toast.makeText(this, R.string.error_while_loading_models, Toast.LENGTH_LONG).show();
                }
                else {
                    if (mTwoPane) {
                        ConfigurationFragment fragment = ConfigurationAddFragment.newInstance();
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.configuration_detail_container, fragment)
                                .commit();
                    } else {
                        startActivity(ConfigurationActivity.newIntent(this, (long) 0, ConfigurationActivity.ADD_FRAGMENT));
                    }
                }
            }
        } else new AlertDialog.Builder(this)
                .setTitle(R.string.anki_not_found)
                .setMessage(R.string.anki_reqd_configs)
                .setCancelable(true)
                .create()
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mLoggedIn) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.fragment_list, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {

            case R.id.refresh_button:

                if (App.isNetworkConnected(this, false)) {
                    JobRequest jobRequest;
                    try {
                        jobRequest = JobManager.instance().getJobRequest(NewSingleJob.jobId);
                    } catch (NullPointerException npe) {
                        jobRequest = null;
                        npe.printStackTrace();
                    }
                    if (jobRequest == null) {
                        long minutesSinceLastSync = EverHelper.differenceInMinutesFromMilli(QueryPreferences.getInternalLastSyncMilliseconds(this), new GregorianCalendar(TimeZone.getTimeZone("GMT + 0")).getTimeInMillis());
                        // Checking here that it is't run before 15 minutes

                        if (minutesSinceLastSync > 15) {
                            try {
                                EverHelper.get(this).performSync(false, true, false);
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(this, String.format(getResources().getString(R.string.evernote_restriction_15), Long.toString(15 - minutesSinceLastSync)), Toast.LENGTH_LONG).show();
                        }
                    } else App.showRateLimitDialog(this);
                    // todo :else if genuine error but not rate limit
                } else Toast.makeText(this, getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();

                return true;

            default :
                return super.onOptionsItemSelected(item);
        }

    }


    private static String getVersionName(Context context, Class cls) {
        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                    comp.getPackageName(), 0);
            return pInfo.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return null;
        }
    }
    private void showFirstRunDialog() {
        String currentVersion = getVersionName(this, this.getClass());
        String storedVersionName = QueryPreferences.getLastVersionName(this);

        if (storedVersionName== null || storedVersionName.length() == 0){
            // First ever run
            QueryPreferences.setLastVersionName(this,currentVersion);
            new AlertDialog.Builder(this)
                    .setMessage(R.string.tutorial_dialog_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.openTutorial(ListActivity.this);
                        }
                    })
                    .setCancelable(true)
                    .show();
        }else if (!storedVersionName.equals(currentVersion)) {
            QueryPreferences.setLastVersionName(this, currentVersion);
            // App was updated. Put appropriate code here
        }

    }

}

