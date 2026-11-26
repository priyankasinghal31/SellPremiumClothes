package com.shaadiswap.app.util;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.shaadiswap.app.R;

/** Helpers for spinners whose stored value (key) differs from the shown label. */
public final class Options {
    private Options() {
    }

    public static String label(Context c, int keysRes, int labelsRes, String key) {
        String[] keys = c.getResources().getStringArray(keysRes);
        String[] labels = c.getResources().getStringArray(labelsRes);
        for (int i = 0; i < keys.length && i < labels.length; i++) {
            if (keys[i].equals(key)) return labels[i];
        }
        return key == null ? "" : key;
    }

    /** Returns {"All", items...} so filter spinners can have an "All" entry first. */
    public static String[] withAll(Context c, String[] items) {
        String[] out = new String[items.length + 1];
        out[0] = c.getString(R.string.all);
        System.arraycopy(items, 0, out, 1, items.length);
        return out;
    }

    public static ArrayAdapter<String> adapter(Context c, String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }
}
