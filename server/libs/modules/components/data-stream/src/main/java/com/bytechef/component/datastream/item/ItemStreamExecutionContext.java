/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.datastream.item;

import com.bytechef.component.definition.datastream.ExecutionContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Set;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public class ItemStreamExecutionContext implements ExecutionContext {

    private final org.springframework.batch.item.ExecutionContext executionContext;

    @SuppressFBWarnings("EI")
    public ItemStreamExecutionContext(org.springframework.batch.item.ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public void putString(String key, @Nullable String value) {
        executionContext.put(key, value);
    }

    @Override
    public void putLong(String key, long value) {
        executionContext.put(key, value);
    }

    @Override
    public void putInt(String key, int value) {
        executionContext.put(key, value);
    }

    @Override
    public void putDouble(String key, double value) {
        executionContext.put(key, value);
    }

    @Override
    public void put(String key, @Nullable Object value) {
        executionContext.put(key, value);
    }

    @Override
    public boolean isDirty() {
        return executionContext.isDirty();
    }

    @Override
    public String getString(String key) {
        return executionContext.getString(key);
    }

    @Override
    public String getString(String key, String defaultString) {
        return executionContext.getString(key, defaultString);
    }

    @Override
    public long getLong(String key) {
        return executionContext.getLong(key);
    }

    @Override
    public long getLong(String key, long defaultLong) {
        return executionContext.getLong(key, defaultLong);
    }

    @Override
    public int getInt(String key) {
        return executionContext.getInt(key);
    }

    @Override
    public int getInt(String key, int defaultInt) {
        return executionContext.getInt(key, defaultInt);
    }

    @Override
    public double getDouble(String key) {
        return executionContext.getDouble(key);
    }

    @Override
    public double getDouble(String key, double defaultDouble) {
        return executionContext.getDouble(key, defaultDouble);
    }

    @Override
    @Nullable
    public Object get(String key) {
        return executionContext.get(key);
    }

    @Override
    @Nullable
    public <V> V get(String key, Class<V> type) {
        return executionContext.get(key, type);
    }

    @Override
    @Nullable
    public <V> V get(String key, Class<V> type, @Nullable V defaultValue) {
        return executionContext.get(key, type, defaultValue);
    }

    @Override
    public boolean isEmpty() {
        return executionContext.isEmpty();
    }

    @Override
    public void clearDirtyFlag() {
        executionContext.clearDirtyFlag();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return executionContext.entrySet();
    }

    @Override
    public Map<String, Object> toMap() {
        return executionContext.toMap();
    }

    @Override
    public boolean containsKey(String key) {
        return executionContext.containsKey(key);
    }

    @Override
    @Nullable
    public Object remove(String key) {
        return executionContext.remove(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return executionContext.containsValue(value);
    }

    @Override
    public boolean equals(Object o) {
        return executionContext.equals(o);
    }

    @Override
    public int hashCode() {
        return executionContext.hashCode();
    }

    @Override
    public String toString() {
        return executionContext.toString();
    }

    @Override
    public int size() {
        return executionContext.size();
    }

}
