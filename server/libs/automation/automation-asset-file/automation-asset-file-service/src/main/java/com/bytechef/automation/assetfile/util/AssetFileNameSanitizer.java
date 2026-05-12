/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.assetfile.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
public final class AssetFileNameSanitizer {

    private static final Pattern PATH_SEPARATOR_PATTERN = Pattern.compile("[/\\\\]");
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("\\p{Cntrl}");
    /**
     * Filenames flow into the {@code Content-Disposition: attachment; filename="..."} response header. RFC 6266
     * recommends backslash-escaping embedded double quotes; the simpler and safer fix is to strip them at sanitize time
     * so no header-construction site has to remember to escape.
     */
    private static final Pattern QUOTE_PATTERN = Pattern.compile("\"");

    private AssetFileNameSanitizer() {
    }

    public static String sanitize(String rawName) {
        if (rawName == null) {
            return "untitled";
        }

        String cleaned = PATH_SEPARATOR_PATTERN.matcher(rawName)
            .replaceAll("");

        cleaned = CONTROL_CHAR_PATTERN.matcher(cleaned)
            .replaceAll("");
        cleaned = QUOTE_PATTERN.matcher(cleaned)
            .replaceAll("");
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFC)
            .trim();

        if (cleaned.length() > 255) {
            cleaned = cleaned.substring(0, 255);
        }

        return cleaned.isEmpty() ? "untitled" : cleaned;
    }
}
