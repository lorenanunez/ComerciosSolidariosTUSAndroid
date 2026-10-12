package com.lorenadev.app.comercios_solidatios_tus.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.lorenadev.app.comercios_solidatios_tus.util.Utils;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Response;

public class MapTilesService {

    private void downloadResourcesToLocalStorage(String segment) {
        try {
            var client = new HttpUrl.Builder()
                    .scheme(Utils.BACKEND_URL.getProtocol())
                    .host(Utils.BACKEND_URL.getHost())
                    .port(Utils.BACKEND_URL.getPort())
                    .addPathSegment("api")
                    .addPathSegment(segment)
                    .build();


        } catch (Exception e) {
            Log.e("MPTiles", e.getMessage());
        }
    }

    public void downloadTiles() {

    }

    private class DownloadResourcesCallback implements Callback {

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {

        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

        }

    }
}
