/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.ee.platform.apiconnector.configuration.exception.ApiConnectorErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    private final ChatModel chatModel;
    private final HttpClient httpClient;

    @SuppressFBWarnings("EI")
    public ApiConnectorAiServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Override
    public String generateOpenApiSpecification(String documentationUrl) {
        String documentationContent = fetchDocumentation(documentationUrl);

        return generateOpenApiSpecificationFromContent(documentationContent, null);
    }

    @Override
    public String generateOpenApiSpecificationFromContent(
        String documentationContent, @Nullable String userInstructions) {

        String promptMessage = String.format(
            "Analyze the following API documentation and generate an OpenAPI 3.0 specification:\n\n%s",
            documentationContent);

        if (userInstructions != null && !userInstructions.isBlank()) {
            promptMessage = promptMessage + "\n\nUser instructions:\n" + userInstructions;
        }

        Prompt prompt = new Prompt(SYSTEM_PROMPT + "\n\nUser: " + promptMessage);

        String response = extractResponseText(chatModel.call(prompt));

        return cleanOpenApiResponse(response);
    }

    private static String extractResponseText(ChatResponse chatResponse) {
        Generation generation = chatResponse.getResult();

        String text = generation == null ? null
            : generation.getOutput()
                .getText();

        if (text == null) {
            throw new ConfigurationException(
                "AI model returned an empty response", ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        return text;
    }

    @Override
    public String fetchDocumentation(String documentationUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(documentationUrl))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "ByteChef API Connector Generator")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ConfigurationException(
                    "Failed to fetch documentation: HTTP " + response.statusCode(),
                    ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
            }

            Document document = Jsoup.parse(response.body());

            document.select("script, style, nav, footer, header, aside")
                .remove();

            return document.body()
                .text();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread()
                    .interrupt();
            }

            throw new ConfigurationException(
                "Failed to fetch documentation: " + e.getMessage(),
                ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
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
}
