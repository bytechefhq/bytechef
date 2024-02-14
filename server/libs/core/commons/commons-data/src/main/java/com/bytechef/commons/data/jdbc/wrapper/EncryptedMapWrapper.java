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

package com.bytechef.commons.data.jdbc.wrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * See https://github.com/spring-projects/spring-data-relational/issues/867
 *
 * @author Ivica Cardic
 */
public class EncryptedMapWrapper {

    private Map<String, Object> map = new HashMap<>();

    public EncryptedMapWrapper() {
    }

    public EncryptedMapWrapper(Map<String, ?> map) {
        this.map = new HashMap<>(map);
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public Map<String, Object> getMap() {
        return Collections.unmodifiableMap(map);
    }

    public Object get(String key) {
        return map.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EncryptedMapWrapper that = (EncryptedMapWrapper) o;

        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    public void putAll(Map<String, ?> map) {
        this.map.putAll(map);
    }

    @Override
    public String toString() {
        return "MapWrapper{" + "map=" + map + '}';
    }
}
