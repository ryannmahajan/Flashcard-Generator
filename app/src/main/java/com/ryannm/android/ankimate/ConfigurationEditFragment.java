package com.ryannm.android.ankimate;

public class ConfigurationEditFragment extends ConfigurationFragment {

   /* @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
      //  super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_configuration_edit, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){

            case R.id.tick_button_edit_configuration :

                List<String> fieldKeywords = ifFieldKeywordsValid();

                if (!fieldKeywords.isEmpty()) {

                    mConfiguration.setFieldKeywords(ConfigurationLab.join(fieldKeywords));

                    if (mConfiguration.getName()==null) { mConfiguration.setName(getResources().getString(R.string.untitled_configuration)); }

                    updateConfiguration();
                    mCallbacks.onSuperBack();
                } else {
                    Toast.makeText(getActivity(), "Invalid input", Toast.LENGTH_SHORT).show();
                }

                return true;

            case R.id.delete_button :
                ConfigurationLab.get(getActivity()).deleteConfiguration(mConfiguration);
                ConfigurationLab.get(getActivity()).showConfigurationsToast(getActivity());
                mCallbacks.onSuperBack();

                return true;

            case R.id.copy_button :
                Configuration configuration = new Configuration();
                configuration.setName( mConfiguration.getName() + " " + getResources().getString(R.string.copy));
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

            case android.R.id.home : // these 3 lines (along with 2 in activity) made onNavUp control possible

                handleBackAndUp();
                return true;


            default :
                return super.onOptionsItemSelected(item);
        }
    }*/

}
