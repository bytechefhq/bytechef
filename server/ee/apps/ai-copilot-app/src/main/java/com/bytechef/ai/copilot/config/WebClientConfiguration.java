/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ai.copilot.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class WebClientConfiguration {

    @LoadBalanced
    @Bean
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
