/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorAiService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorGenerationJobService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class ApiConnectorGenerationJobFacadeImpl implements ApiConnectorGenerationJobFacade {

    private static final Logger log = LoggerFactory.getLogger(ApiConnectorGenerationJobFacadeImpl.class);

    private final ApiConnectorAiService apiConnectorAiService;
    private final ApiConnectorGenerationJobService apiConnectorGenerationJobService;

    @SuppressFBWarnings("EI")
    public ApiConnectorGenerationJobFacadeImpl(
        ApiConnectorAiService apiConnectorAiService,
        ApiConnectorGenerationJobService apiConnectorGenerationJobService) {

        this.apiConnectorAiService = apiConnectorAiService;
        this.apiConnectorGenerationJobService = apiConnectorGenerationJobService;
    }

    @Override
    @Async
    public void generateOpenApiSpecificationAsync(
        String jobId, String documentationUrl, @Nullable String userInstructions) {

        log.debug("Starting async OpenAPI generation for job {}", jobId);

        apiConnectorGenerationJobService.markAsProcessing(jobId);

        try {
            if (apiConnectorGenerationJobService.isCancellationRequested(jobId)) {
                log.debug("Job {} was cancelled before processing started", jobId);

                return;
            }

            String documentationContent = apiConnectorAiService.fetchDocumentation(documentationUrl);

            if (apiConnectorGenerationJobService.isCancellationRequested(jobId)) {
                log.debug("Job {} was cancelled after fetching documentation", jobId);

                return;
            }

            String specification = apiConnectorAiService.generateOpenApiSpecificationFromContent(
                documentationContent, userInstructions);

            if (apiConnectorGenerationJobService.isCancellationRequested(jobId)) {
                log.debug("Job {} was cancelled after AI generation", jobId);

                return;
            }

            apiConnectorGenerationJobService.markAsCompleted(jobId, specification);

            log.debug("Job {} completed successfully", jobId);
        } catch (Exception exception) {
            log.error("Job {} failed", jobId, exception);

            apiConnectorGenerationJobService.markAsFailed(jobId, exception.getMessage());
        }
    }
}
