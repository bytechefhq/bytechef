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

package com.bytechef.atlas.worker.config;

import static com.bytechef.commons.util.MemoizationUtils.memoize;

import com.bytechef.atlas.worker.task.handler.DynamicTaskHandlerProvider;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerProvider;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.util.MapUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskHandlerRegistryConfiguration {

    @Bean
    TaskHandlerRegistry taskHandlerRegistry(
        Map<String, TaskHandler<?>> taskHandlerMap,
        @Autowired(required = false) TaskHandlerProvider taskHandlerProvider,
        @Autowired(required = false) List<DynamicTaskHandlerProvider> dynamicTaskHandlerFactories) {

        return new TaskHandlerRegistryImpl(
            memoize(
                () -> MapUtils.concat(
                    taskHandlerMap,
                    taskHandlerProvider == null ? Map.of() : taskHandlerProvider.getTaskHandlerMap())),
            dynamicTaskHandlerFactories == null ? List.of() : dynamicTaskHandlerFactories);
    }

    private record TaskHandlerRegistryImpl(
        Supplier<Map<String, TaskHandler<?>>> taskHandlerMapSupplier,
        List<DynamicTaskHandlerProvider> dynamicTaskHandlerFactories) implements TaskHandlerRegistry {

        @Override
        public TaskHandler<?> getTaskHandler(String type) {
            TaskHandler<?> taskHandler;

            Map<String, TaskHandler<?>> taskHandlerMap = taskHandlerMapSupplier.get();

            if (taskHandlerMap.containsKey(type)) {
                taskHandler = taskHandlerMap.get(type);
            } else {
                taskHandler = dynamicTaskHandlerFactories.stream()
                    .map(dynamicTaskHandlerProvider -> dynamicTaskHandlerProvider.getTaskHandler(type))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow();
            }

            return taskHandler;
        }
    }
}
