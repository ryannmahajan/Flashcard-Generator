package com.ryannm.android.ankimate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.evernote.client.android.login.EvernoteLoginFragment;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

public class SettingsActivity extends AppCompatActivity {


    private static final String BOOLEAN_AUTHENTICATE_ON_START = "authOnStart";

    // Have used fragments instead of support fragments
    private static Intent newIntent (Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    public static Intent newIntent (Context context, boolean authenticateOnStart) {
        Intent i = newIntent(context);
        i.putExtra(BOOLEAN_AUTHENTICATE_ON_START, authenticateOnStart);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());

        if (getIntent().getBooleanExtra(BOOLEAN_AUTHENTICATE_ON_START, false))
            EverHelper.get(this).getSession().authenticate(this);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = new SettingsFragment();
            fm.beginTransaction().add(R.id.fragment_container, fragment).commit();
        }

        Drawer d = App.returnDrawerBuilder(this).build();
        d.deselect();
        IDrawerItem settingsItem = d.getDrawerItems().get(2);
        settingsItem.withSetSelected(true);
        d.updateItem(settingsItem);
    }

    private int getLayoutResId() {
        return R.layout.activity_fragment_host;
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener,EvernoteLoginFragment.ResultCallback {

        private Preference mEvernoteConnectButton;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.user_settings);

            getActivity().setTitle(R.string.title_activity_settings);

            CheckBoxPreference mEnableService = (CheckBoxPreference) findPreference(getResources().getString(R.string.enable_service));
            mEnableService.setOnPreferenceChangeListener(this);

            ListPreference mSyncIntervalPreference = (ListPreference) findPreference(getResources().getString(R.string.sync_interval_title));
            mSyncIntervalPreference.setOnPreferenceChangeListener(this);

            CheckBoxPreference mSyncOnWifiPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.use_wifi_only_title));
            mSyncOnWifiPreference.setOnPreferenceChangeListener(this);

            /*EditTextPreference mSeparatorPreference = (EditTextPreference) findPreference(getResources().getString(R.string.separator_preference_title)) ;
            mSeparatorPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
               @Override
               public boolean onPreferenceChange(Preference preference, Object newValue) {
                   if (newValue.toString() == null || newValue.toString().trim().isEmpty()) {
                       Toast.makeText(getActivity(), "Invalid input. Setting colon (\" : \") as separator text ", Toast.LENGTH_SHORT).show();
                       QueryPreferences.setPrefColonEquivalent(getActivity(), ":");
                       return false;
                   }
                   else {
                       return true;
                   }
               }}); */

            mEvernoteConnectButton = findPreference(getResources().getString(R.string.connect_to_evernote));
            if (EverHelper.get(getActivity()).getSession().isLoggedIn()) mEvernoteConnectButton.setTitle(R.string.disconnect_to_evernote);
            mEvernoteConnectButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (EverHelper.get(getActivity()).getSession().isLoggedIn()) {
                        new AlertDialog.Builder(getActivity())
                                .setMessage(R.string.are_you_sure_logout)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        EverHelper.get(getActivity()).getSession().logOut();
                                        Toast.makeText(getActivity(), R.string.done, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .create()
                                .show();
                    } else EverHelper.get(getActivity()).getSession().authenticate(getActivity());

                    return true;
                }
            });

        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            if (preference.getKey().equals(getResources().getString(R.string.sync_interval_title))) {
                setupPolling(getActivity(), QueryPreferences.isEnabled(getActivity()));
            } else if (preference.getKey().equals(getResources().getString(R.string.use_wifi_only_title))) {
                setupPolling(getActivity(), QueryPreferences.isEnabled(getActivity()));
            } else if (preference.getKey().equals(getResources().getString(R.string.enable_service))) {
                setupPolling(getActivity(), (Boolean) newValue);
            }
            return true;
        }

        private void setupPolling(Context context, Boolean shouldEnableService) {
            if (shouldEnableService) {
                NewJob.scheduleJob(context);
            } else {
                NewJob.cancelPeriodicJob();
            }
        }

        @Override
        public void onLoginFinished(boolean successful) {
            if (successful) mEvernoteConnectButton.setTitle(R.string.disconnect_to_evernote);
            else mEvernoteConnectButton.setTitle(R.string.connect_to_evernote);

            //if (getActivity().getIntent().getBooleanExtra(BOOLEAN_AUTHENTICATE_ON_START, false)) getActivity().onBackPressed();
        }

    /*    private void setupPolling(Context context, boolean shouldEnableService) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Use PolljobService
                if (shouldEnableService) {
                    if (PollJobService.isJobScheduled(context) ) {
                        PollJobService.scheduleJob(context, false);
                    }
                    PollJobService.scheduleJob(context, true);
                } if (!shouldEnableService) {
                    PollJobService.scheduleJob(context, false);
                }
            }

            else {
                if(shouldEnableService) {
                    if (PollService.isServiceAlarmOn(context) ) {
                        PollService.setServiceAlarmOn(context, false);
                    }
                    PollService.setServiceAlarmOn(context, true);
                } if(!shouldEnableService) {
                    PollService.setServiceAlarmOn(context,false);
                }
            }
        } */

    }

}
