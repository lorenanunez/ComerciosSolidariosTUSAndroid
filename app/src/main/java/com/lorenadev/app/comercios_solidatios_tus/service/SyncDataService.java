package com.lorenadev.app.comercios_solidatios_tus.service;

import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.BACKEND_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lorenadev.app.comercios_solidatios_tus.dto.GetCommerceDto;
import com.lorenadev.app.comercios_solidatios_tus.entity.CommerceEntity;
import com.lorenadev.app.comercios_solidatios_tus.util.Utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SyncDataService {

    private static final String SYNC_TAG = "SYNC";

    private final OkHttpClient client;
    private final Gson gson;
    private final AppDatabaseService appDatabaseService;
    private final TypeToken<List<GetCommerceDto>> typeToken;

    private final Consumer<Integer> onComplete;
    private final Consumer<String> onError;

    public SyncDataService(Context context, Consumer<Integer> onComplete, Consumer<String> onError) {
        this.client = new OkHttpClient();
        this.gson = new GsonBuilder()
                .serializeNulls()
                .create();
        this.typeToken = new TypeToken<>() {};
        this.appDatabaseService = AppDatabaseService.getInstance(context);

        this.onComplete = onComplete;
        this.onError = onError;
    }

    public void sync() {
        var url = new HttpUrl.Builder()
                .scheme(BACKEND_URL.getProtocol())
                .host(BACKEND_URL.getHost())
                .addPathSegment("all")
                .build();
        var request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new SyncCallback());
    }

    private class SyncCallback implements Callback {

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            var message = String.format("Failed to fetch commerces for synchronization: %s", e.getMessage());
            Log.e(SYNC_TAG, message);
            onError.accept(message);
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            if (response.isSuccessful()) {
                var body = response.body().string();
                if (StringUtils.isNotEmpty(body)) {
                    var list = gson.fromJson(body, typeToken);
                    if (CollectionUtils.isNotEmpty(list)) {
                        var mapped = list.stream()
                                .map(CommerceEntity::fromDto)
                                .collect(Collectors.toCollection(ArrayList::new)); // toList() requires API level 31 (Android 12)
                        try {
                            appDatabaseService.commerceDao().insertAll(mapped);
                            Log.i(SYNC_TAG, String.format(Utils.LOCALE_URUGUAY, "Insertion of %d commerces appears to be successful", mapped.size()));
                            onComplete.accept(mapped.size());
                        } catch (Exception e) {
                            var message = String.format("Error saving to database: %s", e.getMessage());
                            Log.e(SYNC_TAG, message);
                            onError.accept(message);
                        }
                    } else {
                        var message = "Response array is empty";
                        Log.e(SYNC_TAG, message);
                        onError.accept(message);
                    }
                } else {
                    var message = "Response body is empty";
                    Log.w(SYNC_TAG, message);
                    onError.accept(message);
                }
            } else {
                var message = String.format(Utils.LOCALE_URUGUAY, "Commerce fetching was not successful: %d", response.code());
                Log.e(SYNC_TAG, message);
                onError.accept(message);
            }
        }
    }
}
