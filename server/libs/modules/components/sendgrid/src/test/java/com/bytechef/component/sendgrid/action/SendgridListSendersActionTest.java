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

package com.bytechef.component.sendgrid.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.sendgrid.util.SendgridUtils;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class SendgridListSendersActionTest {

    @Test
    void testPerform(Context mockedContext) {
        List<Map<?, ?>> senders = List.of(Map.of("id", 1, "nickname", "Orders"));

        try (MockedStatic<SendgridUtils> sendgridUtilsMockedStatic = mockStatic(SendgridUtils.class)) {
            sendgridUtilsMockedStatic
                .when(() -> SendgridUtils.getSenders(mockedContext))
                .thenReturn(senders);

            List<Map<?, ?>> result = SendgridListSendersAction.perform(null, null, mockedContext);

            assertEquals(senders, result);
        }
    }
}
