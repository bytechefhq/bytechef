/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql.config;

import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayModelFacade;
import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiGatewayModelFacade;
import com.bytechef.ee.platform.ai.gateway.catalog.AiGatewayModelCatalogReconciler;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Aggregator annotation that wires the AI Gateway model service/facade beans into the GraphQL slice test context as
 * Mockito mocks, so each {@code @Test} can stub behavior on autowired mocks.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {
    AiGatewayModelCatalogReconciler.class, AiGatewayModelFacade.class, WorkspaceAiGatewayModelFacade.class
})
public @interface AiGatewayGraphQlConfigurationSharedMocks {
}
