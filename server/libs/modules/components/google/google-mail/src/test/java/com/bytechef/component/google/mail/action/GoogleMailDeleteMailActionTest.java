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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class GoogleMailDeleteMailActionTest extends AbstractGoogleMailActionTest {

    @SuppressWarnings("unused")
    @Test
    void testPerform() throws IOException {
        Parameters parameters = MockParametersFactory.create(Map.of(ID, "id"));
        try (MockedStatic<GoogleMailUtils> googleMailUtilsMockedStatic = mockStatic(GoogleMailUtils.class)) {
            GoogleMailDeleteMailAction.perform(parameters, parameters, mockedActionContext);
        }
    }
}
