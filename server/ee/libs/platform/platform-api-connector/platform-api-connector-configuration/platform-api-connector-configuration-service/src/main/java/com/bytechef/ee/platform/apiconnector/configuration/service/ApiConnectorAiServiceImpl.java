/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.commons.util.UrlValidator;
import com.bytechef.ee.platform.apiconnector.configuration.exception.ApiConnectorErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementation of ApiConnectorAiService that uses Spring AI to generate OpenAPI specifications from API
 * documentation.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class ApiConnectorAiServiceImpl implements ApiConnectorAiService {

    private static final Logger log = LoggerFactory.getLogger(ApiConnectorAiServiceImpl.class);

    private static final String SYSTEM_PROMPT =
        """
            You are an expert at analyzing API documentation and generating OpenAPI 3.0 specifications.

            Given the content of an API documentation page, analyze it and generate a valid OpenAPI 3.0 specification in YAML format.

            Guidelines:
            1. Extract all API endpoints, their HTTP methods, paths, parameters, request bodies, and responses
            2. Include proper descriptions for each endpoint
            3. Define appropriate schemas for request and response bodies
            4. Include authentication requirements if mentioned
            5. Set appropriate content types (usually application/json)
            6. Generate meaningful operationIds based on the endpoint purpose
            7. Include servers array with the base URL if available

            IMPORTANT: Return ONLY the OpenAPI specification in YAML format, without any markdown code blocks or explanations.
            The output should start with 'openapi: "3.0.0"' or 'openapi: "3.0.1"'.
            """;

    private final ApiConnectorGenerationJobService apiConnectorGenerationJobService;
    private final ChatModel chatModel;
    private final WebScrapeService webScrapeService;
    private final boolean ssrfEnabled;
    private final Set<String> ssrfAllowedHosts;

    @SuppressFBWarnings("EI")
    public ApiConnectorAiServiceImpl(
        ApiConnectorGenerationJobService apiConnectorGenerationJobService, ChatModel chatModel,
        WebScrapeService webScrapeService,
        @Value("${bytechef.security.ssrf.enabled:true}") boolean ssrfEnabled,
        @Value("${bytechef.security.ssrf.allowed-hosts:}") Set<String> ssrfAllowedHosts) {

        this.apiConnectorGenerationJobService = apiConnectorGenerationJobService;
        this.chatModel = chatModel;
        this.webScrapeService = webScrapeService;
        this.ssrfEnabled = ssrfEnabled;
        this.ssrfAllowedHosts = ssrfAllowedHosts;
    }

    @Override
    public String generateOpenApiSpecification(String documentationUrl) {
        String documentationContent = fetchDocumentation(documentationUrl);

        String userPrompt = String.format(
            "Analyze the following API documentation and generate an OpenAPI 3.0 specification:\n\n%s",
            documentationContent);

        Prompt prompt = new Prompt(SYSTEM_PROMPT + "\n\nUser: " + userPrompt);

        String response = chatModel.call(prompt)
            .getResult()
            .getOutput()
            .getText();

        return cleanOpenApiResponse(response);
    }

    private String fetchDocumentation(String documentationUrl) {
        validateDocumentationUrl(documentationUrl);

        WebScrapeService.ScrapeResult result = webScrapeService.scrape(documentationUrl);

        if (!result.success()) {
            throw new ConfigurationException(
                "Failed to fetch documentation: " + result.error(),
                ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        return result.content();
    }

    private String fetchDocumentation(String documentationUrl, int maxPages) {
        validateDocumentationUrl(documentationUrl);

        if (maxPages <= 1) {
            return fetchDocumentation(documentationUrl);
        }

        WebScrapeService.CrawlResult result = webScrapeService.crawl(documentationUrl, maxPages, List.of());

        if (!result.success()) {
            throw new ConfigurationException(
                "Failed to crawl documentation: " + result.error(),
                ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        return result.content();
    }

    private void validateDocumentationUrl(String documentationUrl) {
        if (ssrfEnabled) {
            UrlValidator.validate(documentationUrl, ssrfAllowedHosts);
        }
    }

    private String cleanOpenApiResponse(String response) {
        String cleaned = response.trim();

        if (cleaned.startsWith("```yaml")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    @Override
    @Async
    public void generateOpenApiSpecificationAsync(
        String jobId, String documentationUrl, String userInstructions, int maxPages) {

        log.debug("Starting async OpenAPI generation for job {}", jobId);

        apiConnectorGenerationJobService.markAsProcessing(jobId);

        try {
            if (apiConnectorGenerationJobService.isCancellationRequested(jobId)) {
                log.debug("Job {} was cancelled before processing started", jobId);

                return;
            }

            String documentationContent = fetchDocumentation(documentationUrl, maxPages);

            if (apiConnectorGenerationJobService.isCancellationRequested(jobId)) {
                log.debug("Job {} was cancelled after fetching documentation", jobId);

                return;
            }

            String promptMessage = String.format(
                "Analyze the following API documentation and generate an OpenAPI 3.0 specification:\n\n%s",
                documentationContent);

            if (userInstructions != null && !userInstructions.isBlank()) {
                promptMessage = promptMessage + "\n\nUser instructions:\n" + userInstructions;
            }

            Prompt prompt = new Prompt(SYSTEM_PROMPT + "\n\nUser: " + promptMessage);

            String response = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText();

            if (apiConnectorGenerationJobService.isCancellationRequested(jobId)) {
                log.debug("Job {} was cancelled after AI generation", jobId);

                return;
            }

            String specification = cleanOpenApiResponse(response);

            apiConnectorGenerationJobService.markAsCompleted(jobId, specification);

            log.debug("Job {} completed successfully", jobId);
        } catch (Exception exception) {
            log.error("Job {} failed", jobId, exception);

            apiConnectorGenerationJobService.markAsFailed(jobId, exception.getMessage());
        }
    }
}
