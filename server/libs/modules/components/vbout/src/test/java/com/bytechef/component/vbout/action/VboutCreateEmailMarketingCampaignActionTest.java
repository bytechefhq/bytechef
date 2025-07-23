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

import static com.bytechef.component.vbout.constant.VboutConstants.BODY;
import static com.bytechef.component.vbout.constant.VboutConstants.FROM_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.FROM_NAME;
import static com.bytechef.component.vbout.constant.VboutConstants.IS_DRAFT;
import static com.bytechef.component.vbout.constant.VboutConstants.IS_SCHEDULED;
import static com.bytechef.component.vbout.constant.VboutConstants.LISTS;
import static com.bytechef.component.vbout.constant.VboutConstants.NAME;
import static com.bytechef.component.vbout.constant.VboutConstants.REPLY_TO;
import static com.bytechef.component.vbout.constant.VboutConstants.SCHEDULED_DATETIME;
import static com.bytechef.component.vbout.constant.VboutConstants.SUBJECT;
import static com.bytechef.component.vbout.constant.VboutConstants.TYPE;
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
class VboutCreateEmailMarketingCampaignActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);

    @Test
    void testPerform() {
        Parameters parameters = MockParametersFactory.create(
            Map.ofEntries(
                entry(NAME, "test"),
                entry(SUBJECT, "Subject"),
                entry(FROM_EMAIL, "from@test.com"),
                entry(FROM_NAME, "from name"),
                entry(REPLY_TO, "replyto@test.com"),
                entry(BODY, "This is a body."),
                entry(TYPE, "standard"),
                entry(IS_SCHEDULED, true),
                entry(SCHEDULED_DATETIME, "2025-07-22"),
                entry(IS_DRAFT, true),
                entry(LISTS, List.of("1", "2", "3"))));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Object result = VboutCreateEmailMarketingCampaignAction.perform(parameters, parameters, mockedContext);

        assertNull(result);

        Object[] query = queryArgumentCaptor.getValue();
        assertEquals(
            List.of(
                NAME, "test",
                SUBJECT, "Subject",
                FROM_EMAIL, "from@test.com",
                FROM_NAME, "from name",
                REPLY_TO, "replyto@test.com",
                BODY, "This is a body.",
                TYPE, "standard",
                IS_SCHEDULED, true,
                SCHEDULED_DATETIME, "2025-07-22",
                IS_DRAFT, true,
                LISTS, List.of("1", "2", "3")),
            Arrays.stream(query)
                .map(v -> v instanceof Object[] arr ? List.of(arr) : v)
                .toList());
    }
}
