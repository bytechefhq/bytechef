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

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ExecutionParametersImpl implements ExecutionParameters {

    private final TaskExecution taskExecution;

    public ExecutionParametersImpl(TaskExecution taskExecution) {
        this.taskExecution = new TaskExecution(taskExecution);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return taskExecution.getBoolean(key, defaultValue);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return taskExecution.getInteger(key, defaultValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        return taskExecution.get(key, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, List<T> defaultValue) {
        return taskExecution.get(key, List.class, defaultValue);
    }

    @Override
    public String getRequiredString(String key) {
        return taskExecution.getRequiredString(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public FileEntry getFileEntry(String key) {
        return new FileEntryImpl(com.bytechef.hermes.file.storage.domain.FileEntry.of((Map) taskExecution.getMap(key)));
    }

    @Override
    public boolean containsKey(String key) {
        return taskExecution.containsKey(key);
    }

    @Override
    public long getLong(String key) {
        return taskExecution.getLong(key);
    }

    @Override
    public Duration getDuration(String key) {
        return taskExecution.getDuration(key);
    }

    @Override
    public Object getRequiredObject(String key) {
        return taskExecution.getRequired(key);
    }

    @Override
    public String getString(String key) {
        return taskExecution.getString(key);
    }

    @Override
    public Integer getInteger(String key) {
        return taskExecution.getInteger(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public FileEntry getFileEntry(Map<String, ?> map, String key) {
        return new FileEntryImpl(
                com.bytechef.hermes.file.storage.domain.FileEntry.of((Map<String, String>) map.get(key)));
    }

    @Override
    public String getString(String key, String defaultValue) {
        return taskExecution.getString(key, defaultValue);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return taskExecution.getLong(key, defaultValue);
    }

    @Override
    public Map<String, Object> getMap(String key) {
        return taskExecution.getMap(key);
    }

    @Override
    public Map<String, Object> getMap(String key, Map<String, Object> defaultValue) {
        return taskExecution.getMap(key, defaultValue);
    }

    @Override
    public Object getObject(String key) {
        return taskExecution.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getRequiredList(String key) {
        return taskExecution.getRequired(key, List.class);
    }
}
