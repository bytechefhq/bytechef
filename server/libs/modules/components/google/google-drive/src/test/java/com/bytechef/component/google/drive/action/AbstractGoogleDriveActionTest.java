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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
public abstract class AbstractGoogleDriveActionTest {

    protected ArgumentCaptor<AbstractInputStreamContent> abstractInputStreamContentArgumentCaptor =
        ArgumentCaptor.forClass(AbstractInputStreamContent.class);
    protected ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
    protected ArgumentCaptor<String> fileIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    protected MockedStatic<GoogleServices> googleServicesMockedStatic;
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Drive.Files.Create mockedCreate = mock(Drive.Files.Create.class);
    protected Drive mockedDrive = mock(Drive.class);
    protected Drive.Files.Get mockedGet = mock(Drive.Files.Get.class);
    protected File mockedGoogleFile = mock(File.class);
    protected Drive.Files mockedFiles = mock(Drive.Files.class);
    protected Parameters mockedParameters = mock(Parameters.class);

    @BeforeEach
    public void beforeEach() {
        googleServicesMockedStatic = mockStatic(GoogleServices.class);

        googleServicesMockedStatic
            .when(() -> GoogleServices.getDrive(any(Parameters.class)))
            .thenReturn(mockedDrive);

        when(mockedDrive.files())
            .thenReturn(mockedFiles);
    }

    @AfterEach
    public void afterEach() {
        googleServicesMockedStatic.close();
    }
}
