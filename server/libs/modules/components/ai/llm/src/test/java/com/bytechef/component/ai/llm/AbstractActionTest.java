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

package com.bytechef.component.ai.llm;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Monika Kušter
 * @author Marko Kriskovic
 */
public abstract class AbstractActionTest {

    protected Parameters mockedParameters = mock(Parameters.class);
    protected ActionContext mockedActionContext = mock(ActionContext.class);

    @BeforeEach
    void beforeEach() {
        when(mockedParameters.getString(TOKEN)).thenReturn("TOKEN");
    }
}
