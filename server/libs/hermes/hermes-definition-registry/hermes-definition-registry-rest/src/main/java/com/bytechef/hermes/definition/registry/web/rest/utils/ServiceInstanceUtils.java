
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

package com.bytechef.hermes.definition.registry.web.rest.utils;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;

import java.util.Arrays;
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
public class ServiceInstanceUtils {

    public static ServiceInstance filterServiceInstance(List<ServiceInstance> serviceInstances, String componentName) {
        for (ServiceInstance serviceInstance : serviceInstances) {
            Map<String, String> metadataMap = serviceInstance.getMetadata();

            List<String> componentNames = getComponentNames(metadataMap);

            for (String curComponentName : componentNames) {
                if (curComponentName.equalsIgnoreCase(componentName)) {
                    return serviceInstance;
                }
            }
        }

        throw new IllegalStateException("None od worker instances contains component %s ".formatted(componentName));
    }

    public static Set<ServiceInstance> filterServiceInstances(List<ServiceInstance> serviceInstances) {
        Set<String> componentNameInstanceIds = toUniqueInstanceIds(serviceInstances);

        return componentNameInstanceIds.stream()
            .map(instanceId -> serviceInstances.stream()
                .filter(serviceInstance -> Objects.equals(serviceInstance.getInstanceId(), instanceId))
                .findFirst()
                .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private static List<String> getComponentNames(Map<String, String> metadataMap) {
        if (metadataMap.containsKey("componentNames")) {
            String componentNamesString = metadataMap.get("componentNames");

            if (StringUtils.hasText(componentNamesString)) {
                return Arrays.asList(StringUtils.commaDelimitedListToStringArray(componentNamesString));
            }
        }

        return List.of();
    }

    public static String toComponentDefinitionActionUri(ServiceInstance serviceInstance) {
        return "http://%s:%s/api/component-definitions/{componentName}/{componentVersion}/actions/{actionName}"
            .formatted(
                serviceInstance.getHost(), serviceInstance.getPort());
    }

    public static String toComponentDefinitionsUri(ServiceInstance serviceInstance) {
        return toComponentDefinitionsUri(serviceInstance, null, null);
    }

    public static String toComponentDefinitionsUri(
        ServiceInstance serviceInstance, Boolean connectionDefinitions, Boolean connectionInstances) {

        return "http://%s:%s/api/component-definitions?connectionDefinitions=%s&connectionInstances=%s".formatted(
            serviceInstance.getHost(), serviceInstance.getPort(),
            connectionDefinitions == null ? "" : connectionDefinitions,
            connectionInstances == null ? "" : connectionInstances);
    }

    public static String toComponentDefinitionUri(ServiceInstance serviceInstance) {
        return "http://%s:%s/api/component-definitions/{name}/{version}".formatted(
            serviceInstance.getHost(), serviceInstance.getPort());
    }

    public static String toComponentDefinitionVersionsUri(ServiceInstance serviceInstance) {
        return "http://%s:%s/api/component-definitions/{name}".formatted(
            serviceInstance.getHost(), serviceInstance.getPort());
    }

    public static String toConnectionDefinitionsUri(ServiceInstance serviceInstance) {
        return "http://%s:%s/api/connection-definitions/{name}".formatted(
            serviceInstance.getHost(), serviceInstance.getPort());
    }

    public static String toConnectionDefinitionUri(ServiceInstance serviceInstance) {
        return "http://%s:%s/api/connection-definition/{componentName}/{componentVersion}".formatted(
            serviceInstance.getHost(), serviceInstance.getPort());
    }

    private static Set<String> toUniqueInstanceIds(List<ServiceInstance> serviceInstances) {
        Map<String, Set<String>> componentNameInstanceIds = new HashMap<>();

        for (ServiceInstance serviceInstance : serviceInstances) {
            Map<String, String> metadataMap = serviceInstance.getMetadata();

            List<String> componentNames = getComponentNames(metadataMap);

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
