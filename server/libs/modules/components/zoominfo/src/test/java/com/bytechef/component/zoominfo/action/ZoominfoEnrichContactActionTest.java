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

import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_ID;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.EMAIL;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.EXTERNAL_URL;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.FIRST_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.FULL_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.JOB_TITLE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.LAST_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.OUTPUT_FIELDS;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PERSON_ID;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PHONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class ZoominfoEnrichContactActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Object mockedObject = mock(Object.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.ofEntries(
            Map.entry(PERSON_ID, 1), Map.entry(FULL_NAME, "test test"), Map.entry(FIRST_NAME, "test"),
            Map.entry(LAST_NAME, "test"), Map.entry(EMAIL, "http://www.example.com"),
            Map.entry(PHONE, "+123456789"), Map.entry(JOB_TITLE, "developer"),
            Map.entry(EXTERNAL_URL, "http://www.url.com"), Map.entry(COMPANY_ID, 2),
            Map.entry(COMPANY_NAME, "Company"), Map.entry(OUTPUT_FIELDS, List.of("id", "firstName", "email"))));

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = ZoominfoEnrichContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);
    }
}
