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

import com.bytechef.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
    "com.bytechef.embedded.configuration.web.rest",
    "com.bytechef.embedded.configuration.web.rest.adapter",
    "com.bytechef.embedded.configuration.web.rest.mapper",
    "com.bytechef.platform.category.web.rest.adapter",
    "com.bytechef.platform.category.web.rest.mapper",
    "com.bytechef.platform.configuration.web.rest.adapter",
    "com.bytechef.platform.configuration.web.rest.mapper",
    "com.bytechef.platform.tag.web.rest.adapter",
    "com.bytechef.platform.tag.web.rest.mapper",
    "com.bytechef.platform.web.rest.mapper"
})
@Configuration
public class IntegrationConfigurationRestTestConfiguration {

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;
}
