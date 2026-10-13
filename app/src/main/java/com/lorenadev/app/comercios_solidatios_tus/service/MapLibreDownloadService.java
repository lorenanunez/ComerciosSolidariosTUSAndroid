package com.lorenadev.app.comercios_solidatios_tus.service;


import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.BACKEND_URL;
import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.LOCALE_URUGUAY;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import lombok.RequiredArgsConstructor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RequiredArgsConstructor
public final class MapLibreDownloadService {

    private static final String TAG = "MapDownloadService";
    private static final String PMTILES_FILENAME = "uruguay.pmtiles";

    private final OkHttpClient client;
    private final Context context;

    public void downloadEntireRegion(IntConsumer onProgress, Runnable onSuccess, Consumer<String> onError) {
        var url = new HttpUrl.Builder()
                .scheme(BACKEND_URL.getProtocol())
                .host(BACKEND_URL.getHost())
                .port(BACKEND_URL.getPort() == -1 ? BACKEND_URL.getDefaultPort() : BACKEND_URL.getPort())
                .addPathSegments("api/tiles/uruguay.pmtiles")
                .build();
        var request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new PMTilesDownloadCallback(onProgress, onError, onSuccess));
    }

    @RequiredArgsConstructor
    private class PMTilesDownloadCallback implements Callback {

        private final IntConsumer onProgress;
        private final Consumer<String> onError;
        private final Runnable onSuccess;

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            var msg = String.format("Error attempting to download tiles: %s", e.getMessage());
            Log.e(TAG, msg, e);
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
            try (var r = response) {
                if (!r.isSuccessful()) {
                    var msg = String.format(LOCALE_URUGUAY, "Server response was not successful: HTTP %d", r.code());
                    Log.e(TAG, msg);
                    return;
                }

                var finalPath = context.getFilesDir().toPath().resolve(PMTILES_FILENAME);
                var tmpPath = finalPath.resolveSibling(PMTILES_FILENAME + ".tmp");

                var body = r.body();
                var total = body.contentLength();
                var downloaded = 0L;
                var lastPercent = -1;

                try (var in = body.byteStream(); var out = Files.newOutputStream(tmpPath)) {
                    var buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded += read;
                        if (total > 0) {
                            int percent = (int) (downloaded * 100 / total);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                onProgress.accept(percent);
                            }
                        }
                    }
                    out.flush();
                } catch (IOException ex) {
                    FileUtils.deleteQuietly(tmpPath.toFile());
                    var msg = String.format("Error writing pmtiles file: %s", ex.getMessage());
                    Log.e(TAG, msg, ex);
                    onError.accept(msg);
                    return;
                }

                try {
                    Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    FileUtils.deleteQuietly(tmpPath.toFile());
                    var msg = String.format("Couldn't finish pmtiles download: %s", ex.getMessage());
                    Log.e(TAG, msg, ex);
                    onError.accept(msg);
                    return;
                }

                onProgress.accept(100);
                Log.i(TAG, String.format("PMTiles downloaded successfully: %s", finalPath));
                onSuccess.run();
            }

        }
    }

}
