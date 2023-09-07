
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

package com.bytechef.file.storage.domain;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import com.bytechef.commons.util.MimeTypeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class FileEntry {

    private String context;
    private String name;
    private String url;

    private FileEntry() {
    }

    @SuppressFBWarnings("NP")
    public FileEntry(String context, String filename, String url) {
        Assert.notNull(context, "'context' must not be null");
        Assert.notNull(filename, "'filename' must not be null");
        Assert.notNull(url, "'url' must not be null");

        this.context = context;

        Path path = Paths.get(filename);

        Path fileNamePath = Objects.requireNonNull(path.getFileName());

        this.name = fileNamePath.toString();
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

    public String getContext() {
        return context;
    }

    @JsonIgnore
    public String getExtension() {
        String extension = null;

        if (name.contains(".")) {
            extension = name.substring(name.lastIndexOf(".") + 1);
        }

        return extension;
    }

    @JsonIgnore
    public String getMimeType() {
        return MimeTypeUtils.getMimeType(getExtension());
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> toMap() {
        return Map.of("context", context, "name", name, "url", url);
    }

    @Override
    public String toString() {
        return "FileEntry{" +
            "context='" + context + '\'' +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            '}';
    }
}
