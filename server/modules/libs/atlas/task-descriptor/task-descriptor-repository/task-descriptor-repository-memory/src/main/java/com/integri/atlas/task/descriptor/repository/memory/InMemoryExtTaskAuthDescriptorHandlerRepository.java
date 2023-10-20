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

package com.integri.atlas.task.descriptor.repository.memory;

import com.integri.atlas.task.descriptor.repository.ExtTaskAuthDescriptorHandlerRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ivica Cardic
 */
public class InMemoryExtTaskAuthDescriptorHandlerRepository implements ExtTaskAuthDescriptorHandlerRepository {

    Map<String, String> taskAuthDescriptorHandlerTaskNameTypeMap = new ConcurrentHashMap<>();

    @Override
    public void create(String taskName, String type) {
        taskAuthDescriptorHandlerTaskNameTypeMap.putIfAbsent(taskName, type);
    }

    @Override
    public void delete(String taskName) {
        taskAuthDescriptorHandlerTaskNameTypeMap.remove(taskName);
    }

    @Override
    public boolean existByTaskNameAndType(String taskName, String type) {
        return Objects.equals(taskAuthDescriptorHandlerTaskNameTypeMap.get(taskName), type);
    }

    @Override
    public Map<String, String> findAll() {
        return taskAuthDescriptorHandlerTaskNameTypeMap;
    }

    @Override
    public List<String> findAllTaskNamesByType(String type) {
        return taskAuthDescriptorHandlerTaskNameTypeMap
            .entrySet()
            .stream()
            .filter(entry -> Objects.equals(entry.getValue(), type))
            .map(Map.Entry::getKey)
            .toList();
    }

    @Override
    public String findTypeByTaskName(String taskName) {
        return taskAuthDescriptorHandlerTaskNameTypeMap.get(taskName);
    }
}
