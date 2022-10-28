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

package com.bytechef.hermes.component.test;

import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.impl.FileEntryImpl;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class MockExecutionParameters implements ExecutionParameters {

    private final Map<String, Object> map;

    public MockExecutionParameters() {
        this.map = new HashMap<>();
    }

    public MockExecutionParameters(Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return map.containsKey(key) ? (Boolean) map.get(key) : defaultValue;
    }

    @Override
    public Duration getDuration(String key) {
        String value = getString(key);

        if (value == null) {
            return null;
        }

        return Duration.parse("PT" + value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public FileEntry getFileEntry(String key) {
        return new FileEntryImpl(
                com.bytechef.hermes.file.storage.domain.FileEntry.of((Map<String, String>) map.get(key)));
    }

    @Override
    public Integer getInteger(String key) {
        return (Integer) map.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public FileEntry getFileEntry(Map<String, ?> map, String key) {
        return new FileEntryImpl(
                com.bytechef.hermes.file.storage.domain.FileEntry.of((Map<String, String>) map.get(key)));
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return map.containsKey(key) ? (Integer) map.get(key) : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        return (List<T>) map.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, List<T> defaultValue) {
        return map.containsKey(key) ? (List<T>) map.get(key) : defaultValue;
    }

    @Override
    public long getLong(String key) {
        return ((Number) map.get(key)).longValue();
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return map.containsKey(key) ? ((Number) map.get(key)).longValue() : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String key) {
        return (Map<String, Object>) map.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String key, Map<String, Object> defaultValue) {
        return map.containsKey(key) ? (Map<String, Object>) map.get(key) : defaultValue;
    }

    @Override
    public Object getObject(String key) {
        return map.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getRequiredList(String key) {
        return (List<T>) map.get(key);
    }

    @Override
    public Object getRequiredObject(String key) {
        return map.get(key);
    }

    @Override
    public String getRequiredString(String key) {
        return (String) map.get(key);
    }

    @Override
    public String getString(String key) {
        return (String) map.get(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return map.containsKey(key) ? (String) map.get(key) : defaultValue;
    }

    public MockExecutionParameters set(String key, Object value) {
        map.put(key, value);

        return this;
    }
}
