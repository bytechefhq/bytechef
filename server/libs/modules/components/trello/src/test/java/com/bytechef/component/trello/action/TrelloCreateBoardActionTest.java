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

package com.bytechef.component.trello.action;

import static com.bytechef.component.trello.constant.TrelloConstants.DESC;
import static com.bytechef.component.trello.constant.TrelloConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.component.definition.Parameters;
import com.bytechef.test.component.properties.ParametersFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class TrelloCreateBoardActionTest extends AbstractTrelloActionTest {

    @Test
    void testPerform() {
        Parameters parameters = ParametersFactory.createParameters(
            Map.of(NAME, "new board", DESC, "new board description"));

        assertNull(TrelloCreateBoardAction.perform(parameters, parameters, mockedActionContext));

        Object[] query = queryArgumentCaptor.getValue();

        assertEquals(List.of(NAME, "new board", DESC, "new board description"), Arrays.asList(query));
    }
}
