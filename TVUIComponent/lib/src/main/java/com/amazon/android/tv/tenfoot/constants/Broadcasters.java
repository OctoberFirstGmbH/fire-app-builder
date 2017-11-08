package com.amazon.android.tv.tenfoot.constants;

/**
 * Created by leokuzmanovic on 20.10.17.
 */

public class Broadcasters {

    private static final String ARD = "ARD";
    private static final String ARD_PACKAGE = "de.swr.ard.avp.mobile.android.amazon";
    private static final String ARD_CLASS = "de.swr.ard.avp.mobile.android.amazon.MainActivity";

    public static String getPackageName(final String broadcaster) {
        switch (broadcaster) {
            case ARD:
                return ARD_PACKAGE;
            default:
                return "";
        }
    }

    public static String getClassName(final String broadcaster) {
        switch (broadcaster) {
            case ARD:
                return ARD_CLASS;
            default:
                return "";
        }
    }
}
