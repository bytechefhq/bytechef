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

import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.DPI;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.FILE;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.FILENAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.pdf.helper.util.PdfHelperUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class PdfHelperConvertToImageActionTest {

    private final ArgumentCaptor<ByteArrayOutputStream> byteArrayOutputStreamArgumentCaptor =
        ArgumentCaptor.forClass(ByteArrayOutputStream.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final BufferedImage mockedBufferedImage = mock(BufferedImage.class);
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final Context mockedContext = mock(Context.class);
    private final File mockedFile = mock(File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE, mockedFileEntry, FILENAME, "TestFile"));
    private final PDDocument mockedPDDocument = mock(PDDocument.class);

    @Test
    void perform() throws Exception {
        when(mockedContext.file(any()))
            .thenReturn(mockedFile);

        try (MockedStatic<Loader> mockedLoader = mockStatic(Loader.class);
            MockedStatic<ImageIO> mockedImageIO = mockStatic(ImageIO.class);
            MockedStatic<PdfHelperUtils> mockedPdfHelperUtils = mockStatic(PdfHelperUtils.class)) {

            mockConstruction(PDFRenderer.class, (mock, context) -> when(mock.renderImageWithDPI(0, DPI, ImageType.RGB))
                .thenReturn(mockedBufferedImage));

            mockedLoader.when(() -> Loader.loadPDF(mockedFile))
                .thenReturn(mockedPDDocument);

            when(mockedPDDocument.getNumberOfPages())
                .thenReturn(1);

            mockedImageIO.when(() -> ImageIO.write(mockedBufferedImage, "jpeg", byteArrayOutputStream))
                .thenReturn(true);

            mockedPdfHelperUtils.when(() -> PdfHelperUtils.storeIntoFileEntry(
                contextArgumentCaptor.capture(),
                byteArrayOutputStreamArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
                .thenReturn(mockedFileEntry);

            List<FileEntry> result = PdfHelperConvertToImageAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(List.of(mockedFileEntry), result);

            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals("TestFile1.jpeg", stringArgumentCaptor.getValue());
        }
    }
}
