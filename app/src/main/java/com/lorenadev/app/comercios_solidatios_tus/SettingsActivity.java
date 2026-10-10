package com.lorenadev.app.comercios_solidatios_tus;


import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.slider.Slider;
import com.lorenadev.app.comercios_solidatios_tus.util.Utils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        var preferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
        int savedDist = preferences.getInt(Utils.MAX_DIST_KEY, 2);

        var toolbar = (MaterialToolbar) findViewById(R.id.settingsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        var maxDistTextView = (TextView) findViewById(R.id.maxDistLabel);
        var maxDistslider = (Slider) findViewById(R.id.maxDistSlider);

        maxDistslider.setValue(savedDist);
        maxDistTextView.setText(String.format(Utils.LOCALE_URUGUAY,"Distancia máxima: %d km", savedDist));

        maxDistslider.addOnChangeListener((slider, value, fromUser) -> {
            int km = (int) value;
            maxDistTextView.setText(String.format(Utils.LOCALE_URUGUAY,"Distancia máxima: %d km", km));
            preferences.edit().putInt(Utils.MAX_DIST_KEY, km).apply();
        });

    }
}