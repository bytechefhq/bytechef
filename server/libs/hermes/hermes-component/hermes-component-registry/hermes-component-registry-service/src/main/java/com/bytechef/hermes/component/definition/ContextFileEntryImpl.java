
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.file.storage.domain.FileEntry;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;

public class ContextFileEntryImpl implements Context.FileEntry {

    private final String extension;
    private final String mimeType;
    private final String name;
    private final String url;

    public ContextFileEntryImpl(String extension, String mimeType, String name, String url) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.name = name;
        this.url = url;
    }

    public ContextFileEntryImpl(FileEntry fileEntry) {
        this(fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getName(), fileEntry.getUrl());
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "ContextFileEntry{" +
            "extension='" + extension + '\'' +
            ", mimeType='" + mimeType + '\'' +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            '}';
    }

    public static class ContextFileEntryConverter implements Converter<Map<?, ?>, Context.FileEntry> {

        @Override
        public Context.FileEntry convert(Map<?, ?> source) {
            return new ContextFileEntryImpl(
                (String) source.get("extension"),
                (String) source.get("mimeType"),
                (String) source.get("name"),
                (String) source.get("url"));
        }
    }
}
