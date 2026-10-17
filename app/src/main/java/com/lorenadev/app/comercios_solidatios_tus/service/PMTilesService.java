package com.lorenadev.app.comercios_solidatios_tus.service;

import android.content.Context;
import android.util.Log;

import com.lorenadev.app.comercios_solidatios_tus.util.Utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PMTilesService {

    private static final String TAG = "PMTILES_SERVICE";
    private static final byte[] PMTILES_MAGIC = "PMTiles".getBytes(StandardCharsets.US_ASCII);
    private static final int PMTILES_VERSION = 3;
    private static final int HEADER_BYTES = 127;
    private static final long MAX_ROOT_DIRECTORY_END = 16384L;

    private final Context context;

    public boolean isValidPMTilesFile(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }

        try (var in = Files.newInputStream(path)) {
            var header = new byte[HEADER_BYTES];
            var fileSize = Files.size(path);

            IOUtils.readFully(in, header);
            if (fileSize < HEADER_BYTES) {
                return false;
            }

            if (!Arrays.equals(Arrays.copyOf(header, PMTILES_MAGIC.length), PMTILES_MAGIC) || Byte.toUnsignedInt(header[7]) != PMTILES_VERSION) {
                Log.w(TAG, "Local file is not PMTiles v3, ignoring...");
                return false;
            }

            var buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            var offsets = new long[4];
            var lengths = new long[4];

            for (int i = 0, pos = 0; i < offsets.length; i++, pos += 16) {
                offsets[i] = buffer.getLong(pos);
                lengths[i] = buffer.getLong(pos + 8);
            }

            if (!hasValidSections(offsets, lengths, fileSize) || !hasValidHeaderFields(header)) {
                Log.w(TAG, "Invalid PMTiles v3 structure");
                return false;
            }

            return true;
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            return false;
        }
    }

    public Path getPMTilesPath() {
        return context.getFilesDir().toPath().resolve(Utils.PMTILES_FILENAME);
    }

    private boolean hasValidSections(long[] offsets, long[] lengths, long fileSize) {
        for (int i = 0; i < offsets.length; i++) {
            if (!isValidSection(offsets[i], lengths[i], fileSize, i == 0)) {
                return false;
            }
        }

        var rootOffset = offsets[0];
        var rootLength = lengths[0];

        if (rootOffset > MAX_ROOT_DIRECTORY_END || rootLength > MAX_ROOT_DIRECTORY_END - rootOffset) {
            return false;
        }

        for (var i = 0; i < offsets.length; i++) {
            if (lengths[i] == 0) {
                continue;
            }

            var end = offsets[i] + lengths[i];
            for (var j = 0; j < offsets.length; j++) {
                var sideEnd = offsets[j] + lengths[j];
                if (offsets[i] < sideEnd && offsets[j] < end) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isValidSection(long offset, long length, long fileSize, boolean required) {
        if (offset < 0 || length < 0 || offset > fileSize) {
            return false;
        }

        if (required && length == 0) {
            return false;
        }

        if (length == 0) {
            return true;
        }

        return offset >= HEADER_BYTES && length <= fileSize - offset;
    }

    private boolean hasValidHeaderFields(byte[] header) {
        var clustered = Byte.toUnsignedInt(header[96]);
        var internalCompression = Byte.toUnsignedInt(header[97]);
        var tileCompression = Byte.toUnsignedInt(header[98]);
        var tileType = Byte.toUnsignedInt(header[99]);
        var minZoom = Byte.toUnsignedInt(header[100]);
        var maxZoom = Byte.toUnsignedInt(header[101]);

        return clustered <= 1 && internalCompression <= 4 && tileCompression <= 4 && tileType <= 6 && minZoom <= maxZoom;
    }

}
