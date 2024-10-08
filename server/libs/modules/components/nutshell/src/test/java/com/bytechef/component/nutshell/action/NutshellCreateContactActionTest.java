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

package com.bytechef.component.nutshell.action;

import static com.bytechef.component.nutshell.constant.NutshellConstants.DESCRIPTION;
import static com.bytechef.component.nutshell.constant.NutshellConstants.EMAIL;
import static com.bytechef.component.nutshell.constant.NutshellConstants.NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.nutshell.util.NutshellUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class NutshellCreateContactActionTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(NAME, "full name", DESCRIPTION, "some description", EMAIL, "test@mail.com"));

    @Test
    void testPerform() {
        try (MockedStatic<NutshellUtils> nutshellUtilsMockedStatic = mockStatic(NutshellUtils.class)) {

            NutshellCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

            nutshellUtilsMockedStatic.verify(
                () -> NutshellUtils.createEntityBasedOnType(mockedParameters, mockedContext, false), times(1));
        }
    }
}
