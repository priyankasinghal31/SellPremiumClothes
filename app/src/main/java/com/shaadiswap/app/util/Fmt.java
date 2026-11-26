package com.shaadiswap.app.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class Fmt {
    private static final NumberFormat INDIAN = NumberFormat.getInstance(new Locale("en", "IN"));

    private Fmt() {
    }

    public static String price(long amount) {
        return "₹" + INDIAN.format(amount);
    }
}
