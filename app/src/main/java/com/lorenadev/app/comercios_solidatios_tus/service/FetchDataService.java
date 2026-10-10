package com.lorenadev.app.comercios_solidatios_tus.service;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lorenadev.app.comercios_solidatios_tus.dto.GetCommerceDto;
import com.lorenadev.app.comercios_solidatios_tus.entity.CommerceEntity;
import com.lorenadev.app.comercios_solidatios_tus.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FetchDataService {

    private final OkHttpClient client;
    private final Gson gson;
    private final TypeToken<List<GetCommerceDto>> typeToken;
    private final AppDatabaseService appDatabaseService;

    public FetchDataService(Context context) {
        this.client = new OkHttpClient();
        this.gson = new GsonBuilder()
                .serializeNulls()
                .create();
        this.typeToken = new TypeToken<>() {
        };
        this.appDatabaseService = AppDatabaseService.getInstance(context);
    }

    public void fetchCommercesOnline(Map<String, Object> params, Consumer<List<GetCommerceDto>> onSuccess, Consumer<String> onError) {
        try {
            var builder = Objects.requireNonNull(HttpUrl.get(Utils.BACKEND_URL)).newBuilder();
            params.forEach((k, v) -> builder.addQueryParameter(k, String.valueOf(v)));
            var request = new Request.Builder().url(builder.build()).build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    onError.accept(String.format("Failed to fetch commerces: %s", e.getMessage()));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        var json = response.body().string();
                        var commerces = gson.fromJson(json, typeToken);
                        onSuccess.accept(commerces);
                    } else {
                        onError.accept(String.format(Utils.LOCALE_URUGUAY, "HTTP: %d", response.code()));
                    }
                }

            });
        } catch (NullPointerException e) {
            Log.e(Utils.COMMERCE_TAG, String.format("Failed to create backend URL: %s", e.getMessage()));
            onError.accept(String.format("Error fetching commerces: %s", e.getMessage()));
        }
    }

    public List<CommerceEntity> fetchNearbyCommercesOffline(double userLat, double userLng, double distanceInKm) {
        try {

            double deltaLat = distanceInKm / 111.0;
            double deltaLng = distanceInKm / (110.0 * Math.cos(Math.toRadians(userLat)));

            double latMin = userLat - deltaLat;
            double latMax = userLat + deltaLat;
            double lngMin = userLng - deltaLng;
            double lngMax = userLng + deltaLng;

            var candidates = appDatabaseService.commerceDao().findInBoundingBox(latMin, latMax, lngMin, lngMax);
            var nearby = new ArrayList<CommerceEntity>();

            double distanceInMeters = distanceInKm * 1000;

            candidates.forEach(entity -> {
                var result = new float[1];
                Location.distanceBetween(userLat, userLng, entity.getLat(), entity.getLng(), result);
                if (result[0] <= distanceInMeters) {
                    nearby.add(entity);
                }
            });

            return nearby;
        } catch (Exception ex) {
            Log.e(Utils.COMMERCE_TAG, String.format("Error finding nearby commerces: %s", ex.getMessage()));
            return Collections.emptyList();
        }
    }

}
