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

package com.bytechef.hermes.descriptor.repository.memory;

import com.bytechef.hermes.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ivica Cardic
 */
public class InMemoryExtTaskDescriptorHandlerRepository implements ExtTaskDescriptorHandlerRepository {

    Map<String, Map<String, Set<Float>>> taskDescriptorHandlerNameTypeMap = new ConcurrentHashMap<>();

    @Override
    public void create(String name, float version, String type) {
        taskDescriptorHandlerNameTypeMap.compute(name, (nameKey, typeValuesMap) -> {
            if (typeValuesMap == null) {
                typeValuesMap = new HashMap<>();
            }

            Set<Float> values = typeValuesMap.computeIfAbsent(type, typeKey -> new HashSet<>());

            values.add(version);

            return typeValuesMap;
        });
    }

    @Override
    public void delete(String name, float version) {
        if (taskDescriptorHandlerNameTypeMap.containsKey(name)) {
            Map<String, Set<Float>> typeVersionsMap = taskDescriptorHandlerNameTypeMap.get(name);

            for (Map.Entry<String, Set<Float>> entry : typeVersionsMap.entrySet()) {
                Set<Float> versions = entry.getValue();

                versions.remove(version);
            }
        }
    }

    @Override
    public boolean existByNameAndVersionAndType(String name, float version, String type) {
        boolean exists = false;

        if (taskDescriptorHandlerNameTypeMap.containsKey(name)) {
            Map<String, Set<Float>> typeVersionsMap = taskDescriptorHandlerNameTypeMap.get(name);

            if (typeVersionsMap.containsKey(type)) {
                exists = typeVersionsMap.get(type).stream().anyMatch(curVersion -> curVersion == version);
            }
        }

        return exists;
    }

    @Override
    public Map<String, Map<String, Set<Float>>> findAll() {
        return taskDescriptorHandlerNameTypeMap;
    }

    @Override
    public List<NameVersions> findAllNamesByType(String type) {
        return taskDescriptorHandlerNameTypeMap.entrySet().stream()
                .filter(entry -> {
                    Map<String, Set<Float>> typeVersions = entry.getValue();

                    return typeVersions.entrySet().stream()
                            .anyMatch(typeVersionsEntry -> Objects.equals(typeVersionsEntry.getKey(), type));
                })
                .map(entry -> {
                    Map<String, Set<Float>> typeVersionsMap = entry.getValue();

                    return new NameVersions(entry.getKey(), typeVersionsMap.get(type));
                })
                .toList();
    }

    @Override
    public String findTypeByNameAndVersion(String name, float version) {
        Map<String, Set<Float>> typeVersionsMap = taskDescriptorHandlerNameTypeMap.get(name);

        return typeVersionsMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(version))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
