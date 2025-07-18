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

package com.bytechef.component.bitbucket.action;

import static com.bytechef.component.bitbucket.constant.BitbucketConstants.WORKSPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.bitbucket.util.BitbucketUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Nikolina Spehar
 */
class BitbucketListProjectsActionTest {
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(WORKSPACE, "workspace"));
    private final List<Map<String, Object>> responseList = List.of(Map.of());
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        try (MockedStatic<BitbucketUtils> mockedBitbucketUtils = Mockito.mockStatic(BitbucketUtils.class)) {
            mockedBitbucketUtils.when(
                () -> BitbucketUtils.getPaginationList(
                    contextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(responseList);

            List<Map<String, Object>> result = BitbucketListProjectsAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseList, result);

            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals("/workspaces/workspace/projects", stringArgumentCaptor.getValue());
        }
    }
}
