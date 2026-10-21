/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.ConfigurationException;
import java.util.Arrays;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
enum AppIconType {

    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp");

    private static final byte[] JPEG_MAGIC_BYTES = {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };
    private static final byte[] PNG_MAGIC_BYTES = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] RIFF_MAGIC_BYTES = {
        'R', 'I', 'F', 'F'
    };
    private static final byte[] WEBP_MAGIC_BYTES = {
        'W', 'E', 'B', 'P'
    };
    private static final int WEBP_MAGIC_BYTES_OFFSET = 8;

    private final String contentType;
    private final String extension;

    AppIconType(String extension, String contentType) {
        this.contentType = contentType;
        this.extension = extension;
    }

    String getContentType() {
        return contentType;
    }

    String getExtension() {
        return extension;
    }

    static AppIconType detect(byte[] content) {
        if (hasMagicBytes(content, 0, PNG_MAGIC_BYTES)) {
            return PNG;
        }

        if (hasMagicBytes(content, 0, JPEG_MAGIC_BYTES)) {
            return JPEG;
        }

        if (hasMagicBytes(content, 0, RIFF_MAGIC_BYTES) &&
            hasMagicBytes(content, WEBP_MAGIC_BYTES_OFFSET, WEBP_MAGIC_BYTES)) {

            return WEBP;
        }

        throw new ConfigurationException(
            "App icon must be a PNG, JPEG or WebP image", AppErrorType.APP_ICON_INVALID);
    }

    private static boolean hasMagicBytes(byte[] content, int offset, byte[] magicBytes) {
        int end = offset + magicBytes.length;

        return content.length >= end && Arrays.equals(content, offset, end, magicBytes, 0, magicBytes.length);
    }
}
