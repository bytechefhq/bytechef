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

package com.bytechef.component.google.docs.action;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.docs.v1.Docs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
public abstract class AbstractGoogleDocsActionTest {

    protected MockedStatic<GoogleServices> googleServicesMockedStatic;
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Docs mockedDocs = mock(Docs.class);
    protected Parameters mockedParameters = mock(Parameters.class);

    @BeforeEach
    public void beforeEach() {
        googleServicesMockedStatic = mockStatic(GoogleServices.class);

        when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("accessToken");

        googleServicesMockedStatic
            .when(() -> GoogleServices.getDocs(mockedParameters))
            .thenReturn(mockedDocs);
    }

    @AfterEach
    public void afterEach() {
        googleServicesMockedStatic.close();
    }
}
