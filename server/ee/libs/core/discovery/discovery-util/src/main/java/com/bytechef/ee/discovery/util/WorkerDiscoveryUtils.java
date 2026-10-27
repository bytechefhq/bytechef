/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.util;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import tools.jackson.core.type.TypeReference;

/**
 * Routes calls to the worker instances that advertise components in their discovery metadata. Each distinct
 * {@code components} metadata value is parsed once, so a changed registration is picked up as soon as discovery returns
 * it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkerDiscoveryUtils {

    private static final Cache<String, List<String>> COMPONENT_NAMES_CACHE = Caffeine.newBuilder()
        .maximumSize(1_000)
        .build();

    public static Optional<ServiceInstance> fetchServiceInstance(
        List<ServiceInstance> serviceInstances, String componentName) {

        for (ServiceInstance serviceInstance : serviceInstances) {
            Map<String, String> metadataMap = serviceInstance.getMetadata();

            List<String> componentNames = metadataMap == null ? List.of() : getComponentNames(metadataMap);

            for (String curComponentName : componentNames) {
                if (curComponentName.equalsIgnoreCase(componentName)) {
                    return Optional.of(serviceInstance);
                }
            }
        }

        return Optional.empty();
    }

    public static ServiceInstance filterServiceInstance(
        List<ServiceInstance> serviceInstances, String componentName) {

        Optional<ServiceInstance> serviceInstance = fetchServiceInstance(serviceInstances, componentName);

        return serviceInstance.orElseThrow(
            () -> new IllegalArgumentException("No worker instance offers component %s".formatted(componentName)));
    }

    public static Set<ServiceInstance> filterServiceInstances(List<ServiceInstance> serviceInstances) {
        Set<String> instanceIds = toUniqueInstanceIds(serviceInstances);

        return instanceIds.stream()
            .map(instanceId -> serviceInstances.stream()
                .filter(serviceInstance -> Objects.equals(serviceInstance.getInstanceId(), instanceId))
                .findFirst()
                .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private static List<String> getComponentNames(Map<String, String> metadataMap) {
        if (metadataMap.containsKey("components")) {
            String componentsString = metadataMap.get("components");

            if (StringUtils.isNotBlank(componentsString)) {
                return COMPONENT_NAMES_CACHE.get(componentsString, WorkerDiscoveryUtils::readComponentNames);
            }
        }

        return List.of();
    }

    private static List<String> readComponentNames(String componentsString) {
        List<Map<String, String>> components = JsonUtils.read(componentsString, new TypeReference<>() {});

        return CollectionUtils.map(components, componentMap -> MapUtils.getString(componentMap, "name"));
    }

    private static Set<String> toUniqueInstanceIds(List<ServiceInstance> serviceInstances) {
        Map<String, Set<String>> componentNameInstanceIds = new HashMap<>();

        for (ServiceInstance serviceInstance : serviceInstances) {
            Map<String, String> metadataMap = serviceInstance.getMetadata();

            List<String> componentNames = metadataMap == null ? List.of() : getComponentNames(metadataMap);

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
