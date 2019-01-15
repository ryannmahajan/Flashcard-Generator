package com.ryannm.android.ankimate;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tokenautocomplete.TokenCompleteTextView;

public class TextCompletionView extends TokenCompleteTextView<String> {

    public TextCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View getViewForObject(String object) {
        LayoutInflater l = (LayoutInflater)getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        @SuppressWarnings("ResourceType") LinearLayout view = (LinearLayout)l.inflate(R.xml.contact_token, (ViewGroup)TextCompletionView.this.getParent(), false);
        ((TextView)view.findViewById(R.id.name)).setText(object);

        return view;
    }

    @Override
    protected String defaultObject(String completionText) {
        return completionText;
    }


}
