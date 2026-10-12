package com.lorenadev.app.comercios_solidatios_tus.util;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    public static Locale LOCALE_URUGUAY = Locale.forLanguageTag("es-UY");
    public static String PREFS_NAME = "app_preferences";
    public static String COMMERCE_TAG = "COMMERCE";
    public static String MAX_DIST_KEY = "max_distance";
    public static String OFFLINE_ENABLED_KEY = "offline_enabled";

    public static URL BACKEND_URL;

    static {
        try {
            BACKEND_URL = URI.create("http://192.168.1.5:8080/").toURL();
        } catch (MalformedURLException e) {
            Log.e(COMMERCE_TAG, e.getMessage() == null ? "Error creating backend URL" : e.getMessage());
        }
    }

}
