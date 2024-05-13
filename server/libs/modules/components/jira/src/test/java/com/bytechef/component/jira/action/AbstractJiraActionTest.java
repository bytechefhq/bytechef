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

package com.bytechef.component.jira.action;

import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public abstract class AbstractJiraActionTest {

    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Http.Executor mockedExecutor = mock(Http.Executor.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected Http.Response mockedResponse = mock(Http.Response.class);
    protected Map<String, Object> responseMap = Map.of("key", "value");
}
