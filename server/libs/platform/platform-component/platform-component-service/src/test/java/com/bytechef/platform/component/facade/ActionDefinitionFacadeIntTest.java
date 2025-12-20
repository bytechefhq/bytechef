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

package com.bytechef.platform.component.facade;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.http.client.action.HttpClientGetAction;
import com.bytechef.platform.component.config.ComponentDefinitionIntTestConfiguration;
import com.bytechef.platform.component.config.ComponentRegistryConfigurationSharedMocks;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.util.TokenRefreshHelper;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Igor Beslic
 */
@ComponentScan("com.bytechef.platform.component")
@SpringBootTest(
    classes = {
        PostgreSQLContainerConfiguration.class,
        ComponentDefinitionIntTestConfiguration.class
    })
@ComponentRegistryConfigurationSharedMocks
public class ActionDefinitionFacadeIntTest {

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ActionDefinitionService actionDefinitionService;

    @Autowired
    private ContextFactory contextFactory;

    @Autowired
    private TokenRefreshHelper tokenRefreshHelper;

    @Disabled
    @Test
    public void testExecutePerform() {
        ActionDefinitionFacadeImpl actionDefinitionFacade = new ActionDefinitionFacadeImpl(
            connectionService, actionDefinitionService);

        Object result = actionDefinitionFacade.executePerform(
            "httpClient", 1, HttpClientGetAction.ACTION_DEFINITION.getName(), 1000L, 1000L, 1000L, "123456789",
            Map.of("uri", "https://api.hnb.hr/o/tecajn-eur/v3"), Map.of(), Map.of(), false, ModeType.AUTOMATION);

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(List.class, result);

        List<?> results = (List<?>) result;

        Assertions.assertFalse(results.isEmpty());

        Http.Response response = (Http.Response) actionDefinitionFacade.executePerform(
            "httpClient", 1, HttpClientGetAction.ACTION_DEFINITION.getName(), 1000L, 1000L, 1000L, "123456789",
            Map.of("uri", "https://api.hnb.hr/o/tecajn-eur/v2", "fullResponse", "true"), Map.of(), Map.of(), false,
            ModeType.AUTOMATION);

        Assertions.assertNull(response.getBody());
        Assertions.assertEquals(404, response.getStatusCode());
    }

    @Disabled
    @Test
    public void testExecuteDynamicProperties() {
        // TODO example - sheets/365 - we cant describe row in advance - first pass sheet-id to obtain header row
    }
}
