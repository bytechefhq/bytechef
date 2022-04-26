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

package com.integri.atlas.file.storage;

import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

/**
 * @author Ivica Cardic
 */
public class FileEntry {

    private String extension;
    private String mimeType;
    private String name;
    private String url;

    private FileEntry() {}

    public static FileEntry of(Map<String, String> map) {
        FileEntry fileEntry = new FileEntry();

        fileEntry.extension = map.get("extension");
        fileEntry.mimeType = map.get("mimeType");
        fileEntry.name = map.get("name");
        fileEntry.url = map.get("url");

        return fileEntry;
    }

    public static FileEntry of(String fileName) {
        FileEntry fileEntry = new FileEntry();

        fileEntry.setExtension(FilenameUtils.getExtension(fileName));

        Tika tika = new Tika();

        fileEntry.setMimeType(tika.detect(fileName));

        fileEntry.setName(FilenameUtils.getName(fileName));
        fileEntry.setUrl(fileName);

        return fileEntry;
    }

    public static FileEntry of(String fileName, String url) {
        FileEntry fileEntry = new FileEntry();

        fileEntry.setExtension(FilenameUtils.getExtension(fileName));

        Tika tika = new Tika();

        fileEntry.setMimeType(tika.detect(fileName));

        fileEntry.setName(FilenameUtils.getName(fileName));
        fileEntry.setUrl(url);

        return fileEntry;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
