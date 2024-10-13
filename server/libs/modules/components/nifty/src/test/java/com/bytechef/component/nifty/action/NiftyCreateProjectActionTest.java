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

package com.bytechef.component.nifty.action;

import static com.bytechef.component.nifty.constant.NiftyConstants.ACCESS_TYPE;
import static com.bytechef.component.nifty.constant.NiftyConstants.DESCRIPTION;
import static com.bytechef.component.nifty.constant.NiftyConstants.NAME;
import static com.bytechef.component.nifty.constant.NiftyConstants.TEMPLATE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Mayank Madan
 */
class NiftyCreateProjectActionTest extends AbstractNiftyActionTest {

    private final Map<String, Object> parameterMap = Map.of(NAME, "name", DESCRIPTION, "description", ACCESS_TYPE,
        "public", TEMPLATE_TYPE, "custom");
    private final Parameters mockedParameters = MockParametersFactory.create(parameterMap);

    @Test
    void testPerform() {
        when(mockedResponse.getBody(any(TypeReference.class))).thenReturn(parameterMap);
        Object result = NiftyCreateProjectAction.perform(mockedParameters, mockedParameters, mockedContext);
        assertEquals(parameterMap, result);
        Body body = bodyArgumentCaptor.getValue();
        assertEquals(parameterMap, body.getContent());
    }
}
