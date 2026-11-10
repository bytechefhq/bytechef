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

package com.bytechef.component.pubmed.connection;

import static com.bytechef.component.pubmed.constant.PubMedConstants.API_KEY;
import static com.bytechef.component.pubmed.constant.PubMedConstants.EMAIL;
import static com.bytechef.component.pubmed.constant.PubMedConstants.TOOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PubMedConnectionTest {

    private final Context mockedContext = mock(Context.class);

    @Test
    void testApplyWithAllParameters() {
        Parameters connectionParameters = MockParametersFactory.create(
            Map.of(API_KEY, "key1", TOOL, "bytechef", EMAIL, "dev@example.com"));

        ApplyResponse applyResponse = PubMedConnection.apply(connectionParameters, mockedContext);

        assertEquals(
            Map.of(API_KEY, List.of("key1"), TOOL, List.of("bytechef"), EMAIL, List.of("dev@example.com")),
            applyResponse.getQueryParameters());
    }

    @Test
    void testApplyOmitsBlankParameters() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(API_KEY, "key1");
        parameters.put(TOOL, " ");

        Parameters connectionParameters = MockParametersFactory.create(parameters);

        ApplyResponse applyResponse = PubMedConnection.apply(connectionParameters, mockedContext);

        assertEquals(Map.of(API_KEY, List.of("key1")), applyResponse.getQueryParameters());
    }
}
