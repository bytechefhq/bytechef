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

package com.bytechef.component.one.simple.api.action;

import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.CUSTOM_CSS;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.CUSTOM_SIZE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FORCE_REFRESH;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FULL_PAGE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.HEIGHT;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.HTML;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.SCREEN_SIZE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TRANSPARENT_BACKGROUND;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.URL;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.WAIT;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.WIDTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivona Pavela
 */
class OneSimpleAPIAddScreenshotActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            URL, "https://test.com",
            HTML, "<html><body> test </body></html>",
            CUSTOM_CSS, "test_css",
            WAIT, 2,
            FULL_PAGE, true,
            FORCE_REFRESH, true,
            TRANSPARENT_BACKGROUND, true,
            SCREEN_SIZE, CUSTOM_SIZE,
            WIDTH, 1920,
            HEIGHT, 1080));
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Http mockedHttp = mock(Http.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of("key", "value");

    @Test
    void testPerform() {
        when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> httpFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Object result = OneSimpleAPIAddScreenshotAction.perform(
            mockedParameters, mockedParameters, mockedActionContext);
        assertEquals(responseMap, result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();
        assertNotNull(capturedFunction);

        assertEquals("/screenshot", stringArgumentCaptor.getValue());

        Http.Configuration configuration = configurationBuilderArgumentCaptor.getValue()
            .build();
        Http.ResponseType responseType = configuration.getResponseType();
        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());

        assertEquals(Http.Body.of(getExpectedBody(), Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());

    }

    private Map<String, Object> getExpectedBody() {
        return Map.of(
            URL, "https://test.com",
            HTML, "<html><body> test </body></html>",
            CUSTOM_CSS, "test_css",
            WAIT, 2,
            FULL_PAGE, true,
            FORCE_REFRESH, true,
            TRANSPARENT_BACKGROUND, true,
            SCREEN_SIZE, "1920x1080");
    }
}
