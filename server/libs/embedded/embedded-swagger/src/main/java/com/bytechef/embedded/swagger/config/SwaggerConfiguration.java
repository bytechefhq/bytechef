/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.swagger.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Ivica Cardic
 */
@Configuration("embeddedSwaggerConfiguration")
@Profile("api-docs")
public class SwaggerConfiguration {

    @Bean
    public GroupedOpenApi embeddedByConnectedUserTokeOpenApi() {
        return GroupedOpenApi.builder()
            .group("embedded-by-connected-user-token")
            .displayName("Embedded by Connected User Token API")
            .pathsToMatch("/api/embedded/by-connected-user-token/v1/**")
            .build();
    }

    @Bean
    public GroupedOpenApi embeddedInternalOpenApi() {
        return GroupedOpenApi.builder()
            .group("embedded-internal")
            .displayName("Embedded Internal API")
            .pathsToMatch("/api/embedded/internal/**")
            .build();
    }

    @Bean
    public GroupedOpenApi embeddedPublicOpenApi() {
        return GroupedOpenApi.builder()
            .group("embedded-public")
            .displayName("Embedded Public V1 API")
            .pathsToMatch("/api/embedded/v1/**")
            .build();
    }
}
