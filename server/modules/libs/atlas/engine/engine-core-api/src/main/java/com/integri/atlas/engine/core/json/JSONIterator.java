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

package com.integri.atlas.engine.core.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
class JSONIterator implements Iterator<Map<String, ?>> {

    private final JsonParser jsonParser;
    private final ObjectMapper objectMapper;
    private Map<String, ?> value;

    public JSONIterator(JsonParser jsonParser, ObjectMapper objectMapper) {
        this.jsonParser = jsonParser;
        this.objectMapper = objectMapper;

        try {
            JsonToken jsonToken = jsonParser.nextToken();

            if (jsonToken != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("Provided stream is not a JSON array");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            if (jsonParser.nextToken() == JsonToken.START_OBJECT) {
                value = objectMapper.readValue(jsonParser, new TypeReference<Map<String, Object>>() {});
            } else {
                value = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return value != null;
    }

    @Override
    public Map<String, ?> next() {
        return value;
    }
}
