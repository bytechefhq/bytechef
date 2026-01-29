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

package com.bytechef.automation.knowledgebase.worker.etl;

import com.bytechef.automation.knowledgebase.worker.document.OcrDocumentReader;
import com.bytechef.automation.knowledgebase.worker.document.ocr.OcrService;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Factory for creating Spring AI DocumentReader instances based on document MIME type.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.knowledgebase", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentReaderFactory {

    private final OcrService ocrService;

    public KnowledgeBaseDocumentReaderFactory(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * Creates an appropriate DocumentReader for the given resource based on its MIME type.
     *
     * @param resource        the document resource to read
     * @param mimeType        the MIME type of the document
     * @param useParagraphPdf whether to use paragraph-based PDF reading (better for structured documents)
     * @return the appropriate DocumentReader for the document type
     */
    public DocumentReader createDocumentReader(
        Resource resource, String mimeType, boolean useParagraphPdf) {

        if (ocrService.isEnabled() && useOcr(mimeType)) {
            return new OcrDocumentReader(ocrService, resource);
        }

        if (mimeType == null) {
            return new TikaDocumentReader(resource);
        }

        return switch (mimeType.toLowerCase()) {
            case "application/json" -> new JsonReader(resource);
            case "text/plain" -> new TextReader(resource);
            case "text/markdown", "text/x-markdown" ->
                new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.defaultConfig());
            case "application/pdf" -> useParagraphPdf
                ? new ParagraphPdfDocumentReader(resource)
                : new PagePdfDocumentReader(resource);
            default -> new TikaDocumentReader(resource);
        };
    }

    /**
     * Creates an appropriate DocumentReader for the given resource based on its MIME type. Uses page-based PDF reading
     * by default.
     *
     * @param resource the document resource to read
     * @param mimeType the MIME type of the document
     * @return the appropriate DocumentReader for the document type
     */
    public DocumentReader createDocumentReader(Resource resource, String mimeType) {
        return createDocumentReader(resource, mimeType, false);
    }

    /**
     * Determines if OCR should be used for the given MIME type.
     *
     * @param mimeType the MIME type of the document
     * @return true if OCR should be used
     */
    private boolean useOcr(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        String lowerMimeType = mimeType.toLowerCase();

        return lowerMimeType.equals("application/pdf") || lowerMimeType.startsWith("image/");
    }
}
