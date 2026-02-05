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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.w3c.dom.Node;

/**
 * @author Marija Horvat
 */
class ImageHelperGetImageMetadataActionTest {

    private final Context mockedContext = mock(Context.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final File mockedFile = mock(File.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context.File mockedContextFile = mock(Context.File.class);
    private final ImageInputStream imageInputStream = mock(ImageInputStream.class);
    private final ImageReader imageReader = mock(ImageReader.class);
    private final IIOMetadata metadata = mock(IIOMetadata.class);
    private final Node mockedNode = mock(Node.class);

    @Test
    void testPerform() throws IOException {
        when(mockedParameters.getRequiredFileEntry("image")).thenReturn(mockedFileEntry);

        when(mockedContext.file(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Context.File, ?> function =
                invocation.getArgument(0);

            return function.apply(mockedContextFile);
        });

        when(mockedContextFile.toTempFile(mockedFileEntry)).thenReturn(mockedFile);

        try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {

            imageIOMock.when(() -> ImageIO.createImageInputStream(mockedFile))
                .thenReturn(imageInputStream);

            imageIOMock.when(() -> ImageIO.getImageReaders(imageInputStream))
                .thenReturn(Collections.singletonList(imageReader)
                    .iterator());

            when(imageReader.getImageMetadata(0)).thenReturn(metadata);

            when(metadata.getMetadataFormatNames()).thenReturn(new String[] {
                "formatA", "formatB"
            });

            when(metadata.getAsTree(any())).thenReturn(mockedNode);

            Map<String, Object> result =
                ImageHelperGetImageMetadataAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(2, result.size());
            assertNotNull(result.get("formatA"));
            assertNotNull(result.get("formatB"));

            verify(mockedContextFile).toTempFile(mockedFileEntry);
            verify(imageReader).setInput(imageInputStream, true);
        }
    }
}
