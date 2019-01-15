package com.ryannm.android.ankimate;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ryannm.android.ankimate.Dao.Configuration;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigurationFragment extends Fragment {

    protected static final String ARG_CONFIGURATION_ID = "crimeIdArgument";
    private EditText NameEditText;
    private TextCompletionView TagsToFetchEditTokens;
    private CheckBox SameTagsCheckBox;
    private TextView TagsToSaveText;
    private TextCompletionView TagsToSaveEditTokens;
    private Spinner DeckSpinner;
    private Spinner ModelSpinner;
    protected Configuration mConfiguration;

    private boolean isSameTags;
    protected RecyclerView FieldKeywordsRecyclerview;
    public Callbacks mCallbacks;
    private boolean mConfigurationChanged;
    private Long modelId;
    private List<String> fields;
    private long deckId;

    public ConfigurationFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConfiguration = initConfiguration();
        modelId = mConfiguration.getModelId();
        fields = ConfigurationLab.split(mConfiguration.getFields());

        if (mConfiguration.getTagsToSave() != null) {
            isSameTags = equalsHandleNull(mConfiguration.getTagsToSave(), ConfigurationLab.SAVE_TAGS_FROM_EVERNOTE);
        }

        ConfigurationLab.get(getActivity());
        AnkiHelper.get(getActivity());
        setHasOptionsMenu(true);

       /* CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) this.getActivity().findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            appBarLayout.setTitle(mConfiguration.getName());
        } */
    }

    protected Configuration initConfiguration() {
        return ConfigurationLab.get(getActivity()).getConfiguration((Long) getArguments().get(ARG_CONFIGURATION_ID));
    }

    public interface Callbacks {
        void onSuperBack();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_configuration, parent, false); // No more CollapsingAppBar. Muhaha!

        if (getActivity() instanceof ListActivity) {
            @SuppressWarnings("ResourceType") FloatingActionButton FAB = (FloatingActionButton) v.findViewById(R.id.confirm_fab);
            FAB.setVisibility(View.VISIBLE);
            FAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveAndBack();
                }
            });
        }

        NameEditText = (EditText) v.findViewById(R.id.name_edit_text);
        NameEditText.setText(mConfiguration.getName());
        NameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
             //   mConfiguration.setName(s.toString());
                mConfigurationChanged = true;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        TagsToFetchEditTokens = (TextCompletionView) v.findViewById(R.id.fetch_tags_edit_tokens_text);
        List<String> storedTags = ConfigurationLab.split(QueryPreferences.getEvernoteTags(getActivity()) );
        ArrayAdapter<String> tagsToFetch = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, storedTags);
        TagsToFetchEditTokens.setAdapter(tagsToFetch);
        TagsToFetchEditTokens.allowDuplicates(false);
        TagsToFetchEditTokens.setSplitChar(',');

        final List<String> configFetchTags = ConfigurationLab.split(mConfiguration.getTagsToFetch());

        if (configFetchTags!=null) {
            for (String tag : configFetchTags) {
                if (storedTags.contains(tag)) {
                    TagsToFetchEditTokens.addObject(tag);
                } else {
                    configFetchTags.remove(tag);
                }}}
        mConfiguration.setTagsToFetch(ConfigurationLab.join(configFetchTags));

        TagsToFetchEditTokens.setTokenListener(new TokenCompleteTextView.TokenListener<String>() {
            @Override
            public void onTokenAdded(String token) {
                    if (!ConfigurationLab.split(QueryPreferences.getEvernoteTags(getActivity())).contains(token)) {
                        if (!isNetworkAvailableAndConnected()) {
                         //   TagsToFetchEditTokens.removeObject(token); The user doesn't like interruptions
                            Toast.makeText(getActivity(), "Unable to connect to the Evernote service. Can't create new tag: " + token, Toast.LENGTH_SHORT).show();
                        } else {
                            EverHelper.get(getActivity()).addTag(token);
                        }
                    }
                    if(configFetchTags==null || !configFetchTags.contains(token)) {
                        mConfigurationChanged = true;
                       // mConfiguration.setTagsToFetch(ConfigurationLab.join(TagsToFetchEditTokens.getObjects()));
                    }
                }

            @Override
            public void onTokenRemoved(String token) {
                mConfigurationChanged = true;
               // mConfiguration.setTagsToFetch(ConfigurationLab.join(TagsToFetchEditTokens.getObjects()));
            }});

        SameTagsCheckBox = (CheckBox) v.findViewById(R.id.use_same_tags_switch);
        SameTagsCheckBox.setChecked(isSameTags);
        SameTagsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mConfigurationChanged = true;
                isSameTags = isChecked;
                if (isChecked) {
                    //mConfiguration.setTagsToSave(ConfigurationLab.SAVE_TAGS_FROM_EVERNOTE);
                    TagsToSaveEditTokens.setVisibility(View.GONE);
                    TagsToSaveText.setVisibility(View.GONE);

                } else {
                    //mConfiguration.setTagsToSave(null);
                    TagsToSaveEditTokens.setVisibility(View.VISIBLE);
                    TagsToSaveText.setVisibility(View.VISIBLE);
                }
            }
        });

        TagsToSaveText = (TextView) v.findViewById(R.id.tags_to_save_with_text_view);
        if (!isSameTags) {
            TagsToSaveText.setVisibility(View.VISIBLE);
        }

        TagsToSaveEditTokens = (TextCompletionView) v.findViewById(R.id.save_tags_edit_tokens_text);
        if (!isSameTags) {
            TagsToSaveEditTokens.setVisibility(View.VISIBLE);
        }

        TagsToSaveEditTokens = (TextCompletionView) v.findViewById(R.id.save_tags_edit_tokens_text);
        ArrayAdapter<String> tagsToSave = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, AnkiHelper.get(getActivity()).getTags());
        TagsToSaveEditTokens.setAdapter(tagsToSave);
        TagsToSaveEditTokens.allowDuplicates(false);
        TagsToSaveEditTokens.setSplitChar(new char[] {' '});
        final List<String> tagsList =  ConfigurationLab.split(mConfiguration.getTagsToSave());
        if (tagsList!=null ) {
            if (!equalsHandleNull(mConfiguration.getTagsToSave(),ConfigurationLab.SAVE_TAGS_FROM_EVERNOTE)) {
                for (String tag : tagsList) {
                    TagsToSaveEditTokens.addObject(tag);
                }
            }
        }
        TagsToSaveEditTokens.setTokenListener(new TokenCompleteTextView.TokenListener<String>() {
            @Override
            public void onTokenAdded(String token) {
                if (tagsList == null || !tagsList.contains(token)) {
                   // mConfiguration.setTagsToSave(ConfigurationLab.join(TagsToSaveEditTokens.getObjects()));
                    mConfigurationChanged = true;
                }
            }

            @Override
            public void onTokenRemoved(String token) {
             //   mConfiguration.setTagsToSave(ConfigurationLab.join(TagsToSaveEditTokens.getObjects()));
                mConfigurationChanged = true;
            }
        });

        DeckSpinner = (Spinner) v.findViewById(R.id.deck_spinner);
        ArrayAdapter<Object> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, AnkiHelper.get(getActivity()).getDeckNames().toArray());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        DeckSpinner.setAdapter(adapter);
        DeckSpinner.setSelection(AnkiHelper.get(getActivity()).getDeckPosition(mConfiguration.getDeckId()));
        DeckSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String deckName = parent.getItemAtPosition(position).toString();
                deckId = AnkiHelper.get(getActivity()).getDeckId(deckName);
              //  mConfiguration.setDeckId(deckId);

                if (!equalsHandleNull(mConfiguration.getDeckId(),deckId)) {
                    mConfigurationChanged = true;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ModelSpinner = (Spinner) v.findViewById(R.id.type_spinner);
        ArrayAdapter<Object> modelAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, AnkiHelper.get(getActivity()).getModelNames().toArray());
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ModelSpinner.setAdapter(modelAdapter);
        ModelSpinner.setSelection(AnkiHelper.get(getActivity()).getModelPosition(mConfiguration.getModelId()));
        ModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String modelName = parent.getItemAtPosition(position).toString();
                modelId = AnkiHelper.get(getActivity()).getModelId(modelName);
              //  mConfiguration.setModelId(modelId);
              //  mConfiguration.setFields(ConfigurationLab.join(Arrays.asList(AnkiHelper.get(getActivity()).getFieldList(mConfiguration.getModelId()))));
                fields = Arrays.asList(AnkiHelper.get(getActivity()).getFieldList(modelId));
                updateFieldKeywordsRecyclerview();

                if (!equalsHandleNull(mConfiguration.getModelId(),modelId)) {
                    mConfigurationChanged = true;

                  //  updateConfiguration();

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        FieldKeywordsRecyclerview = (RecyclerView) v.findViewById(R.id.field_keywords_recyclerview);
        FieldKeywordsRecyclerview.setLayoutManager( new LinearLayoutManager(getActivity()));

        return v;
    }

    private void saveAndBack() {
        mConfiguration.setName(NameEditText.getText().toString());
        mConfiguration.setTagsToFetch(ConfigurationLab.join(TagsToFetchEditTokens.getObjects()));
        mConfiguration.setTagsToSave(SameTagsCheckBox.isChecked() ? ConfigurationLab.SAVE_TAGS_FROM_EVERNOTE: ConfigurationLab.join(TagsToSaveEditTokens.getObjects())) ;
        mConfiguration.setModelId(modelId);
        mConfiguration.setFields(ConfigurationLab.join(fields));
        mConfiguration.setDeckId(deckId);

        List<String> fieldKeywords = ifFieldKeywordsValid();

        if (!fieldKeywords.isEmpty()) {

            mConfiguration.setFieldKeywords(ConfigurationLab.join(fieldKeywords));

            if (mConfiguration.getName()==null) { mConfiguration.setName(getResources().getString(R.string.untitled_configuration)); }

            ConfigurationLab.get(getActivity()).insertOrReplaceConfiguration(mConfiguration);

            if (getActivity() instanceof ListActivity){
                ((ListActivity) getActivity()).notifyUpdateOrInsertRecyclerView(mConfiguration);

                for (int i = 0; i < fieldKeywords.size(); i++) {
                    setFieldKeywordError(i, null);
                }
                //getFragmentManager().findFragmentById().getView().findViewById()
            } else {
                mCallbacks.onSuperBack();
            }
        }
    }

    private void updateFieldKeywordsRecyclerview() {
     //  mFieldKeywords = ConfigurationLab.split(ConfigurationLab.unNullifyFieldKeywords(mConfiguration).getFieldKeywords());

        if (FieldKeywordsRecyclerview.getAdapter() == null) {
            FieldKeywordsRecyclerview.setAdapter(new KeyAdapter(fields, ConfigurationLab.split(mConfiguration.getFieldKeywords())));
        } else {
            FieldKeywordsRecyclerview.swapAdapter(new KeyAdapter(fields, ConfigurationLab.split(mConfiguration.getFieldKeywords())) , false);
        }
       // updateConfiguration();
    }

    public static ConfigurationFragment newInstance(Long configurationId) {
        Bundle args = new Bundle();
        args.putLong(ARG_CONFIGURATION_ID, configurationId);
        ConfigurationFragment fragment = new ConfigurationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);
      //  inflater.inflate(R.menu.fragment_configuration_edit, menu); WE just want the tick button
        if (getActivity() instanceof ConfigurationActivity) inflater.inflate(R.menu.fragment_configuration_add, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){

            case R.id.tick_button_add_configuration :
                saveAndBack();

                return true;

            case android.R.id.home : // these 3 lines (along with 2 in activity) made onNavUp control possible

                handleBackAndUp();
                return true;


            default :
                return super.onOptionsItemSelected(item);
        }

    }

    protected List<String> ifFieldKeywordsValid() {
        boolean ifFieldKeywordsValid = true;
        List<Configuration> subsetConfigurations = getSubsetConfigurations();
        int size = ConfigurationLab.split(mConfiguration.getFields()).size();
        List<String> fieldKeywords = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {

            EditText editText = ((TextInputLayout)FieldKeywordsRecyclerview.getChildAt(i).findViewById(R.id.field_keyword_input_layout)).getEditText();
            if (editText != null) {
                fieldKeywords.add(i, editText.getText().toString().trim());
            }
        }

        for (String fieldKeyword : fieldKeywords) {
           // int lastIndex = fieldKeywords.lastIndexOf(fieldKeyword);
            List<Integer> indices = getIndices(fieldKeyword, fieldKeywords);

            if (!TextUtils.isEmpty(fieldKeyword)) {

                if (indices.size()>1) { // So,same keyword
                    for (int index: indices) {
                        setFieldKeywordError(index, getResources().getString(R.string.duplicate));
                        ifFieldKeywordsValid = false;
                    }
                }

                for(Configuration configuration:subsetConfigurations) {
               //     if (ConfigurationLab.split(configuration.getFieldKeywords()).contains(fieldKeyword))
                    for (String otherConfigurationKeyword :ConfigurationLab.split(configuration.getFieldKeywords())) {
                        if (otherConfigurationKeyword.equalsIgnoreCase(fieldKeyword)) {
                            for (int index : indices) {
                                setFieldKeywordError(index, "Conflict: " + configuration.getName());
                                ifFieldKeywordsValid = false;
                            }
                        }
                    }
                }
            } else {
                for (int index : indices) {
                    setFieldKeywordError(index, getResources().getString(R.string.cant_be_empty));
                    ifFieldKeywordsValid = false;
                }
            }
        }

        if (ifFieldKeywordsValid) return fieldKeywords;
        return new ArrayList<>();
    }

    // Get all indices of string from list
    private List<Integer> getIndices(String string, List<String> list) {
        List<Integer> result = new ArrayList<>();

        for (int i=0; i < list.size(); i++) {
            String val = list.get(i);
            if (val.equalsIgnoreCase(string)) result.add(i);
        }
        
        return result;
    }

    private List<Configuration> getSubsetConfigurations() {
        List<Configuration> result = new ArrayList<>();
        for (Configuration configuration: ConfigurationLab.get(getActivity()).getConfigurations()) {
            if (!equalsHandleNull(configuration.getId(),mConfiguration.getId())) {
                List<String> otherConfigTags = ConfigurationLab.split(configuration.getTagsToFetch());
                if (otherConfigTags.containsAll(TagsToFetchEditTokens.getObjects()) || TagsToFetchEditTokens.getObjects().containsAll(otherConfigTags)) {
                    result.add(configuration);
                }
            }
        }
        // result.remove(mConfiguration);
        return result;
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isNetAvailable = (cm.getActiveNetworkInfo() != null);

        return (isNetAvailable && cm.getActiveNetworkInfo().isConnected() );
    }

    public void handleBackAndUp() {
       // if (!mConfiguration.equalsHandleNull(mPreviousConfiguration)) {
        if (mConfigurationChanged) {
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
                            mCallbacks.onSuperBack();
                        }
                    })
                    .show();
        } else {
            mCallbacks.onSuperBack();
        }

    }


    private class KeyHolder extends RecyclerView.ViewHolder {
        private TextView mFieldName;
        private EditText mFieldKeyword;

        public KeyHolder(View itemView) {
            super(itemView);

            mFieldName = (TextView) itemView.findViewById(R.id.field_name);
            mFieldKeyword = ((TextInputLayout)itemView.findViewById(R.id.field_keyword_input_layout) ).getEditText();
        }

        public void bindHolder (String fieldName, String fieldKeyword) {
            mFieldName.setText(fieldName);
            if (fieldKeyword!=null) {
                if (!fieldKeyword.equals(ConfigurationLab.EMPTY_FIELD_KEYWORD)) {
                    mFieldKeyword.setText(fieldKeyword);
                }
            }

            mFieldKeyword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) ((TextInputLayout)itemView.findViewById(R.id.field_keyword_input_layout) ).setError(null);
                }
            });

            mFieldKeyword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    /*if (s==null || s.toString().trim().isEmpty())  {
                        saveKeyword(FieldKeywordsRecyclerview.getChildAdapterPosition(itemView), ConfigurationLab.EMPTY_FIELD_KEYWORD);
                    } else {
                        saveKeyword(FieldKeywordsRecyclerview.getChildAdapterPosition(itemView), s.toString());
                    }*/
                    mConfigurationChanged = true;
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }

    }

    private class KeyAdapter extends RecyclerView.Adapter<KeyHolder> {

        private List<String> mFields;
        private List<String> mKeywords;

        public KeyAdapter (List<String> fields, List<String> keywords) {
            mFields = fields;
            mKeywords = keywords;
            if (mKeywords!=null) {
                if (mKeywords.size()!=mFields.size()) {
                    mKeywords = null;
                }
            }
        }

        @Override
        public KeyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            @SuppressWarnings("ResourceType") View v = inflater.inflate(R.xml.field_keyword_setup_layout,parent,false);
            return new KeyHolder(v);
        }

        @Override
        public void onBindViewHolder(KeyHolder holder, int position) {
            if (mKeywords==null) { holder.bindHolder(mFields.get(position), null); }
            else { holder.bindHolder(mFields.get(position), mKeywords.get(position)); }
        }

        @Override
        public int getItemCount() {
            return mFields.size();
        }
    }

    private void setFieldKeywordError(int index, CharSequence error) {
        TextInputLayout textInputLayout = (TextInputLayout)FieldKeywordsRecyclerview.getChildAt(index).findViewById(R.id.field_keyword_input_layout);
        textInputLayout.setError(error);
        textInputLayout.setErrorEnabled(error!=null);
    }

    protected boolean equalsHandleNull(Object one, Object two) {

        if (one==null | two==null) {
            return one == null && two == null;
        }

        return one.equals(two);
    }








}
