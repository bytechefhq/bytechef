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

package com.bytechef.automation.knowledgebase.worker.document.ocr;

import org.springframework.core.io.Resource;

/**
 * Service interface for Optical Character Recognition (OCR) operations.
 *
 * @author Ivica Cardic
 */
public interface OcrService {

    /**
     * Performs OCR on a document resource.
     *
     * @param resource the document resource to process
     * @return the extracted text in markdown format
     */
    String perform(Resource resource);

    /**
     * Checks if this OCR service is enabled and properly configured.
     *
     * @return true if the service is enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }
}
