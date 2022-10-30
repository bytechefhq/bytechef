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

package com.bytechef.coordinator.config;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DiscoveryClientConfiguration {

    public static final String PLATFORM_SERVICE_APP = "platform-service-app";

    @Configuration
    @ConditionalOnProperty(value = "discovery-client.provider", havingValue = "consul")
    @EnableDiscoveryClient
    static class ConsulDiscoveryClientConfiguration {}

    @Configuration
    @ConditionalOnProperty(value = "discovery-client.provider", havingValue = "property")
    @EnableConfigurationProperties(DiscoveryClientPropertyProperties.class)
    static class PropertyDiscoveryClientConfiguration {

        @Bean
        DiscoveryClient discoveryClient(DiscoveryClientPropertyProperties discoveryClientPropertyProperties) {
            Map<String, List<ServiceInstance>> instances = Map.of(
                    PLATFORM_SERVICE_APP,
                    discoveryClientPropertyProperties.getPlatformServiceApp().getInstances().stream()
                            .map(instance -> (ServiceInstance) new DefaultServiceInstance(
                                    PLATFORM_SERVICE_APP + Objects.hash(instance.getHost(), instance.getPort()),
                                    PLATFORM_SERVICE_APP,
                                    instance.getHost(),
                                    instance.getPort(),
                                    false))
                            .toList());

            return new DiscoveryClient() {

                @Override
                public String description() {
                    return "Coordinator Discovery Client";
                }

                @Override
                public List<ServiceInstance> getInstances(String serviceId) {
                    return instances.get(serviceId);
                }

                @Override
                public List<String> getServices() {
                    return discoveryClientPropertyProperties.getPlatformServiceApp().getInstances().stream()
                            .map(instance ->
                                    PLATFORM_SERVICE_APP + Objects.hash(instance.getHost(), instance.getPort()))
                            .toList();
                }
            };
        }
    }
}
