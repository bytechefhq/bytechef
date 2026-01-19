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

package com.bytechef.component.zoominfo.action;

import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.CONTACT_OUTPUT_PROPERTY;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.DEPARTMENT;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.EMAIL;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.FIRST_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.FULL_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.JOB_TITLE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.LAST_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PAGE_NUMBER;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class ZoominfoSearchContactActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final Response mockedResponse = mock(Response.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(EMAIL, "http://www.example.com", FULL_NAME, "Test Test",
            FIRST_NAME, "Test", LAST_NAME, "Test", JOB_TITLE, "developer", DEPARTMENT, "IT",
            COMPANY_NAME, "Company"));

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("data", List.of(CONTACT_OUTPUT_PROPERTY), "meta", Map.of("totalResults", 1)));

        Object result = ZoominfoSearchContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(List.of(CONTACT_OUTPUT_PROPERTY), result);

        Object[] expectedQueryParameters = {
            PAGE_SIZE, 25, PAGE_NUMBER, 1
        };

        assertArrayEquals(expectedQueryParameters, queryArgumentCaptor.getValue());
    }
}
