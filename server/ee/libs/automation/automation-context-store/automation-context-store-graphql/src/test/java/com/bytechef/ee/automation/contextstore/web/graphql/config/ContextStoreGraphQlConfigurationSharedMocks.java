/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.web.graphql.config;

import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Aggregator annotation that wires the Context Store service/facade beans into the GraphQL slice test context as
 * Mockito mocks. Mirrors the {@code McpProjectGraphQl} pattern so each {@code @Test} can stub behavior on autowired
 * mocks.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {
    ContextStoreService.class, ContextStoreSourceFacade.class, ContextStoreSourceService.class,
    EnvironmentService.class, WorkspaceContextStoreFacade.class, WorkspaceContextStoreSourceFacade.class,
    WorkspaceContextStoreSourceService.class
})
public @interface ContextStoreGraphQlConfigurationSharedMocks {
}
