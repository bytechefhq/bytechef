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

package com.bytechef.platform.workflow.task.dispatcher.test.task.handler;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ivica Cardic
 */
public class TestVarTaskHandler<T, V> implements TaskHandler<Object> {

    private final Map<String, T> valueMap = new ConcurrentHashMap<>();
    private final Consumer<Map<String, T>, String, V> valuesMapBiConsumer;

    public TestVarTaskHandler(Consumer<Map<String, T>, String, V> valuesMapBiConsumer) {
        this.valuesMapBiConsumer = valuesMapBiConsumer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object handle(TaskExecution taskExecution) {
        Map<String, ?> parametersMap = taskExecution.getParameters();

        V value = (V) parametersMap.get("value");

        valuesMapBiConsumer.accept(valueMap, taskExecution.getName(), (V) parametersMap.get("value"));

        return value;
    }

    public T get(String key) {
        return valueMap.get(key);
    }

    @FunctionalInterface
    public interface Consumer<T, U, V> {

        void accept(T t, U u, V v);
    }
}
