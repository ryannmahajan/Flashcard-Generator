package com.ryannm.android.ankimate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class ConfigurationActivity extends SingleFragmentActivity implements ConfigurationFragment.Callbacks {

    private static final String EXTRA_CONFIGURATION_ID = "com.ryannm.android.autoanki.configuration_id";
    private static final String EXTRA_FRAGMENT_ID = "com.ryannm.android.autoanki.configuration_fragment_id";
    private ConfigurationFragment mConfigurationFragment;
    public static final short ADD_FRAGMENT = 0;
    public static final short EDIT_FRAGMENT = 1;
    public static final short CLONE_FRAGMENT = 2;

    @Override
    protected Fragment createFragment() {
        switch (getIntent().getShortExtra(EXTRA_FRAGMENT_ID, ADD_FRAGMENT)) {
            case ADD_FRAGMENT :
                mConfigurationFragment = ConfigurationAddFragment.newInstance();
                break;
            case EDIT_FRAGMENT :
                mConfigurationFragment =  ConfigurationEditFragment.newInstance(getIntent().getLongExtra(EXTRA_CONFIGURATION_ID,0));
                break;
            case CLONE_FRAGMENT :
                mConfigurationFragment = ConfigurationCloneFragment.newInstance(getIntent().getLongExtra(EXTRA_CONFIGURATION_ID, 0));
        }
        return mConfigurationFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void onBackPressed() {
        mConfigurationFragment.handleBackAndUp();
    }

    @Override
    public boolean onNavigateUp() {
        mConfigurationFragment.handleBackAndUp();
        return true;
    }

    public static Intent newIntent(Context context, Long configurationId, short fragmentId) {
        Intent i = new Intent(context, ConfigurationActivity.class);
        i.putExtra(EXTRA_CONFIGURATION_ID, configurationId);
        i.putExtra(EXTRA_FRAGMENT_ID, fragmentId);
        return i;
    }

    @Override
    public void onSuperBack() {
        super.onBackPressed();
    }
}