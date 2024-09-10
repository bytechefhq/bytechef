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
import static com.bytechef.component.trello.constant.TrelloConstants.ID_LIST;
import static com.bytechef.component.trello.constant.TrelloConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.test.component.properties.ParametersFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class TrelloCreateCardActionTest extends AbstractTrelloActionTest {

    private static final Object mockedObject = mock(Object.class);

    @Test
    void testPerform() {
        Parameters parameters = ParametersFactory.createParameters(
            Map.of(ID_LIST, "abc", NAME, "new card", DESC, "new card description"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        assertEquals(mockedObject, TrelloCreateCardAction.perform(parameters, parameters, mockedActionContext));

        Object[] query = queryArgumentCaptor.getValue();

        assertEquals(List.of(ID_LIST, "abc", NAME, "new card", DESC, "new card description"), Arrays.asList(query));
    }
}
