/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.json.item;

import java.net.FileNameMap;
import java.net.URLConnection;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

/**
 * @author Ivica Cardic
 */
public class BinaryItem extends JSONObject {

    private String data;
    private String extension;
    private String mimeType;
    private String name;

    public static BinaryItem of(String fileName, String data) {
        BinaryItem binaryItem = new BinaryItem();

        binaryItem.setData(data);
        binaryItem.setExtension(FilenameUtils.getExtension(fileName));
        binaryItem.setName(fileName);

        FileNameMap fileNameMap = URLConnection.getFileNameMap();

        binaryItem.setMimeType(fileNameMap.getContentTypeFor(fileName));

        return binaryItem;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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
}
