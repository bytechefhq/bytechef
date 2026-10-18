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

package com.bytechef.component.ai.typesafe.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class TypeSafeUtilsTest {

    @Test
    @SuppressWarnings("unchecked")
    void testGetModelOptions(Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get("/v1/models"))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "models", List.of(
                        Map.of("name", "jev-1.13.0", "description", "Stable"),
                        Map.of("name", "jev-latest"))));

        List<Option<String>> options = TypeSafeUtils.getModelOptions(null, null, Map.of(), "", mockedContext);

        assertEquals(
            List.of(
                option("jev-latest", "jev-latest"), option("jev-preview", "jev-preview"),
                option("jev-1.13.0", "jev-1.13.0")),
            options);
    }
}
