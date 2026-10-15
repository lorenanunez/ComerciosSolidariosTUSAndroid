package com.lorenadev.app.comercios_solidatios_tus.service;

import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.DOWNLOAD_TAG;
import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.PMTILES_FILENAME;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

            return styleJson.toString();
        }
    }

    private Path getPmtilesPath() {
        return Paths.get(context.getFilesDir().getAbsolutePath(), PMTILES_FILENAME);
    }

    @SneakyThrows(IOException.class)
    public boolean hasDownloadedRegion() {
        var path = getPmtilesPath();
        return Files.exists(path) && Files.size(getPmtilesPath()) > 0;
    }

    public URI getLocalPmtilesUri() {
        return URI.create(String.format("pmtiles://file://%s", getPmtilesPath()));
    }
}
