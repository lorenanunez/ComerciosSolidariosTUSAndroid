package com.lorenadev.app.comercios_solidatios_tus.service;

import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.OFFLINE_KEY;
import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.PMTILES_FILENAME;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.maplibre.android.maps.Style;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class OfflineService {

    private final Context context;

    public Style.Builder loadLocalMapStyle() {
        return new Style.Builder().fromJson(readStyleJsonWithLocalPmtiles());
    }

    @SneakyThrows(IOException.class)
    private String readStyleJsonWithLocalPmtiles() {
        try (var input = context.getAssets().open("style-local.json")) {
            var styleJson = JsonParser.parseString(IOUtils.toString(input, StandardCharsets.UTF_8)).getAsJsonObject();

            styleJson.getAsJsonObject("sources")
                    .getAsJsonObject("openmaptiles")
                    .addProperty("url", getLocalPmtilesUri().toString());

            var response = styleJson.toString();
            Log.d(OFFLINE_KEY, String.format("styleJson: %s", response));
            return response;
        }
    }

    private Path getPmtilesPath() {
        return Paths.get(context.getFilesDir().getAbsolutePath(), PMTILES_FILENAME);
    }


    public boolean hasDownloadedRegion() {
        try {
            var path = getPmtilesPath();
            var downloaded = (Files.exists(path) && Files.size(getPmtilesPath()) > 0);
            Log.d(OFFLINE_KEY, String.format("Region downloaded: %b", downloaded));
            return downloaded;
        } catch (IOException ex) {
            Log.e(OFFLINE_KEY, ex.getMessage(), ex);
            return false;
        }
    }

    public URI getLocalPmtilesUri() {
        var uri = URI.create(String.format("pmtiles://file://%s", getPmtilesPath()));
        Log.d(OFFLINE_KEY, uri.toString());
        return uri;
    }
}
