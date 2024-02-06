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

package com.bytechef.component.google.drive.action;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 */
public abstract class AbstractGoogleDriveActionTest {

    protected MockedStatic<GoogleServices> mockedGoogleServices;
    protected Drive mockedDrive = mock(Drive.class);
    protected Drive.Files mockedFiles = mock(Drive.Files.class);
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Parameters mockedParameters = mock(Parameters.class);

    @BeforeEach
    public void beforeEach() {
        mockedGoogleServices = mockStatic(GoogleServices.class);

        when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("accessToken");

        mockedGoogleServices.when(() -> GoogleServices.getDrive(mockedParameters))
            .thenReturn(mockedDrive);

        when(mockedDrive.files())
            .thenReturn(mockedFiles);
    }

    @AfterEach
    public void afterEach() {
        verify(mockedDrive, times(1))
            .files();

        mockedGoogleServices.close();
    }
}
