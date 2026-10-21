/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest.config;

import com.bytechef.ee.automation.app.public_.web.rest.AppApiCollectionBindingApiController;
import com.bytechef.ee.automation.app.public_.web.rest.AppApiController;
import com.bytechef.ee.automation.app.public_.web.rest.AppWorkflowBindingApiController;
import com.bytechef.ee.automation.app.public_.web.rest.EnvironmentModelConverter;
import com.bytechef.ee.automation.app.public_.web.rest.WorkspaceConnectionApiController;
import com.bytechef.jackson.config.JacksonConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The management controllers the app loop's lookups go through, and the converter that binds their
 * {@code X-Environment} header, for {@code @WebMvcTest}s. Each test declares {@code @MockitoBean}s for
 * {@code AppApiCollectionFacade}, {@code AppFacade}, {@code AppWorkflowFacade} and {@code EnvironmentService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@Import({
    JacksonConfiguration.class, AppApiCollectionBindingApiController.class, AppApiController.class,
    AppWorkflowBindingApiController.class,
    EnvironmentModelConverter.class, WorkspaceConnectionApiController.class
})
public class AppManagementRestTestConfiguration {
}
