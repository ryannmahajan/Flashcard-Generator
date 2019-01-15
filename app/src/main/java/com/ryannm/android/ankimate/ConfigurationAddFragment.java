package com.ryannm.android.ankimate;

import com.ryannm.android.ankimate.Dao.Configuration;

public class ConfigurationAddFragment extends ConfigurationFragment {

    @Override
    protected Configuration initConfiguration() {
        return new Configuration();
    }

    public static ConfigurationAddFragment newInstance() {
        return new ConfigurationAddFragment();
    }

  /*  @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
       // super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_configuration_add, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){

            case R.id.tick_button_add_configuration :

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

            case android.R.id.home : // these 3 lines (along with 2 in activity) made onNavUp control possible

                handleBackAndUp();
                return true;

            default :
                return super.onOptionsItemSelected(item);
        }

    } */





}
