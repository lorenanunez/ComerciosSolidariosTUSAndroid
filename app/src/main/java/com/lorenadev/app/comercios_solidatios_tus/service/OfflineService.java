package com.lorenadev.app.comercios_solidatios_tus.service;

import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.BACKEND_URL;
import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.OFFLINE_KEY;
import static com.lorenadev.app.comercios_solidatios_tus.util.Utils.PMTILES_FILENAME;

import android.content.Context;
import android.service.quicksettings.TileService;
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

    private static final String LOCAL_GLYPHS = "asset://fonts/{fontstack}/{range}.pbf";

    private final Context context;

    public Style.Builder loadLocalMapStyle() {
        return buildStyle("pmtiles://file://" + getPmtilesPath());
    }

    public Style.Builder loadOnlineMapStyle() {
        var base = StringUtils.removeEnd(BACKEND_URL.toString(), "/");
        return buildStyle("pmtiles://" + base + "/api/tiles/uruguay.pmtiles");
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

    @SneakyThrows(IOException.class)
    private Style.Builder buildStyle(String pmtilesUrl) {
        try (var input = context.getAssets().open("style-local.json")) {
            var styleJson = JsonParser.parseString(IOUtils.toString(input, StandardCharsets.UTF_8)).getAsJsonObject();
            styleJson.getAsJsonObject("sources")
                    .getAsJsonObject("openmaptiles")
                    .addProperty("url", pmtilesUrl);
            styleJson.addProperty("glyphs", LOCAL_GLYPHS); // glyphs siempre del APK, online y offline
            Log.d(OFFLINE_KEY, "Style source: " + pmtilesUrl);
            return new Style.Builder().fromJson(styleJson.toString());
        }
    }


    public boolean hasDownloadedRegion() {
        try {
            var tilesService = new PMTilesService(context);
            var path = tilesService.getPMTilesPath();
            var downloaded = (Files.exists(path) && Files.size(getPmtilesPath()) > 0);
            Log.d(OFFLINE_KEY, String.format("Region downloaded: %b", downloaded));
            return downloaded && tilesService.isValidPMTilesFile(path);
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
