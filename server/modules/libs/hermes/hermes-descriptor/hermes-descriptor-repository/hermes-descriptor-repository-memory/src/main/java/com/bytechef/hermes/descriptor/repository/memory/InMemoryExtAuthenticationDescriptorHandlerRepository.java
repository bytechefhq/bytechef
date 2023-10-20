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

import com.bytechef.hermes.descriptor.repository.ExtAuthenticationDescriptorHandlerRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ivica Cardic
 */
public class InMemoryExtAuthenticationDescriptorHandlerRepository
        implements ExtAuthenticationDescriptorHandlerRepository {

    Map<String, String> authenticationDescriptorHandlerTaskNameTypeMap = new ConcurrentHashMap<>();

    @Override
    public void create(String taskName, String type) {
        authenticationDescriptorHandlerTaskNameTypeMap.putIfAbsent(taskName, type);
    }

    @Override
    public void delete(String taskName) {
        authenticationDescriptorHandlerTaskNameTypeMap.remove(taskName);
    }

    @Override
    public boolean existByTaskNameAndType(String taskName, String type) {
        return Objects.equals(authenticationDescriptorHandlerTaskNameTypeMap.get(taskName), type);
    }

    @Override
    public Map<String, String> findAll() {
        return authenticationDescriptorHandlerTaskNameTypeMap;
    }

    @Override
    public List<String> findAllByType(String type) {
        return authenticationDescriptorHandlerTaskNameTypeMap.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue(), type))
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public String findTypeByTaskName(String taskName) {
        return authenticationDescriptorHandlerTaskNameTypeMap.get(taskName);
    }
}
