
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

package com.bytechef.commons.discovery.util;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class WorkerDiscoveryUtils {

    public static ServiceInstance filterServiceInstance(
        List<ServiceInstance> serviceInstances, String componentName, ObjectMapper objectMapper) {

        for (ServiceInstance serviceInstance : serviceInstances) {
            Map<String, String> metadataMap = serviceInstance.getMetadata();

            List<String> componentNames = getComponentNames(metadataMap, objectMapper);

            for (String curComponentName : componentNames) {
                if (curComponentName.equalsIgnoreCase(componentName)) {
                    return serviceInstance;
                }
            }
        }

        throw new IllegalStateException("None od worker instances contains component %s ".formatted(componentName));
    }

    public static Set<ServiceInstance> filterServiceInstances(
        List<ServiceInstance> serviceInstances, ObjectMapper objectMapper) {

        Set<String> instanceIds = toUniqueInstanceIds(serviceInstances, objectMapper);

        return instanceIds.stream()
            .map(instanceId -> serviceInstances.stream()
                .filter(serviceInstance -> Objects.equals(serviceInstance.getInstanceId(), instanceId))
                .findFirst()
                .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private static List<String> getComponentNames(Map<String, String> metadataMap, ObjectMapper objectMapper) {
        if (metadataMap.containsKey("components")) {
            String componentsString = metadataMap.get("components");

            if (StringUtils.hasText(componentsString)) {
                List<Map<String, String>> components = JsonUtils.read(
                    componentsString, new TypeReference<>() {}, objectMapper);

                return components.stream()
                    .map(componentMap -> MapUtils.getString(componentMap, "name"))
                    .toList();
            }
        }

        return List.of();
    }

    private static Set<String> toUniqueInstanceIds(List<ServiceInstance> serviceInstances, ObjectMapper objectMapper) {
        Map<String, Set<String>> componentNameInstanceIds = new HashMap<>();

        for (ServiceInstance serviceInstance : serviceInstances) {
            Map<String, String> metadataMap = serviceInstance.getMetadata();

            List<String> componentNames = getComponentNames(metadataMap, objectMapper);

            for (String componentName : componentNames) {
                componentNameInstanceIds.compute(componentName, (key, instanceIds) -> {
                    if (instanceIds == null) {
                        instanceIds = new HashSet<>();
                    }

                    instanceIds.add(serviceInstance.getInstanceId());

                    return instanceIds;
                });
            }
        }

        return componentNameInstanceIds.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }
}
