/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.mailerlite.action;

import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.GROUP_ID;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.SUBSCRIBER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class MailerLiteAddSubscriberToGroupActionTest extends AbstractMailerLiteActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            SUBSCRIBER_ID, "subscriber_id1",
            GROUP_ID, "id1"));
    private final Map<String, Object> responseMap = mock(Map.class);

    @Test
    void perform() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Object result = MailerLiteAddSubscriberToGroupAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);
    }
}
