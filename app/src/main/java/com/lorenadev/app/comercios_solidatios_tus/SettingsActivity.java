package com.lorenadev.app.comercios_solidatios_tus;


import static android.widget.Toast.LENGTH_LONG;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;
import com.lorenadev.app.comercios_solidatios_tus.service.SyncDataService;
import com.lorenadev.app.comercios_solidatios_tus.util.Utils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        var preferences = getSharedPreferences(Utils.PREFS_NAME, MODE_PRIVATE);
        int savedDist = preferences.getInt(Utils.MAX_DIST_KEY, 2);

        var offlineCheckbox = (MaterialCheckBox) findViewById(R.id.offlineCheckbox);
        offlineCheckbox.setChecked(
                preferences.getBoolean(Utils.OFFLINE_ENABLED_KEY, false)
        );

        offlineCheckbox.setOnCheckedChangeListener(
                (button, checked) -> preferences.edit()
                        .putBoolean(Utils.OFFLINE_ENABLED_KEY, checked)
                        .apply()
        );


        int syncCount = preferences.getInt("sync_count_key", 0);
        boolean thereIsData = syncCount > 0;

        //offlineCheckbox.setEnabled(thereIsData);

        var toolbar = (MaterialToolbar) findViewById(R.id.settingsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        var maxDistTextView = (TextView) findViewById(R.id.maxDistLabel);
        var maxDistslider = (Slider) findViewById(R.id.maxDistSlider);

        maxDistslider.setValue(savedDist);
        maxDistTextView.setText(String.format(Utils.LOCALE_URUGUAY, "Distancia máxima: %d km", savedDist));

        maxDistslider.addOnChangeListener((slider, value, fromUser) -> {
            int km = (int) value;
            maxDistTextView.setText(String.format(Utils.LOCALE_URUGUAY, "Distancia máxima: %d km", km));
            preferences.edit().putInt(Utils.MAX_DIST_KEY, km).apply();
        });

        var syncProgress = (ProgressBar) findViewById(R.id.syncProgress);
        var downloadButton = (MaterialButton) findViewById(R.id.downloadButton);
        var lastSyncLabel = (TextView) findViewById(R.id.lastSyncLabel);

        if (thereIsData) {
            lastSyncLabel.setText(String.format(Utils.LOCALE_URUGUAY, "%d comercios descargados", syncCount));
        } else {
            lastSyncLabel.setText("Sin datos descargados");
        }

        downloadButton.setOnClickListener(v -> {
            downloadButton.setEnabled(false);
            syncProgress.setVisibility(View.VISIBLE);

            var syncService = new SyncDataService(this,
                    count -> runOnUiThread(() -> {
                        downloadButton.setEnabled(true);
                        syncProgress.setVisibility(View.GONE);
                        var message = String.format(Utils.LOCALE_URUGUAY, "%d comercios descargados", count);
                        lastSyncLabel.setText(message);
                        preferences.edit().putBoolean(Utils.OFFLINE_ENABLED_KEY, true).apply();

                        offlineCheckbox.setEnabled(count > 0);
                        offlineCheckbox.setChecked(true);
                    }),
                    message -> runOnUiThread(() -> {
                        downloadButton.setEnabled(true);
                        syncProgress.setVisibility(View.GONE);
                        Toast.makeText(this, message, LENGTH_LONG).show();
                    })
            );

            syncService.sync();
        });

    }
}