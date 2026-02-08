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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.w3c.dom.Node;

/**
 * @author Marija Horvat
 */
class ImageHelperGetImageMetadataActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Context.File, ?>> contextFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Context.File mockedContextFile = mock(Context.File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final File mockedFile = mock(File.class);
    private final IIOMetadata mockedIioMetadata = mock(IIOMetadata.class);
    private final ImageInputStream mockedImageInputStream = mock(ImageInputStream.class);
    private final ImageReader mockedImageReader = mock(ImageReader.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(IMAGE, mockedFileEntry));
    private final Node mockedNode = mock(Node.class);

    @Test
    void testPerform() throws Exception {
        when(mockedContext.file(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<Context.File, ?> function = invocation.getArgument(0);

                return function.apply(mockedContextFile);
            });

        when(mockedContextFile.toTempFile(mockedFileEntry))
            .thenReturn(mockedFile);

        try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {

            imageIOMock.when(() -> ImageIO.createImageInputStream(mockedFile))
                .thenReturn(mockedImageInputStream);

            imageIOMock.when(() -> ImageIO.getImageReaders(mockedImageInputStream))
                .thenReturn(Collections.singletonList(mockedImageReader)
                    .iterator());

            when(mockedImageReader.getImageMetadata(0))
                .thenReturn(mockedIioMetadata);
            when(mockedIioMetadata.getMetadataFormatNames())
                .thenReturn(new String[] {
                    "formatA", "formatB"
                });
            when(mockedIioMetadata.getAsTree(any()))
                .thenReturn(mockedNode);

            Map<String, Object> result = ImageHelperGetImageMetadataAction.perform(
                mockedParameters, any(Parameters.class), mockedContext);

            assertEquals(2, result.size());
            assertNotNull(result.get("formatA"));
            assertNotNull(result.get("formatB"));

            verify(mockedContextFile).toTempFile(mockedFileEntry);
            verify(mockedImageReader).setInput(mockedImageInputStream, true);

            ContextFunction<Context.File, ?> contextFunction = contextFunctionArgumentCaptor.getValue();

            assertEquals(mockedFile, contextFunction.apply(mockedContextFile));
        }
    }
}
