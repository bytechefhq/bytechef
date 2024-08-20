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

package com.bytechef.component.monday.action;

import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.monday.util.MondayUtils;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
abstract class AbstractMondayActionTest {

    protected ActionContext mockedActionContext = mock(ActionContext.class);
    protected MockedStatic<MondayUtils> mondayUtilsMockedStatic;
    protected Map<String, Object> responseMap = Map.of("data", Map.of(ID, "abc"));

    @BeforeEach
    void beforeEach() {
        mondayUtilsMockedStatic = mockStatic(MondayUtils.class);

        mondayUtilsMockedStatic.when(() -> MondayUtils.executeGraphQLQuery(any(ActionContext.class), anyString()))
            .thenReturn(responseMap);
    }

    @AfterEach
    public void afterEach() {
        mondayUtilsMockedStatic.close();
    }
}
