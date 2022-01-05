/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.engine.core.binary;

import com.integri.atlas.engine.core.MapObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

/**
 * @author Ivica Cardic
 */
public class Binary extends MapObject {

    private static final String BINARY = "BINARY";
    private static final String DATA = "data";
    private static final String EXTENSION = "extension";
    private static final String MIME_TYPE = "mimeType";
    private static final String NAME = "name";
    private static final String TYPE = "__type__";

    private Binary() {
        put(TYPE, BINARY);
    }

    private Binary(Binary binary) {
        super(binary);
    }

    private Binary(Map<String, Object> map) {
        super(map);
    }

    public static Binary of(Binary binary) {
        return validate(new Binary(binary));
    }

    public static Binary of(Map<String, Object> map) {
        return validate(new Binary(map));
    }

    public static Binary of(String fileName, String data) {
        Binary binary = new Binary();

        binary.setData(data);
        binary.setExtension(FilenameUtils.getExtension(fileName));
        binary.setName(FilenameUtils.getName(fileName));

        Tika tika = new Tika();

        binary.setMimeType(tika.detect(fileName));

        return validate(binary);
    }

    public static Binary of(String fileName, String data, Map<String, ?> map) {
        Binary binary = of(fileName, data);

        for (String key : map.keySet()) {
            binary.put(key, map.get(key));
        }

        return validate(binary);
    }

    public String getData() {
        return (String) get(DATA);
    }

    public void setData(String data) {
        put(DATA, data);
    }

    public String getExtension() {
        return (String) get(EXTENSION);
    }

    public void setExtension(String extension) {
        put(EXTENSION, extension);
    }

    public String getMimeType() {
        return (String) get(MIME_TYPE);
    }

    public void setMimeType(String mimeType) {
        put(MIME_TYPE, mimeType);
    }

    public String getName() {
        return (String) get(NAME);
    }

    public void setName(String name) {
        put(NAME, name);
    }

    private static Binary validate(Binary binary) {
        Set<String> keys = binary.keySet();

        if (
            !keys.containsAll(List.of(TYPE, DATA, EXTENSION, MIME_TYPE, NAME)) &&
            !Objects.equals(binary.get(TYPE), BINARY)
        ) {
            throw new IllegalStateException("Binary does not contain all required fields.");
        }

        return binary;
    }
}
