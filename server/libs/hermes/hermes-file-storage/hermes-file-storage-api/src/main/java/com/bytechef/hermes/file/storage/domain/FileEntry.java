
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.file.storage.domain;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.tika.Tika;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class FileEntry {

    private final String extension;
    private final String mimeType;
    private final String name;
    private final String url;

    public FileEntry(String name, String extension, String mimeType, String url) {
        Assert.notNull(name, "'name' must not be null");
        Assert.notNull(extension, "'extension' must not be null");
        Assert.notNull(mimeType, "'mimeType' must not be null");
        Assert.notNull(url, "'url' must not be null");

        this.extension = extension;
        this.mimeType = mimeType;
        this.name = name;
        this.url = url;
    }

    public FileEntry(String fileName) {
        this(fileName, fileName);
    }

    @SuppressFBWarnings("NP")
    public FileEntry(String filename, String url) {
        Assert.notNull(filename, "'filename' must not be null");
        Assert.notNull(url, "'url' must not be null");

        this.extension = Optional.of(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(filename.lastIndexOf(".") + 1))
            .orElse("");

        Tika tika = new Tika();

        this.mimeType = tika.detect(filename);

        Path path = Paths.get(filename);

        Path fileName = Objects.requireNonNull(path.getFileName());

        this.name = fileName.toString();
        this.url = url;
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

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        FileEntry fileEntry = (FileEntry) o;

        return Objects.equals(extension, fileEntry.extension)
            && Objects.equals(mimeType, fileEntry.mimeType)
            && Objects.equals(name, fileEntry.name)
            && Objects.equals(url, fileEntry.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extension, mimeType, name, url);
    }

    public Map<String, String> toMap() {
        return Map.of(
            "extension", getExtension(),
            "mimeType", getMimeType(),
            "name", getName(),
            "url", getUrl());
    }

    @Override
    public String toString() {
        return "FileEntry{" + "extension='"
            + extension + '\'' + ", mimeType='"
            + mimeType + '\'' + ", name='"
            + name + '\'' + ", url='"
            + url + '\'' + '}';
    }
}
