/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.config;

import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.ApiCollectionEndpointPublicApiController;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.ApiCollectionPublicApiController;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.EnvironmentModelConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The public API controllers and the converter that binds their {@code X-Environment} header, for {@code @WebMvcTest}s.
 * Each test declares {@code @MockitoBean}s for {@code ApiCollectionFacade} and {@code EnvironmentService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@Import({
    ApiCollectionEndpointPublicApiController.class, ApiCollectionPublicApiController.class,
    EnvironmentModelConverter.class, JacksonConfiguration.class
})
public class ApiCollectionPublicRestTestConfiguration {
}
