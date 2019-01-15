package com.ryannm.android.ankimate;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class ConfigurationCloneFragment extends ConfigurationFragment {

    public static ConfigurationCloneFragment newInstance(Long configurationId) {
        Bundle args = new Bundle();
        args.putLong(ARG_CONFIGURATION_ID, configurationId);
        ConfigurationCloneFragment fragment = new ConfigurationCloneFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConfigurationLab.get(getActivity()).deleteConfiguration(mConfiguration);
    }

   /* @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
     //   super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_configuration_cloned, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.tick_button_cloned_configuration:

                List<String> fieldKeywords = ifFieldKeywordsValid();

                if (!fieldKeywords.isEmpty()) {

                    mConfiguration.setFieldKeywords(ConfigurationLab.join(fieldKeywords));

                    if (mConfiguration.getName() == null) {
                        mConfiguration.setName(getResources().getString(R.string.untitled_configuration));
                    }

                    updateConfiguration();
                    mCallbacks.onSuperBack();
                } else {
                    Toast.makeText(getActivity(), "Invalid input", Toast.LENGTH_SHORT).show();
                }

                return true;

            case R.id.copy_button:
                Configuration configuration = new Configuration();
                configuration.setName(mConfiguration.getName() + " " + getResources().getString(R.string.copy));
                configuration.setTagsToFetch(mConfiguration.getTagsToFetch());
                configuration.setTagsToSave(mConfiguration.getTagsToSave());

                fieldKeywords = new ArrayList<>();
                for (int i = 0; i < ConfigurationLab.split(mConfiguration.getFields()).size(); i++) {
                    EditText editText = (EditText) FieldKeywordsRecyclerview.getChildAt(i).findViewById(R.id.field_keyword);
                    fieldKeywords.add(editText.getText().toString());
                }
                configuration.setFieldKeywords(ConfigurationLab.join(fieldKeywords));

                configuration.setFields(mConfiguration.getFields());
                configuration.setDeckId(mConfiguration.getDeckId());
                configuration.setModelId(mConfiguration.getModelId());
                ConfigurationLab.get(getActivity()).insertOrReplaceConfiguration(configuration);
                startActivity(ConfigurationActivity.newIntent(getActivity(), configuration.getId(), ConfigurationActivity.CLONE_FRAGMENT));

                return true;

            case android.R.id.home: // these 3 lines (along with 2 in activity) made onNavUp control possible

                handleBackAndUp();
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }

    } */

    public void handleBackAndUp() {
        // if (!mConfiguration.equals(mPreviousConfiguration)) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.clear_lose_input_dialog_text)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ConfigurationLab.get(getActivity()).deleteConfiguration(mConfiguration); // The key line
                        mCallbacks.onSuperBack();
                    }
                })
                .show();
    /*    } else {
            mCallbacks.onSuperBack();
        } */
    }
}