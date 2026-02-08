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

package com.bytechef.component.image.helper.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class ImageHelperUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);

    @Test
    void testStoreBufferedImage() throws IOException {
        when(mockedContext.file(any()))
            .thenReturn(mockedFileEntry);

        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        String extension = "png";
        String resultFileName = "testImage";

        FileEntry result = ImageHelperUtils.storeBufferedImage(mockedContext, image, extension, resultFileName);

        verify(mockedContext).file(any());

        assertEquals(mockedFileEntry, result);
    }
}
