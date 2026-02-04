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
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.pdf.helper.util.PdfHelperUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class PdfHelperTextToPdfActionTest {

    private final ArgumentCaptor<ByteArrayOutputStream> byteArrayOutputStreamArgumentCaptor =
        ArgumentCaptor.forClass(ByteArrayOutputStream.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Context mockedContext = mock(Context.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(TEXT, "sample text", FILENAME, "fileName"));
    private final PdfWriter mockPdfWriter = mock(PdfWriter.class);

    @Test
    void perform() {
        try (MockedStatic<PdfWriter> mockedPdfWriter = mockStatic(PdfWriter.class);
            MockedConstruction<Document> mockedDocumentConstruction = mockConstruction(Document.class);
            MockedStatic<PdfHelperUtils> mockedPdfHelperUtils = mockStatic(PdfHelperUtils.class)) {

            mockedPdfWriter.when(() -> PdfWriter.getInstance(any(Document.class), any(ByteArrayOutputStream.class)))
                .thenReturn(mockPdfWriter);

            mockedPdfHelperUtils.when(() -> PdfHelperUtils.storeIntoFileEntry(
                contextArgumentCaptor.capture(),
                byteArrayOutputStreamArgumentCaptor.capture(),
                fileNameArgumentCaptor.capture()))
                .thenReturn(mockedFileEntry);

            FileEntry result = PdfHelperTextToPdfAction.perform(mockedParameters, mockedParameters, mockedContext);

            Document mockDocument = mockedDocumentConstruction.constructed()
                .getFirst();

            verify(mockDocument).open();
            verify(mockDocument).add(any(Paragraph.class));
            verify(mockDocument).close();

            mockedPdfWriter.verify(() -> PdfWriter.getInstance(any(Document.class), any(ByteArrayOutputStream.class)));

            assertEquals(mockedFileEntry, result);
        }
    }
}
