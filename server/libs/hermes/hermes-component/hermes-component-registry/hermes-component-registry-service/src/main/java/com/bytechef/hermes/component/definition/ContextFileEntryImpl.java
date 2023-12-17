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

package com.bytechef.hermes.component.definition;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ContextFileEntryImpl implements ActionContext.FileEntry {

    private final String extension;
    private final String mimeType;
    private final String name;
    private final String url;

    public ContextFileEntryImpl(FileEntry fileEntry) {
        this(fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getName(), fileEntry.getUrl());
    }

    public ContextFileEntryImpl(String extension, String mimeType, String name, String url) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.name = name;
        this.url = url;
    }

    @Override

    public String getExtension() {
        return extension;
    }

    public FileEntry getFileEntry() {
        return new FileEntry(name, url);
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
        return "ContextFileEntryImpl{" +
            "extension='" + extension + '\'' +
            ", mimeType='" + mimeType + '\'' +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            '}';
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public static class ContextFileEntryConverter implements Converter<Map, ActionContext.FileEntry> {

        @Override
        public ActionContext.FileEntry convert(Map source) {
            return new ContextFileEntryImpl(
                MapUtils.getRequiredString(source, "extension"), MapUtils.getRequiredString(source, "mimeType"),
                MapUtils.getRequiredString(source, "name"), MapUtils.getRequiredString(source, "url"));
        }
    }
}
