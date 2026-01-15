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

package com.bytechef.component.heygen.action;

import static com.bytechef.component.heygen.constant.HeyGenConstants.CAPTION;
import static com.bytechef.component.heygen.constant.HeyGenConstants.ENABLE_SHARING;
import static com.bytechef.component.heygen.constant.HeyGenConstants.FOLDER_ID;
import static com.bytechef.component.heygen.constant.HeyGenConstants.TEMPLATE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class HeyGenGenerateVideoFromTemplateActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Response mockedResponse = mock(Response.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(TEMPLATE_ID, "1", FOLDER_ID, "2", CAPTION, false, ENABLE_SHARING, false));

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Map<String, Object> responseBody = Map.of(
            "data", mockedObject);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);

        Object result =
            HeyGenGenerateVideoFromTemplateAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Body body = bodyArgumentCaptor.getValue();
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put(FOLDER_ID, "2");
        expected.put(CAPTION, false);
        expected.put(ENABLE_SHARING, false);

        assertEquals(expected, body.getContent());
    }
}
