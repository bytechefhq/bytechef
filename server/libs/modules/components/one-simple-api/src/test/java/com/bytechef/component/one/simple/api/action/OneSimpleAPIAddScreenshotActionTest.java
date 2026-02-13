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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivona Pavela
 */
@ExtendWith(MockContextSetupExtension.class)
class OneSimpleAPIAddScreenshotActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            URL, "https://test.com", HTML, "<html><body> test </body></html>", CUSTOM_CSS, "test_css",
            WAIT, 2, FULL_PAGE, true, FORCE_REFRESH, true, TRANSPARENT_BACKGROUND, true,
            SCREEN_SIZE, CUSTOM_SIZE, WIDTH, 1920, HEIGHT, 1080));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = OneSimpleAPIAddScreenshotAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
        assertEquals("/screenshot", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(
            Http.Body.of(getExpectedBody(), Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
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
