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

package com.integri.atlas.task.definition.repository.memory;

import com.integri.atlas.task.definition.repository.ExtTaskDefinitionHandlerRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ivica Cardic
 */
public class InMemoryExtTaskDefinitionHandlerRepository implements ExtTaskDefinitionHandlerRepository {

    Map<String, String> taskDefinitionHandlerNameTypeMap = new ConcurrentHashMap<>();

    @Override
    public void create(String name, String type) {
        taskDefinitionHandlerNameTypeMap.putIfAbsent(name, type);
    }

    @Override
    public void delete(String name) {
        taskDefinitionHandlerNameTypeMap.remove(name);
    }

    @Override
    public boolean existByNameAndType(String name, String type) {
        return Objects.equals(taskDefinitionHandlerNameTypeMap.get(name), type);
    }

    @Override
    public Map<String, String> findAll() {
        return taskDefinitionHandlerNameTypeMap;
    }

    @Override
    public List<String> findAllNamesByType(String type) {
        return taskDefinitionHandlerNameTypeMap
            .entrySet()
            .stream()
            .filter(entry -> Objects.equals(entry.getValue(), type))
            .map(Map.Entry::getValue)
            .toList();
    }

    @Override
    public String findTypeByName(String name) {
        return taskDefinitionHandlerNameTypeMap.get(name);
    }

    @Override
    public void update(String name, String type) {
        taskDefinitionHandlerNameTypeMap.put(name, type);
    }
}
