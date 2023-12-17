/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class FileEntry {

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
        Validate.notNull(filename, "'filename' must not be null");
        Validate.notNull(url, "'url' must not be null");

        this.name = filename.substring(indexOfLastSeparator(filename) + 1);

        if (name.contains(".")) {
            this.extension = name.substring(name.lastIndexOf(".") + 1);

            this.mimeType = MimeTypeUtils.getMimeType(extension);
        }

        this.url = url;
    }

    public FileEntry(String name, String extension, String mimeType, String url) {
        Validate.notNull(name, "'name' must not be null");
        Validate.notNull(url, "'url' must not be null");

        this.name = name;
        this.extension = extension;
        this.mimeType = mimeType;
        this.url = url;
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

    @Override
    public String toString() {
        return "FileEntry{" +
            "name='" + name + '\'' +
            ", url='" + url + '\'' +
            '}';
    }

    private int indexOfLastSeparator(final String fileName) {
        int lastUnixPos = fileName.lastIndexOf(UNIX_NAME_SEPARATOR);
        int lastWindowsPos = fileName.lastIndexOf(WINDOWS_NAME_SEPARATOR);

        return Math.max(lastUnixPos, lastWindowsPos);
    }
}
