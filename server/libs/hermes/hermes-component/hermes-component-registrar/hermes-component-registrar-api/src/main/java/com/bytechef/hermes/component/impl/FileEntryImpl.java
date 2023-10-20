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

package com.bytechef.hermes.component.impl;

import com.bytechef.hermes.component.FileEntry;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FileEntryImpl implements FileEntry {
    private final com.bytechef.hermes.file.storage.domain.FileEntry fileEntry;

    public FileEntryImpl(com.bytechef.hermes.file.storage.domain.FileEntry fileEntry) {
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
    public Map<String, String> toMap() {
        return fileEntry.toMap();
    }
}
