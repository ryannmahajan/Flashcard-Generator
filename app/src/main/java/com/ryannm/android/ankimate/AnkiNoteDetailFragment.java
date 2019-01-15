package com.ryannm.android.ankimate;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.ryannm.android.ankimate.Dao.AnkiNote;
import com.ryannm.android.ankimate.Dao.Configuration;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.HashSet;
import java.util.List;

public class AnkiNoteDetailFragment extends Fragment {
    private static final String EXTRA_ANKINOTE_ID="com.ryannm.android.autoanki.ankinote_id";

    private Spinner mDeckSpinner;
    private RecyclerView mFieldsRecyclerView;
    private TextCompletionView mTagsToSaveEditTokens;

    private AnkiNote mAnkiNote;
    private boolean mNoteChanged;
    private Callbacks mCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAnkiNote = AnkiNoteLab.get(getActivity()).getAnkiNoteById(getArguments().getLong(EXTRA_ANKINOTE_ID));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_ankinote_detail, parent, false);

        Toolbar toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.fragment_ankinote_detail);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        toolbar.setTitle("Edit");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(2.0f);
        }
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.delete_button :
                        mCallbacks.nextNote(mAnkiNote);
                        return true;

                    case R.id.confirm_button :
                        Configuration c = ConfigurationLab.get(getActivity()).getConfiguration(mAnkiNote.getConfigurationId());
                        AnkiHelper.get(getActivity()).saveSingle(mAnkiNote.getDeckId(), c.getModelId(),
                                (String[]) ConfigurationLab.split(mAnkiNote.getFields()).toArray(new String[1]), new HashSet<>(ConfigurationLab.split(mAnkiNote.getTags())) );
                        mCallbacks.nextNote(mAnkiNote);

                        return true;
                    default:
                        return false;
                }
            }
        });
        //setHasOptionsMenu(true);
        //((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        mDeckSpinner = (Spinner) v.findViewById(R.id.deck_spinner);
        ArrayAdapter<Object> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, AnkiHelper.get(getActivity()).getDeckNames().toArray());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDeckSpinner.setAdapter(adapter);
        mDeckSpinner.setSelection(AnkiHelper.get(getActivity()).getDeckPosition(mAnkiNote.getDeckId()));
        mDeckSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String deckName = parent.getItemAtPosition(position).toString();
                Long deckId = AnkiHelper.get(getActivity()).getDeckId(deckName);
                mAnkiNote.setDeckId(deckId);

                if (!equalsHandleNull(mAnkiNote.getDeckId(),deckId)) {
                    mNoteChanged = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mFieldsRecyclerView = (RecyclerView) v.findViewById(R.id.ankinote_fields_recyclerview);
        mFieldsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFieldsRecyclerView.setAdapter
                (new FieldAdapter
                        ( ConfigurationLab.split(ConfigurationLab.get(getActivity()).getConfiguration(mAnkiNote.getConfigurationId()).getFields()), ConfigurationLab.split(mAnkiNote.getFields()) )
                );

        mTagsToSaveEditTokens = (TextCompletionView) v.findViewById(R.id.save_tags_edit_tokens_text);
        ArrayAdapter<String> tagsToSave = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, AnkiHelper.get(getActivity()).getTags());
        mTagsToSaveEditTokens.setAdapter(tagsToSave);
        mTagsToSaveEditTokens.allowDuplicates(false);
        mTagsToSaveEditTokens.setSplitChar(new char[] {' '});
        final List<String> tagsList =  ConfigurationLab.split(mAnkiNote.getTags());
        if (tagsList!=null ) {
            for (String tag : tagsList) {
                mTagsToSaveEditTokens.addObject(tag);
            }
        }

        mTagsToSaveEditTokens.setTokenListener(new TokenCompleteTextView.TokenListener<String>() {
            @Override
            public void onTokenAdded(String token) {
                if (tagsList == null || !tagsList.contains(token)) {
                    mAnkiNote.setTags(ConfigurationLab.join(mTagsToSaveEditTokens.getObjects()));
                    mNoteChanged = true;
                }
            }

            @Override
            public void onTokenRemoved(String token) {

                mNoteChanged = true;
                mAnkiNote.setTags(ConfigurationLab.join(mTagsToSaveEditTokens.getObjects()));
            }
        });

        return v;
    }

    protected boolean equalsHandleNull(Object one, Object two) {

        if (one==null | two==null) {
            return one == null && two == null;
        }

        return one.equals(two);
    }

    private class FieldAdapter extends RecyclerView.Adapter {
        private List<String> mFieldNames;
        private List<String> mFields;

        public FieldAdapter(List<String> fieldNames, List<String> fields) {
            mFieldNames = fieldNames;
            mFields = fields;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            @SuppressWarnings("ResourceType") View view = LayoutInflater.from(parent.getContext()).inflate(R.xml.field_ankinote, parent, false);
            return new FieldHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((FieldHolder)holder).bindHolder(mFieldNames.get(position), mFields.get(position));
        }

        @Override
        public int getItemCount() {
            return mFields.size();
        }

        private class FieldHolder extends RecyclerView.ViewHolder {
            private TextView mFieldNameTextView;
            private TextInputEditText mFieldContentEditText;

            public FieldHolder(View itemView) {
                super(itemView);

                mFieldNameTextView = (TextView) itemView.findViewById(R.id.field_name);
                mFieldContentEditText = (TextInputEditText) itemView.findViewById(R.id.field_content);
            }

            public void bindHolder(String fieldName, String fieldContent) {
                mFieldNameTextView.setText(fieldName);
                mFieldContentEditText.setText(fieldContent);

                mFieldContentEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        mNoteChanged = true;
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
            }
        }
    }

    public static AnkiNoteDetailFragment getInstance(Long ankiNoteId) {
        Bundle args = new Bundle();
        args.putLong(EXTRA_ANKINOTE_ID, ankiNoteId);
        AnkiNoteDetailFragment fragment = new AnkiNoteDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_ankinote_detail, menu);
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
        if (mNoteChanged) AnkiNoteLab.get(getActivity()).insertOrReplaceAnkiNote(mAnkiNote);
    }

    public interface Callbacks {
        void nextNote(AnkiNote ankiNote);
    }

}
