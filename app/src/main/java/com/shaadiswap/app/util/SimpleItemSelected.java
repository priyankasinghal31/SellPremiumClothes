package com.shaadiswap.app.util;

import android.view.View;
import android.widget.AdapterView;

public class SimpleItemSelected implements AdapterView.OnItemSelectedListener {
    private final Runnable onChange;

    public SimpleItemSelected(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        onChange.run();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
