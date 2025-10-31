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

package com.bytechef.static_resources.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Igor Beslic
 */
@Configuration
@Profile("prod")
public class StaticResourcesWebConfiguration implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(StaticResourcesWebConfiguration.class);

    private final Resources resources;

    public StaticResourcesWebConfiguration(ApplicationProperties applicationProperties) {
        this.resources = applicationProperties.getResources();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        ResourceHandlerRegistration resourceHandlerRegistration = registry.addResourceHandler("/**", "*");

        resourceHandlerRegistration.addResourceLocations(resources.getWeb());

        if (logger.isInfoEnabled()) {
            logger.debug("Serving static web content at {}", resources.getWeb());
        }
    }
}
