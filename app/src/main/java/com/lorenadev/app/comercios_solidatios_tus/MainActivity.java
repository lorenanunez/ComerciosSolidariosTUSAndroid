package com.lorenadev.app.comercios_solidatios_tus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.lorenadev.app.comercios_solidatios_tus.dto.GetCommerceDto;
import com.lorenadev.app.comercios_solidatios_tus.service.FetchDataService;
import com.lorenadev.app.comercios_solidatios_tus.service.SyncDataService;
import com.lorenadev.app.comercios_solidatios_tus.util.Utils;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private FetchDataService fetchDataService;

    private MapView map;
    private ActivityResultLauncher<String> localPermissionLauncher;
    private MyLocationNewOverlay locationOverlay;
    private Runnable debounceRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fetchDataService = new FetchDataService(this.getApplicationContext());

        var syncService = new SyncDataService(this);
        syncService.sync();

        var contract = new ActivityResultContracts.RequestPermission();
        localPermissionLauncher = registerForActivityResult(contract, isGranted -> {
            if (Boolean.TRUE.equals(isGranted)) {
                Log.d(Utils.COMMERCE_TAG, "Location permission acquired");
                enableLocation();
            } else {
                Log.e(Utils.COMMERCE_TAG, "Location permission is not granted");
            }
        });

        var ctx = getApplicationContext();
        var preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        var config = Configuration.getInstance();
        config.load(ctx, preference);
        config.setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        var lat = -34.789723;
        var lng = -56.257302;

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(new GeoPoint(lat, lng));

        var mapListener = new MapListenerHandler();
        map.addMapListener(mapListener);

        requestLocationAccess();
        mapListener.scheduleReload();

        var toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> openConfiguration());
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    private void openConfiguration() {
        var configIntent = new Intent(this, SettingsActivity.class);
        startActivity(configIntent);
    }

    private void printMarker(GetCommerceDto c) {
        if (c == null) {
            Log.w(Utils.COMMERCE_TAG, "Commerce is null");
            return;
        }

        var marker = new Marker(map);
        marker.setPosition(new GeoPoint(c.lat(), c.lng()));
        marker.setTitle(c.name());
        marker.setSubDescription(c.address());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
    }

    private void loadCommerces(double lat, double lng, double maxDist) {
        var params = Map.<String, Object>of(
                "lat", lat,
                "lng", lng,
                "maxDist", maxDist
        );

        fetchDataService.fetchCommercesOnline(params, commerces -> runOnUiThread(() -> {
            map.getOverlays().removeIf(Marker.class::isInstance);
            commerces.forEach(this::printMarker);
            map.invalidate();
        }), error -> Log.e(Utils.COMMERCE_TAG, "Error: " + error));
    }

    private void requestLocationAccess() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableLocation();
        } else {
            localPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableLocation() {
        var provider = new GpsMyLocationProvider(this);
        locationOverlay = new MyLocationNewOverlay(provider, map);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        map.getOverlays().add(locationOverlay);
        map.invalidate();
    }



    private final class MapListenerHandler implements MapListener {

        private int getMaxDist() {
            var prefs = getSharedPreferences("app_preferences", MODE_PRIVATE);
            return prefs.getInt(Utils.MAX_DIST_KEY, 2);
        }

        @Override
        public boolean onScroll(ScrollEvent event) {
            scheduleReload();
            return true;
        }

        @Override
        public boolean onZoom(ZoomEvent event) {
            scheduleReload();
            return true;
        }

        private void scheduleReload() {
            debounceHandler.removeCallbacks(debounceRunnable);
            debounceRunnable = () -> {
                var center = (GeoPoint) map.getMapCenter();
                loadCommerces(center.getLatitude(), center.getLongitude(), getMaxDist());
            };
            debounceHandler.postDelayed(debounceRunnable, 50);
        }
    }
}