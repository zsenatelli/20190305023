package com.example.shoppingapp;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences("settings_preferences", MODE_PRIVATE);
        String language = prefs.getString("language", "en");
        Locale locale = new Locale(language);
        Context context = ContextWrapper.wrap(base, locale);
        super.attachBaseContext(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
