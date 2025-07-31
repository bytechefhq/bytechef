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

package com.bytechef.component.vbout.action;

import static com.bytechef.component.vbout.constant.VboutConstants.CONFIRMATION_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.CONFIRMATION_MESSAGE;
import static com.bytechef.component.vbout.constant.VboutConstants.EMAIL_SUBJECT;
import static com.bytechef.component.vbout.constant.VboutConstants.ERROR_MESSAGE;
import static com.bytechef.component.vbout.constant.VboutConstants.FROM_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.FROM_NAME;
import static com.bytechef.component.vbout.constant.VboutConstants.NAME;
import static com.bytechef.component.vbout.constant.VboutConstants.NOTIFY_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.REPLY_TO;
import static com.bytechef.component.vbout.constant.VboutConstants.SUCCESS_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.SUCCESS_MESSAGE;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class VboutCreateEmailListActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);

    @Test
    void testPerform() {
        Parameters parameters = MockParametersFactory.create(
            Map.ofEntries(
                entry(NAME, "test"),
                entry(EMAIL_SUBJECT, "test@test.com"),
                entry(REPLY_TO, "replyto@test.com"),
                entry(FROM_EMAIL, "from@test.com"),
                entry(FROM_NAME, "from name"),
                entry(NOTIFY_EMAIL, "notify@test.com"),
                entry(SUCCESS_EMAIL, "subscription@test.com"),
                entry(SUCCESS_MESSAGE, "This is a success."),
                entry(ERROR_MESSAGE, "This is an error."),
                entry(CONFIRMATION_EMAIL, "confirmation@test.com"),
                entry(CONFIRMATION_MESSAGE, "confirmation@test.com")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Object result = VboutCreateEmailListAction.perform(parameters, parameters, mockedContext);

        assertNull(result);

        Object[] query = queryArgumentCaptor.getValue();
        assertEquals(
            List.of(
                NAME, "test",
                EMAIL_SUBJECT, "test@test.com",
                REPLY_TO, "replyto@test.com",
                FROM_EMAIL, "from@test.com",
                FROM_NAME, "from name",
                NOTIFY_EMAIL, "notify@test.com",
                SUCCESS_EMAIL, "subscription@test.com",
                SUCCESS_MESSAGE, "This is a success.",
                ERROR_MESSAGE, "This is an error.",
                CONFIRMATION_EMAIL, "confirmation@test.com",
                CONFIRMATION_MESSAGE, "confirmation@test.com"),
            Arrays.asList(query));
    }
}
