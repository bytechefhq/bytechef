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

import static com.bytechef.component.monday.constant.MondayConstants.GROUP_ID;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.ITEM_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.monday.util.MondayUtils;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class MondayCreateItemActionTest extends AbstractMondayActionTest {

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredString(GROUP_ID))
            .thenReturn("group");
        when(mockedParameters.getRequiredString(ITEM_NAME))
            .thenReturn("name");

        Object result = MondayCreateItemAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(Map.of(ID, "abc"), result);

        mondayUtilsMockedStatic.verify(() -> MondayUtils.executeGraphQLQuery(mockedActionContext, "mutation{create_item(board_id: board, group_id: \"group\", item_name: \"name\"){id name}}"));
    }
}
