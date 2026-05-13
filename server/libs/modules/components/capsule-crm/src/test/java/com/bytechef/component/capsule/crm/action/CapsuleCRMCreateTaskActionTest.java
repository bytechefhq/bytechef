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

package com.bytechef.component.capsule.crm.action;

import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.CATEGORY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.DESCRIPTION;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.DETAIL;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.DUE_ON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class CapsuleCRMCreateTaskActionTest extends AbstractCapsuleCRMActionTest {

    @Test
    void testPerform(
        ActionContext mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        mockedParameters = MockParametersFactory.create(propertyStubsMap);

        Object result = CapsuleCRMCreateTaskAction.perform(mockedParameters, null, mockedContext);

        assertEquals(responseMap, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/tasks", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(
            Body.of(Map.of("task", propertyStubsMap), BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(DESCRIPTION, "description");
        propertyStubsMap.put(DUE_ON, new Date());
        propertyStubsMap.put(DETAIL, "detail");
        propertyStubsMap.put(CATEGORY, Map.of("name", "name", "colour", "#ffffff"));

        return propertyStubsMap;
    }
}
