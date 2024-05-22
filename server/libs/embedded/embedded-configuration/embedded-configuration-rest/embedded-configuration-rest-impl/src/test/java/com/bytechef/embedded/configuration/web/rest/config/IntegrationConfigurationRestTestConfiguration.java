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

package com.bytechef.embedded.configuration.web.rest.config;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.embedded.configuration.facade.ConnectedUserFacade;
import com.bytechef.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.embedded.configuration.service.ConnectedUserService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
    "com.bytechef.embedded.configuration.web.rest",
    "com.bytechef.embedded.configuration.web.rest.adapter",
    "com.bytechef.embedded.configuration.web.rest.mapper",
    "com.bytechef.platform.configuration.web.rest.adapter",
    "com.bytechef.platform.configuration.web.rest.mapper",
})
@Configuration
@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public class IntegrationConfigurationRestTestConfiguration {

    private final ObjectMapper objectMapper;

    @MockBean
    private ConnectedUserFacade connectedUserFacade;

    @MockBean
    private ConnectedUserService connectedUserService;

    @MockBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockBean
    private IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public IntegrationConfigurationRestTestConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    JsonUtils jsonUtils() {
        return new JsonUtils() {
            {
                objectMapper = IntegrationConfigurationRestTestConfiguration.this.objectMapper;
            }
        };
    }

    @Bean
    MapUtils mapUtils() {
        return new MapUtils() {
            {
                objectMapper = IntegrationConfigurationRestTestConfiguration.this.objectMapper;
            }
        };
    }
}
