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

package com.bytechef.api.gateway.config;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DiscoveryClientConfiguration {

    @Configuration
    @ConditionalOnProperty(value = "discovery-client.provider", havingValue = "consul")
    @EnableDiscoveryClient
    static class ConsulDiscoveryClientConfiguration {}

    @Configuration
    @ConditionalOnProperty(value = "discovery-client.provider", havingValue = "property")
    @LoadBalancerClients({
        @LoadBalancerClient(
                name = "coordinator-service-app",
                configuration = PropertyDiscoveryClientConfiguration.CoordinatorConfiguration.class),
        @LoadBalancerClient(
                name = "platform-service-app",
                configuration = PropertyDiscoveryClientConfiguration.PlatformConfiguration.class),
        @LoadBalancerClient(
                name = "worker-service-app",
                configuration = PropertyDiscoveryClientConfiguration.WorkerConfiguration.class)
    })
    @EnableConfigurationProperties(DiscoveryClientPropertyProperties.class)
    static class PropertyDiscoveryClientConfiguration {

        static class CoordinatorConfiguration {

            @Bean
            ServiceInstanceListSupplier coordinatorServiceInstanceListSupplier(
                    DiscoveryClientPropertyProperties properties) {
                return new ConfigServiceInstanceListSuppler(
                        properties.getCoordinatorServiceApp().getInstances(), "coordinator-service-app");
            }
        }

        static class PlatformConfiguration {

            @Bean
            ServiceInstanceListSupplier platformServiceInstanceListSupplier(
                    DiscoveryClientPropertyProperties properties) {
                return new ConfigServiceInstanceListSuppler(
                        properties.getPlatformServiceApp().getInstances(), "platform-service-app");
            }
        }

        static class WorkerConfiguration {

            @Bean
            ServiceInstanceListSupplier workerServiceInstanceListSupplier(
                    DiscoveryClientPropertyProperties properties) {
                return new ConfigServiceInstanceListSuppler(
                        properties.getWorkerServiceApp().getInstances(), "worker-service-app");
            }
        }

        static class ConfigServiceInstanceListSuppler implements ServiceInstanceListSupplier {

            private final List<DiscoveryClientPropertyProperties.Instance> instances;
            private final String serviceId;

            ConfigServiceInstanceListSuppler(
                    List<DiscoveryClientPropertyProperties.Instance> instances, String serviceId) {
                this.instances = instances;
                this.serviceId = serviceId;
            }

            @Override
            public String getServiceId() {
                return serviceId;
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(instances.stream()
                        .map(instance -> (ServiceInstance) new DefaultServiceInstance(
                                serviceId + Objects.hash(instance.getHost(), instance.getPort()),
                                serviceId,
                                instance.getHost(),
                                instance.getPort(),
                                false))
                        .toList());
            }
        }
    }
}
