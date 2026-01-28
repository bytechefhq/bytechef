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

package com.bytechef.automation.knowledgebase.worker.document;

import com.bytechef.automation.knowledgebase.worker.document.ocr.OcrService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.Resource;

/**
 * A DocumentReader implementation that uses an OCR service to extract text from documents.
 *
 * <p>
 * This reader takes a document resource and uses the configured OCR service to extract text content in markdown format.
 * </p>
 *
 * @author Ivica Cardic
 */
public class OcrDocumentReader implements DocumentReader {

    private final OcrService ocrService;
    private final Resource resource;

    public OcrDocumentReader(OcrService ocrService, Resource resource) {
        this.ocrService = ocrService;
        this.resource = resource;
    }

    @Override
    public List<Document> get() {
        String extractedText = ocrService.perform(resource);

        if (extractedText == null || extractedText.isBlank()) {
            return List.of();
        }

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("source", resource.getFilename());
        metadata.put("ocrProcessed", true);

        Document document = new Document(extractedText, metadata);

        return List.of(document);
    }
}
