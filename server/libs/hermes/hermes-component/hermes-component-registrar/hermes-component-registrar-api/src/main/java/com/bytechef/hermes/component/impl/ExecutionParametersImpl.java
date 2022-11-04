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

import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ExecutionParametersImpl implements ExecutionParameters {

    private final WorkflowTask workflowTask;

    @SuppressFBWarnings("EI2")
    public ExecutionParametersImpl(WorkflowTask workflowTask) {
        this.workflowTask = workflowTask;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return workflowTask.getBoolean(key, defaultValue);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return workflowTask.getInteger(key, defaultValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        return workflowTask.get(key, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, List<T> defaultValue) {
        return workflowTask.get(key, List.class, defaultValue);
    }

    @Override
    public String getRequiredString(String key) {
        return workflowTask.getRequiredString(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public FileEntry getFileEntry(String key) {
        return new FileEntryImpl(com.bytechef.hermes.file.storage.domain.FileEntry.of((Map) workflowTask.getMap(key)));
    }

    @Override
    public boolean containsKey(String key) {
        return workflowTask.containsKey(key);
    }

    @Override
    public long getLong(String key) {
        return workflowTask.getLong(key);
    }

    @Override
    public Duration getDuration(String key) {
        return workflowTask.getDuration(key);
    }

    @Override
    public Object getRequiredObject(String key) {
        return workflowTask.getRequired(key);
    }

    @Override
    public String getString(String key) {
        return workflowTask.getString(key);
    }

    @Override
    public Integer getInteger(String key) {
        return workflowTask.getInteger(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public FileEntry getFileEntry(Map<String, ?> map, String key) {
        return new FileEntryImpl(
                com.bytechef.hermes.file.storage.domain.FileEntry.of((Map<String, String>) map.get(key)));
    }

    @Override
    public String getString(String key, String defaultValue) {
        return workflowTask.getString(key, defaultValue);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return workflowTask.getLong(key, defaultValue);
    }

    @Override
    public Map<String, Object> getMap(String key) {
        return workflowTask.getMap(key);
    }

    @Override
    public Map<String, Object> getMap(String key, Map<String, Object> defaultValue) {
        return workflowTask.getMap(key, defaultValue);
    }

    @Override
    public Object getObject(String key) {
        return workflowTask.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getRequiredList(String key) {
        return workflowTask.getRequired(key, List.class);
    }
}
