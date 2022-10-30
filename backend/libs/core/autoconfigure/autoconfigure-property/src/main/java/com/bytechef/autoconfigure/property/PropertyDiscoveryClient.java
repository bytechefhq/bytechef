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

package com.bytechef.autoconfigure.property;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * @author Ivica Cardic
 */
public class PropertyDiscoveryClient implements DiscoveryClient {

    private Map<String, List<ServiceInstance>> instances;

    public PropertyDiscoveryClient(
            DiscoveryClientPropertyProperties discoveryClientPropertyProperties, String serviceName) {
        Map<String, DiscoveryClientPropertyProperties.Application> applicationMap =
                discoveryClientPropertyProperties.getProperty();

        instances = Map.of(
                serviceName,
                applicationMap.get(serviceName).getInstances().stream()
                        .map(instance -> (ServiceInstance) new DefaultServiceInstance(
                                serviceName + Objects.hash(instance.getHost(), instance.getPort()),
                                serviceName,
                                instance.getHost(),
                                instance.getPort(),
                                false))
                        .toList());
    }

    @Override
    public String description() {
        return "Property Discovery Client";
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        return instances.get(serviceId);
    }

    @Override
    public List<String> getServices() {
        return instances.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(instance -> entry.getKey() + Objects.hash(instance.getHost(), instance.getPort())))
                .toList();
    }
}
