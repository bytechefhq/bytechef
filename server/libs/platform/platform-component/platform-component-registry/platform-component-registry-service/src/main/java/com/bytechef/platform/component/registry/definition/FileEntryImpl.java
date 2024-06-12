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

package com.bytechef.platform.component.registry.definition;

import com.bytechef.file.storage.domain.FileEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Ivica Cardic
 */
public class FileEntryImpl implements com.bytechef.component.definition.FileEntry {

    private String extension;
    private String mimeType;
    private String name;
    private String url;

    private FileEntryImpl() {
    }

    public FileEntryImpl(FileEntry fileEntry) {
        this(fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getName(), fileEntry.getUrl());
    }

    public FileEntryImpl(String extension, String mimeType, String name, String url) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.name = name;
        this.url = url;
    }

    @Override

    public String getExtension() {
        return extension;
    }

    @JsonIgnore
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
        return "FileEntryImpl{" +
            "extension='" + extension + '\'' +
            ", mimeType='" + mimeType + '\'' +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            '}';
    }
}
