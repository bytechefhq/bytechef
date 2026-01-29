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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mistralai.ocr.MistralOcrApi;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRModel;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRRequest;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRRequest.DocumentURLChunk;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;

/**
 * OCR service implementation using Mistral AI's OCR API.
 *
 * <p>
 * This service uploads documents to Mistral's file storage, then uses Mistral's OCR model to extract text. The
 * extracted text is returned in markdown format.
 * </p>
 *
 * @author Ivica Cardic
 */
public class MistralOcrService implements OcrService {

    private static final Logger logger = LoggerFactory.getLogger(MistralOcrService.class);
    private static final String DEFAULT_BASE_URL = "https://api.mistral.ai";

    private final MistralOcrApi mistralOcrApi;
    private final RestClient restClient;

    /**
     * Creates a new MistralOcrService with the specified API key.
     *
     * @param apiKey the Mistral AI API key
     */
    public MistralOcrService(String apiKey) {
        this.mistralOcrApi = new MistralOcrApi(apiKey);
        this.restClient = RestClient.builder()
            .baseUrl(DEFAULT_BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
    }

    @Override
    public String perform(Resource resource) {
        String filename = resource.getFilename();

        logger.debug("Running OCR on document: {}", filename);

        try {
            // Step 1: Upload the file to Mistral
            FileUploadResponse uploadResponse = uploadFile(resource);

            logger.debug("File uploaded successfully: {}", uploadResponse.id());

            // Step 2: Get signed URL for the uploaded file
            SignedUrlResponse signedUrlResponse = getSignedUrl(uploadResponse.id());

            logger.debug("Got signed URL for file: {}", uploadResponse.id());

            // Step 3: Run OCR using the signed URL
            String result = performOcr(signedUrlResponse.url());

            // Step 4: Delete the uploaded file
            deleteFile(uploadResponse.id());

            return result;
        } catch (IOException exception) {
            logger.error("Error processing document {}: {}", filename, exception.getMessage(), exception);

            throw new RuntimeException("Failed to process document for OCR: " + filename, exception);
        }
    }

    @Override
    public boolean isEnabled() {
        return mistralOcrApi != null;
    }

    private FileUploadResponse uploadFile(Resource resource) throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        String filename = resource.getFilename();

        if (filename == null) {
            filename = "document";
        }

        builder.part("file", resource)
            .filename(filename)
            .contentType(MediaType.APPLICATION_OCTET_STREAM);
        builder.part("purpose", "ocr");

        ResponseEntity<FileUploadResponse> response = restClient.post()
            .uri("/v1/files")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(builder.build())
            .retrieve()
            .toEntity(FileUploadResponse.class);

        if (response.getBody() == null) {
            throw new RuntimeException("Failed to upload file: empty response");
        }

        return response.getBody();
    }

    private SignedUrlResponse getSignedUrl(String fileId) {
        ResponseEntity<SignedUrlResponse> response = restClient.get()
            .uri("/v1/files/{fileId}/url", fileId)
            .retrieve()
            .toEntity(SignedUrlResponse.class);

        if (response.getBody() == null || response.getBody()
            .url() == null) {
            throw new RuntimeException("Failed to get signed URL for file: " + fileId);
        }

        return response.getBody();
    }

    private String performOcr(String documentUrl) {
        OCRRequest request = new OCRRequest(
            OCRModel.MISTRAL_OCR_LATEST.getValue(),
            new DocumentURLChunk(documentUrl));

        ResponseEntity<OCRResponse> responseEntity = mistralOcrApi.ocr(request);

        if (responseEntity.getBody() == null || responseEntity.getBody()
            .pages() == null) {
            logger.warn("OCR response was empty for document");

            return "";
        }

        OCRResponse response = responseEntity.getBody();

        String markdown = response.pages()
            .stream()
            .map(page -> page.markdown() != null ? page.markdown() : "")
            .collect(Collectors.joining("\n\n"));

        // Remove null bytes (0x00) which PostgreSQL text columns don't support
        markdown = markdown.replace("\0", "");

        logger.debug("OCR completed, extracted {} pages", response.pagesProcessed());

        return markdown;
    }

    private void deleteFile(String fileId) {
        try {
            restClient.delete()
                .uri("/v1/files/{fileId}", fileId)
                .retrieve()
                .toBodilessEntity();

            logger.debug("Deleted uploaded file: {}", fileId);
        } catch (Exception exception) {
            logger.warn("Failed to delete uploaded file {}: {}", fileId, exception.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FileUploadResponse(
        @JsonProperty("id") String id,
        @JsonProperty("filename") String filename,
        @JsonProperty("purpose") String purpose) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SignedUrlResponse(
        @JsonProperty("url") String url,
        @JsonProperty("expires_at") Long expiresAt) {
    }
}
