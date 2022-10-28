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

package com.bytechef.config;

import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Ivica Cardic
 */
@Configuration
public class RSocketConfiguration {

    @Bean
    RSocketRequester rSocketRequester(
            RSocketStrategies rSocketStrategies, Flux<List<LoadbalanceTarget>> loadBalancedTargets) {
        return RSocketRequester.builder()
                .rsocketConnector(
                        rSocketConnector -> rSocketConnector.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2))))
                .rsocketStrategies(rSocketStrategies)
                .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                .transports(loadBalancedTargets, new RoundRobinLoadbalanceStrategy());
    }

    @Bean
    public Flux<List<LoadbalanceTarget>> loadBalancedTargets(DiscoveryClient discoveryClient) {
        return Mono.fromSupplier(() -> discoveryClient.getInstances(DiscoveryClientConfiguration.PLATFORM_SERVICE_APP))
                .repeatWhen(longFlux -> longFlux.delayElements(Duration.ofSeconds(2)))
                .map(this::toLoadBalanceTarget);
    }

    private List<LoadbalanceTarget> toLoadBalanceTarget(List<ServiceInstance> rSocketServers) {
        return rSocketServers.stream()
                .map(serviceInstance -> LoadbalanceTarget.from(
                        serviceInstance.getHost() + serviceInstance.getPort(),
                        WebsocketClientTransport.create(
                                HttpClient.from(TcpClient.create()
                                        .host(serviceInstance.getHost())
                                        .port(serviceInstance.getPort())),
                                "/rsocket")))
                .collect(Collectors.toList());
    }
}
