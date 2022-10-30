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

package com.bytechef.commons.json;

import com.bytechef.commons.exception.ProcessingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author Ivica Cardic
 */
class JsonIterator implements Iterator<Map<String, ?>> {

    private final JsonParser jsonParser;
    private final ObjectMapper objectMapper;
    private Map<String, ?> value;
    private JsonToken lastJsonToken = null;

    public JsonIterator(JsonParser jsonParser, ObjectMapper objectMapper) {
        this.jsonParser = jsonParser;
        this.objectMapper = objectMapper;

        try {
            lastJsonToken = jsonParser.nextToken();

            if (lastJsonToken != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("Provided stream is not a JSON array");
            }
        } catch (IOException ioException) {
            throw new ProcessingException("Unable to read json stream", ioException);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            lastJsonToken = jsonParser.nextToken();

            if (lastJsonToken == JsonToken.START_OBJECT) {
                value = objectMapper.readValue(jsonParser, new TypeReference<Map<String, Object>>() {});

                return true;
            }

            return false;
        } catch (Exception exception) {
            throw new ProcessingException("Unable to read json stream", exception);
        }
    }

    @Override
    public Map<String, ?> next() throws NoSuchElementException {
        if (lastJsonToken == JsonToken.END_ARRAY) {
            throw new NoSuchElementException();
        }

        return value;
    }
}
