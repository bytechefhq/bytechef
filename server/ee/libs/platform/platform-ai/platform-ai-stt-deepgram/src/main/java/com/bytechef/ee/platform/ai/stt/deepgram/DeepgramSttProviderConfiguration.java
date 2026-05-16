/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.stt.deepgram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class DeepgramSttProviderConfiguration {

    @Bean
    RestClient deepgramSttRestClient(
        @Value("${bytechef.ai.stt.deepgram.base-url:https://api.deepgram.com}") String baseUrl) {

        return RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
}
