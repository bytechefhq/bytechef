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
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

/**
 * @author Ivica Cardic
 */
public class BinaryItem extends JSONObject {

    public static BinaryItem of(String fileName, String data) {
        BinaryItem binaryItem = new BinaryItem();

        binaryItem.setData(data);
        binaryItem.setExtension(FilenameUtils.getExtension(fileName));
        binaryItem.setName(FilenameUtils.getName(fileName));

        FileNameMap fileNameMap = URLConnection.getFileNameMap();

        binaryItem.setMimeType(fileNameMap.getContentTypeFor(fileName));

        return binaryItem;
    }

    public static BinaryItem of(String fileName, String data, Map<String, ?> map) {
        BinaryItem binaryItem = BinaryItem.of(fileName, data);

        for (String key : map.keySet()) {
            binaryItem.put(key, map.get(key));
        }

        return binaryItem;
    }

    public static BinaryItem of(String json) {
        BinaryItem binaryItem = new BinaryItem();

        JSONObject jsonObject = new JSONObject(json);

        for (String key : jsonObject.keySet()) {
            switch (key) {
                case "data" -> binaryItem.setData(jsonObject.getString("data"));
                case "extension" -> binaryItem.setExtension(jsonObject.getString("extension"));
                case "mimeType" -> binaryItem.setMimeType(jsonObject.getString("mimeType"));
                case "name" -> binaryItem.setName(jsonObject.getString("name"));
                default -> binaryItem.put(key, jsonObject.getString(key));
            }
        }

        return binaryItem;
    }

    public String getData() {
        return getString("data");
    }

    public void setData(String data) {
        put("data", data);
    }

    public String getExtension() {
        return getString("extension");
    }

    public void setExtension(String extension) {
        put("extension", extension);
    }

    public String getMimeType() {
        return getString("mimeType");
    }

    public void setMimeType(String mimeType) {
        put("mimeType", mimeType);
    }

    public String getName() {
        return getString("name");
    }

    public void setName(String name) {
        put("name", name);
    }
}
