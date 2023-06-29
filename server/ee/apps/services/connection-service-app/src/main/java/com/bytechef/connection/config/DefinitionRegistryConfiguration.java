
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

package com.bytechef.connection.config;

import com.bytechef.hermes.definition.registry.remote.client.service.ConnectionDefinitionServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DefinitionRegistryConfiguration {

    @Bean
    ConnectionDefinitionServiceClient connectionDefinitionServiceClient(
        DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        return new ConnectionDefinitionServiceClient(discoveryClient, objectMapper);
    }
}
