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

import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.QUALITY;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class ImageHelperCompressImageActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Context.File, ?>> contextFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Context.File mockedContextFile = mock(Context.File.class);
    private final ImageOutputStream mockedImageOutputStream = mock(ImageOutputStream.class);
    private final ImageWriter mockedImageWriter = mock(ImageWriter.class);
    private final ImageWriteParam mockedImageWriteParam = mock(ImageWriteParam.class);
    private final FileEntry mockedInputFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(IMAGE, mockedInputFileEntry, QUALITY, 1, RESULT_FILE_NAME, "compressedImage"));
    private final FileEntry mockedResultFileEntry = mock(FileEntry.class);

    @Test
    void testPerform() throws Exception {
        BufferedImage originalImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);

        File tempFile = File.createTempFile("image", ".png");
        tempFile.deleteOnExit();

        when(mockedInputFileEntry.getExtension())
            .thenReturn("png");
        when(mockedContext.file(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<Context.File, ?> function = invocation.getArgument(0);

                return function.apply(mockedContextFile);
            });
        when(mockedContextFile.toTempFile(mockedInputFileEntry))
            .thenReturn(tempFile);
        when(mockedContextFile.storeContent(anyString(), any(InputStream.class)))
            .thenReturn(mockedResultFileEntry);
        when(mockedImageWriter.getDefaultWriteParam())
            .thenReturn(mockedImageWriteParam);

        try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {
            imageIOMock.when(() -> ImageIO.read(tempFile))
                .thenReturn(originalImage);

            imageIOMock.when(() -> ImageIO.getImageWritersByFormatName("png"))
                .thenReturn(Collections.singletonList(mockedImageWriter)
                    .iterator());

            imageIOMock.when(() -> ImageIO.createImageOutputStream(any(ByteArrayOutputStream.class)))
                .thenReturn(mockedImageOutputStream);

            FileEntry result =
                ImageHelperCompressImageAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedResultFileEntry, result);

            verify(mockedContextFile).toTempFile(mockedInputFileEntry);
            verify(mockedContextFile).storeContent(eq("compressedImage.png"), any(InputStream.class));
            verify(mockedImageWriter).write(any(), any(), eq(mockedImageWriteParam));

            ContextFunction<Context.File, ?> contextFunction = contextFunctionArgumentCaptor.getValue();

            assertEquals(mockedResultFileEntry, contextFunction.apply(mockedContextFile));
        }
    }
}
