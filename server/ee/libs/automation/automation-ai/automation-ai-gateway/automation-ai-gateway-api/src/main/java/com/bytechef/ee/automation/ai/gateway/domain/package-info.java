/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

/**
 * Domain model for the AI Gateway &amp; Observability Platform.
 *
 * <p>
 * This package contains the persistent entities that back the four feature areas of the platform:
 *
 * <ul>
 * <li><b>Gateway</b> — {@code AiGatewayProvider}, {@code AiGatewayModel}, {@code AiGatewayRoutingPolicy},
 * {@code AiGatewayBudget}, {@code AiGatewayRateLimit}, {@code AiGatewayRequestLog}, and friends. Drives routing,
 * budgets, caching, and rate limiting on the hot request path.
 * <li><b>Observability</b> — {@code AiObservabilityTrace}, {@code AiObservabilitySpan}, {@code AiObservabilitySession},
 * {@code AiObservabilityAlertRule}, {@code AiObservabilityAlertEvent}, {@code AiObservabilityExportJob},
 * {@code AiObservabilityWebhookSubscription}. Records and aggregates traffic for the control plane.
 * <li><b>Prompt management</b> — {@code AiPrompt} and {@code AiPromptVersion} for version-controlled,
 * environment-deployed prompts.
 * <li><b>Evaluation</b> — {@code AiEvalRule}, {@code AiEvalExecution}, {@code AiEvalScore}, and
 * {@code AiEvalScoreConfig} for manual annotations, programmatic scoring, and LLM-as-judge evaluation.
 * </ul>
 *
 * <p>
 * Entities are Spring Data JDBC aggregates. They are intended to be loaded and persisted via the repository/service
 * layer in {@code com.bytechef.ee.automation.ai.gateway.service}; do not mutate them from the REST or GraphQL layer
 * directly.
 *
 * @version ee
 */
package com.bytechef.ee.automation.ai.gateway.domain;
