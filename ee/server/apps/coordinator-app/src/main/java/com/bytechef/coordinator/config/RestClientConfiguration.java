/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.coordinator.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@EnableRetry
@LoadBalancerClients({
    @LoadBalancerClient("configuration-app"), @LoadBalancerClient("execution-app")
})
public class RestClientConfiguration {

    @LoadBalanced
    @Bean
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
