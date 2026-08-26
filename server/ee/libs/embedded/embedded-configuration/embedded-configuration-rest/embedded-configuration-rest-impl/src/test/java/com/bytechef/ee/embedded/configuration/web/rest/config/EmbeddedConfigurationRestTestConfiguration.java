/*
 * Copyright 2025 ByteChef
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

package com.bytechef.ee.embedded.configuration.web.rest.config;

import com.bytechef.jackson.config.JacksonConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Test sources share the scanned {@code com.bytechef.ee.embedded.configuration.web.rest} package with the production
 * controllers, so the exclude filter keeps the classes nested inside a sibling test out of this context.
 * {@code WebhookTriggerTestApiControllerAuthorizationTest.Config} is the one that matters: it is a
 * {@code @SpringBootConfiguration} contributing its own {@code WebhookTriggerTestApiController} bean, which otherwise
 * lands here beside the scanned controller of the same type and leaves the request mapping ambiguous.
 *
 * <p>
 * The pattern stops at nested classes on purpose -- the top-level configurations this scan also reaches, this class
 * among them, contribute beans the contexts need. It matches {@code Test$} rather than the {@code IntTest$} of the
 * equivalent filters in {@code embedded-configuration-service}, because the nested configuration to keep out lives in a
 * class named {@code ...AuthorizationTest}.
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.ee.embedded.configuration.web.rest",
        "com.bytechef.ee.embedded.configuration.web.rest.adapter",
        "com.bytechef.ee.embedded.configuration.web.rest.mapper",
        "com.bytechef.platform.configuration.web.rest.adapter",
        "com.bytechef.platform.configuration.web.rest.mapper",
        "com.bytechef.web.rest.mapper"
    },
    excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*Test\\$.*"))
@Configuration
@Import(JacksonConfiguration.class)
public class EmbeddedConfigurationRestTestConfiguration {
}
