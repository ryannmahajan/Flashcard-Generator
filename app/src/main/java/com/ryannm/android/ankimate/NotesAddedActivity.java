package com.ryannm.android.ankimate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class NotesAddedActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new NotesAddedFragment();
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, NotesAddedActivity.class);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new App().returnDrawerBuilder(this).build();
    }


}
