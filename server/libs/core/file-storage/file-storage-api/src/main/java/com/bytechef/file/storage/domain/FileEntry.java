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

package com.bytechef.file.storage.domain;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class FileEntry {

    private static final String SPLITTER = "_;_";
    private static final char UNIX_NAME_SEPARATOR = '/';
    private static final char WINDOWS_NAME_SEPARATOR = '\\';

    private String extension;
    private String mimeType;
    private String name;
    private String url;

    private FileEntry() {
    }

    public FileEntry(Map<String, ?> source) {
        this(MapUtils.getRequiredString(source, "name"), MapUtils.getRequiredString(source, "url"));
    }

    public FileEntry(String filename, String url) {
        Assert.notNull(filename, "'filename' must not be null");
        Assert.notNull(url, "'url' must not be null");

        this.name = filename.substring(indexOfLastSeparator(filename) + 1);

        int lastDotIndex = name.lastIndexOf(".");

        if (lastDotIndex > 0) {
            this.extension = name.substring(lastDotIndex + 1);

            this.mimeType = MimeTypeUtils.getMimeType(extension);
        }

        this.url = url;
    }

    public FileEntry(String name, String extension, String mimeType, String url) {
        Assert.notNull(name, "'name' must not be null");
        Assert.notNull(url, "'url' must not be null");

        this.name = name;
        this.extension = extension;
        this.mimeType = mimeType;
        this.url = url;
    }

    public static boolean isFileEntryMap(Map<?, ?> map) {
        return map.containsKey("extension") && map.containsKey("mimeType") &&
            map.containsKey("name") && map.containsKey("url");
    }

    public static FileEntry parse(String id) {
        String decodedString = new String(EncodingUtils.base64Decode(id), StandardCharsets.UTF_8);

        String[] parts = decodedString.split(SPLITTER);

        if (parts.length != 4) {
            throw new IllegalArgumentException(
                "Invalid FileEntry id format: expected exactly 4 parts but got " + parts.length);
        }

        String extension = parts[0].isEmpty() ? null : parts[0];
        String mimeType = parts[1].isEmpty() ? null : parts[1];

        return new FileEntry(parts[2], extension, mimeType, parts[3]);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileEntry fileEntry = (FileEntry) o;

        return Objects.equals(name, fileEntry.name) && Objects.equals(url, fileEntry.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String toId() {
        String string = String.join(
            SPLITTER, Objects.toString(extension, ""), Objects.toString(mimeType, ""), name, url);

        return EncodingUtils.base64EncodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "FileEntry{" +
            "extension='" + extension + '\'' +
            ", mimeType='" + mimeType + '\'' +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            '}';
    }

    private int indexOfLastSeparator(final String fileName) {
        int lastUnixPos = fileName.lastIndexOf(UNIX_NAME_SEPARATOR);
        int lastWindowsPos = fileName.lastIndexOf(WINDOWS_NAME_SEPARATOR);

        return Math.max(lastUnixPos, lastWindowsPos);
    }
}
