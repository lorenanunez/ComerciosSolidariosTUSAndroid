package com.lorenadev.app.comercios_solidatios_tus;

import static android.widget.Toast.LENGTH_LONG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.lorenadev.app.comercios_solidatios_tus.entity.CommerceEntity;
import com.lorenadev.app.comercios_solidatios_tus.service.FetchDataService;
import com.lorenadev.app.comercios_solidatios_tus.util.Utils;

import org.apache.commons.lang3.StringUtils;
import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.plugins.annotation.SymbolManager;
import org.maplibre.android.plugins.annotation.SymbolOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MapLibreActivity extends AppCompatActivity {

    private final Map<Long, CommerceEntity> symbolCommerceMap = new HashMap<>();
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private FetchDataService fetchDataService;
    private MapView mapView;
    private CardView cardCommerceInfo;
    private TextView txtCommerceName;
    private TextView txtCommerceDetails;
    private MapLibreMap mapLibreMap;
    private SymbolManager symbolManager;
    private Runnable debounceRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_map_libre);

        fetchDataService = new FetchDataService(this.getApplicationContext());

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        cardCommerceInfo = findViewById(R.id.cardCommerceInfo);
        txtCommerceName = findViewById(R.id.txtCommerceName);
        txtCommerceDetails = findViewById(R.id.txtCommerceDetails);

        var contract = new ActivityResultContracts.RequestPermission();
        registerForActivityResult(contract, isGranted -> {
            if (Boolean.TRUE.equals(isGranted)) {
                if (mapLibreMap != null && mapLibreMap.getStyle() != null) {
                    var locationComponent = mapLibreMap.getLocationComponent();
                    locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(this, mapLibreMap.getStyle()).build());
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationComponent.setLocationComponentEnabled(true);
                        locationComponent.setCameraMode(CameraMode.TRACKING);
                        locationComponent.setRenderMode(RenderMode.COMPASS);
                    }
                }
            } else {
                Toast.makeText(this, "Location permission is not granted", LENGTH_LONG).show();
            }
        });

        mapView.getMapAsync(map -> {
            mapLibreMap = map;
            map.setStyle(new Style.Builder().fromUri("asset://style-local.json"), style -> {
                symbolManager = new SymbolManager(mapView, mapLibreMap, style);
                symbolManager.setIconAllowOverlap(true);
                symbolManager.setIconIgnorePlacement(true);

                var uruguayBounds = new LatLngBounds.Builder()
                        .include(new LatLng(-30.0, -53.0))
                        .include(new LatLng(-35.0, -58.5))
                        .build();

                mapLibreMap.setLatLngBoundsForCameraTarget(uruguayBounds);
                mapLibreMap.setMinZoomPreference(6.0);

                symbolManager.addClickListener(symbol -> {
                    var commerce = symbolCommerceMap.get(symbol.getId());
                    if (commerce != null) {
                        showCommerceDetails(commerce);
                    }
                    return true;
                });

                mapLibreMap.addOnMapClickListener(latLng -> {
                    hideCommerceDetails();
                    return false;
                });

                mapLibreMap.addOnCameraIdleListener(this::scheduleReload);
                scheduleReload();
            });

            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(-34.9011, -56.1645))
                    .zoom(12.0)
                    .build());
        });

    }

    private void showCommerceDetails(CommerceEntity c) {
        var subDescription = Stream.of(c.getAddress(), c.getPhone(), c.getCategory(), c.getSubCategory())
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining("\n"));

        txtCommerceName.setText(c.getName());
        txtCommerceDetails.setText(subDescription);
        cardCommerceInfo.setVisibility(View.VISIBLE);
    }

    private void hideCommerceDetails() {
        if (cardCommerceInfo.getVisibility() == View.VISIBLE) {
            cardCommerceInfo.setVisibility(View.GONE);
        }
    }

    private int getMaxDist() {
        var prefs = getSharedPreferences("app_preferences", MODE_PRIVATE);
        return prefs.getInt(Utils.MAX_DIST_KEY, 2);
    }

    private void scheduleReload() {
        debounceHandler.removeCallbacks(debounceRunnable);
        debounceRunnable = () -> {
            if (mapLibreMap == null) return;
            var prefs = getSharedPreferences(Utils.PREFS_NAME, MODE_PRIVATE);
            var center = mapLibreMap.getCameraPosition().target;

            if (center == null) return;

            if (prefs.getBoolean(Utils.OFFLINE_ENABLED_KEY, false)) {
                loadCommercesOffline(center.getLatitude(), center.getLongitude(), getMaxDist());
            } else {
                loadCommercesOnline(center.getLatitude(), center.getLongitude(), getMaxDist());
            }
        };

        debounceHandler.postDelayed(debounceRunnable, 50);
    }

    private void printMarker(CommerceEntity c) {
        if (c == null || symbolManager == null) {
            Log.w(Utils.COMMERCE_TAG, "Commerce or symbolManager is null");
            return;
        }

        symbolManager.create(new SymbolOptions()
                .withLatLng(new LatLng(c.getLat(), c.getLng()))
                .withIconImage("marker_11")
                .withIconSize(5.0f)
                .withIconAnchor("bottom")
                .withTextOffset(new Float[]{0f, 1.5f})
        );
    }

    private void clearMarkers() {
        if (symbolManager != null) {
            symbolManager.deleteAll();
        }
    }

    private void loadCommercesOnline(double lat, double lng, double maxDist) {
        var params = Map.<String, Object>of(
                "lat", lat,
                "lng", lng,
                "maxDist", maxDist
        );

        fetchDataService.fetchCommercesOnline(params, commerces -> runOnUiThread(() -> {
            clearMarkers();
            commerces.stream()
                    .map(CommerceEntity::fromDto)
                    .forEach(this::printMarker);
        }), error -> Log.e(Utils.COMMERCE_TAG, "Error: " + error));
    }

    private void loadCommercesOffline(double lat, double lng, double maxDist) {
        executor.execute(() -> {
            var entities = fetchDataService.fetchNearbyCommercesOffline(lat, lng, maxDist);
            runOnUiThread(() -> {
                clearMarkers();
                entities.forEach(this::printMarker);
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
        executor.shutdown();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

}
