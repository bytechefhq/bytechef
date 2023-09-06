
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

import com.bytechef.file.storage.domain.FileEntry;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;

public class ContextFileEntryImpl implements Context.FileEntry {

    private final FileEntry fileEntry;

    public ContextFileEntryImpl(String filename, String url) {
        this.fileEntry = new FileEntry(filename, url);
    }

    public ContextFileEntryImpl(FileEntry fileEntry) {
        this.fileEntry = fileEntry;
    }

    @Override
    public String getExtension() {
        return fileEntry.getExtension();
    }

    @Override
    public String getMimeType() {
        return fileEntry.getMimeType();
    }

    @Override
    public String getName() {
        return fileEntry.getName();
    }

    @Override
    public String getUrl() {
        return fileEntry.getUrl();
    }

    @Override
    public String toString() {
        return "ContextFileEntryImpl{" +
            "fileEntry=" + fileEntry +
            '}';
    }

    public static class ContextFileEntryConverter implements Converter<Map<?, ?>, Context.FileEntry> {

        @Override
        public Context.FileEntry convert(Map<?, ?> source) {
            return new ContextFileEntryImpl((String) source.get("name"), (String) source.get("url"));
        }
    }
}
