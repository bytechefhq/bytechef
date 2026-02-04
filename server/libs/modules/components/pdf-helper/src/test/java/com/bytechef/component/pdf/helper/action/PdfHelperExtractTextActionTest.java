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

import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class PdfHelperExtractTextActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final File mockedFile = mock(File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(FILE, mockedFileEntry));
    private final PDDocument mockedPDDocument = mock(PDDocument.class);

    @Test
    void testPerform() throws IOException {
        when(mockedActionContext.file(any()))
            .thenReturn(mockedFile);

        try (MockedStatic<Loader> loaderMockedStatic = mockStatic(Loader.class)) {
            try (MockedConstruction<PDFTextStripper> pdfTextStripperMockedConstruction = mockConstruction(
                PDFTextStripper.class,
                (mock, context) -> when(mock.getText(mockedPDDocument)).thenReturn("Sample text"))) {

                loaderMockedStatic.when(() -> Loader.loadPDF(mockedFile))
                    .thenReturn(mockedPDDocument);

                String result = PdfHelperExtractTextAction.perform(mockedParameters, null, mockedActionContext);

                assertEquals("Sample text", result);

                List<PDFTextStripper> constructed = pdfTextStripperMockedConstruction.constructed();
                assertEquals(1, constructed.size());

                verify(mockedActionContext).file(any());
            }
        }
    }
}
