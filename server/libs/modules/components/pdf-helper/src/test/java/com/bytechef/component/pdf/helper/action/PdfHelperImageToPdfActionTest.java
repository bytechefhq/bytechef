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

package com.bytechef.component.pdf.helper.action;

import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.FILENAME;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.IMAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.pdf.helper.util.PdfHelperUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class PdfHelperImageToPdfActionTest {

    private final ArgumentCaptor<ByteArrayOutputStream> byteArrayOutputStreamArgumentCaptor =
        ArgumentCaptor.forClass(ByteArrayOutputStream.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final File mockedFile = mock(File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(IMAGES, List.of(mockedFileEntry), FILENAME, "testFileName"));
    private final PDImageXObject mockPDImageXObject = mock(PDImageXObject.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void perform() throws Exception {
        when(mockedContext.file(any()))
            .thenReturn(mockedFile);

        try (MockedStatic<PDImageXObject> mockedPDImageXObject = mockStatic(PDImageXObject.class);
            MockedStatic<PdfHelperUtils> mockedPdfHelperUtils = mockStatic(PdfHelperUtils.class)) {

            mockedPDImageXObject.when(() -> PDImageXObject.createFromFileByContent(any(), any()))
                .thenReturn(mockPDImageXObject);

            when(mockPDImageXObject.getWidth()).thenReturn(800);
            when(mockPDImageXObject.getHeight()).thenReturn(600);

            mockedPdfHelperUtils.when(() -> PdfHelperUtils.storeIntoFileEntry(
                contextArgumentCaptor.capture(),
                byteArrayOutputStreamArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
                .thenReturn(mockedFileEntry);

            FileEntry result = PdfHelperImageToPdfAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedFileEntry, result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals("testFileName.pdf", stringArgumentCaptor.getValue());

            verify(mockedContext, times(1)).file(any());

            mockedPDImageXObject.verify(
                () -> PDImageXObject.createFromFileByContent(any(), any()),
                times(1));
        }
    }
}
