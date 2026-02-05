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

package com.bytechef.component.image.helper.action;

import static com.bytechef.component.image.helper.constant.ImageHelperConstants.DEGREE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.image.helper.util.ImageHelperUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class ImageHelperRotateImageActionTest {

    private final ArgumentCaptor<String> extenstionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<BufferedImage> bufferedImageArgumentCaptor =
        ArgumentCaptor.forClass(BufferedImage.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor =
        ArgumentCaptor.forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final File mockedFile = mock(File.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testPerform() throws IOException {
        BufferedImage bufferedImage = new BufferedImage(200, 300, BufferedImage.TYPE_INT_RGB);

        when(mockedParameters.getRequiredInteger(DEGREE))
            .thenReturn(90);
        when(mockedParameters.getRequiredFileEntry(IMAGE))
            .thenReturn(mockedFileEntry);
        when(mockedParameters.getRequiredString(RESULT_FILE_NAME))
            .thenReturn("rotatedImage");
        when(mockedFileEntry.getExtension())
            .thenReturn("png");
        when(mockedContext.file(any()))
            .thenReturn(mockedFile);

        try (MockedStatic<ImageHelperUtils> imageHelperUtilsMockedStatic = mockStatic(ImageHelperUtils.class);
            MockedStatic<ImageIO> imageIOMockedStatic = mockStatic(ImageIO.class)) {
            imageHelperUtilsMockedStatic.when(() -> ImageHelperUtils.storeBufferedImage(
                contextArgumentCaptor.capture(), bufferedImageArgumentCaptor.capture(),
                extenstionArgumentCaptor.capture(), fileNameArgumentCaptor.capture()))
                .thenReturn(mockedFileEntry);

            imageIOMockedStatic.when(() -> ImageIO.read(mockedFile))
                .thenReturn(bufferedImage);

            FileEntry result =
                ImageHelperRotateImageAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedFileEntry, result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());

            BufferedImage bufferedImageArgumentCaptorValue = bufferedImageArgumentCaptor.getValue();

            assertEquals(300, bufferedImageArgumentCaptorValue.getHeight());
            assertEquals(200, bufferedImageArgumentCaptorValue.getWidth());
            assertEquals("png", extenstionArgumentCaptor.getValue());
            assertEquals("rotatedImage", fileNameArgumentCaptor.getValue());
        }
    }
}
