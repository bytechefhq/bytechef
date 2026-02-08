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

import static com.bytechef.component.image.helper.constant.ImageHelperConstants.HEIGHT;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.WIDTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.image.helper.util.ImageHelperUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class ImageHelperResizeImageActionTest {

    private final ArgumentCaptor<BufferedImage> bufferedImageArgumentCaptor = forClass(BufferedImage.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Context.File, ?>> contextFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Context.File mockedContextFile = mock(Context.File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final File mockedFile = mock(File.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(WIDTH, 300, HEIGHT, 200, IMAGE, mockedFileEntry, RESULT_FILE_NAME, "resizedImage"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() throws Exception {
        BufferedImage bufferedImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);

        when(mockedFileEntry.getExtension())
            .thenReturn("png");
        when(mockedContext.file(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<Context.File, ?> function = invocation.getArgument(0);

                return function.apply(mockedContextFile);
            });
        when(mockedContextFile.toTempFile(mockedFileEntry)).thenReturn(mockedFile);

        try (MockedStatic<ImageHelperUtils> imageHelperUtilsMockedStatic = mockStatic(ImageHelperUtils.class);
            MockedStatic<ImageIO> imageIOMockedStatic = mockStatic(ImageIO.class)) {

            imageHelperUtilsMockedStatic.when(() -> ImageHelperUtils.storeBufferedImage(
                contextArgumentCaptor.capture(),
                bufferedImageArgumentCaptor.capture(),
                stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
                .thenReturn(mockedFileEntry);

            imageIOMockedStatic.when(() -> ImageIO.read(mockedFile))
                .thenReturn(bufferedImage);

            FileEntry result = ImageHelperResizeImageAction.perform(
                mockedParameters, any(Parameters.class), mockedContext);

            assertEquals(mockedFileEntry, result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());

            BufferedImage bufferedImageArgumentCaptorValue = bufferedImageArgumentCaptor.getValue();

            assertEquals(200, bufferedImageArgumentCaptorValue.getHeight());
            assertEquals(300, bufferedImageArgumentCaptorValue.getWidth());
            assertEquals(List.of("png", "resizedImage"), stringArgumentCaptor.getAllValues());

            ContextFunction<Context.File, ?> contextFunction = contextFunctionArgumentCaptor.getValue();

            assertEquals(mockedFile, contextFunction.apply(mockedContextFile));
        }
    }
}
