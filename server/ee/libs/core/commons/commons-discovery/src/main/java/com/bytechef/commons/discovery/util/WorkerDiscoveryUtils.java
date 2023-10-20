
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.commons.discovery.util;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.ServiceInstance;
import org.apache.commons.lang3.StringUtils;

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

            if (StringUtils.isNotBlank(componentsString)) {
                List<Map<String, String>> components = JsonUtils.read(
                    componentsString, new TypeReference<>() {}, objectMapper);

                return CollectionUtils.map(components, componentMap -> MapUtils.getString(componentMap, "name"));
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
