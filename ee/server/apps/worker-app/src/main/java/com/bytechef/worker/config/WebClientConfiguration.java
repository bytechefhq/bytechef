/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.worker.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@EnableRetry
@LoadBalancerClients({
    @LoadBalancerClient("connection-app"), @LoadBalancerClient("scheduler-app")
})
public class WebClientConfiguration {

    @LoadBalanced
    @Bean
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
