/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

/**
 * Facade layer for the AI Gateway &amp; Observability Platform.
 *
 * <p>
 * Facades are the public entry points for higher layers (REST controllers, GraphQL resolvers, EE remote clients). Each
 * facade orchestrates one or more services and is the right place to put cross-service transactions, authorization
 * checks, and workspace/environment scoping.
 *
 * <ul>
 * <li>{@link com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade} — the main request-path entry point. Handles
 * {@code chatCompletion}, {@code chatCompletionStream}, and embedding calls: resolves the routing policy, enforces
 * budgets and rate limits, consults the response cache, dispatches to the upstream provider, and records traces and
 * request logs.
 * <li>{@code WorkspaceAiGatewayProviderFacadeImpl}, {@code WorkspaceAiGatewayModelFacadeImpl},
 * {@code WorkspaceAiGatewayRoutingPolicyFacadeImpl} — workspace-scoped administration of providers, models, and routing
 * policies used by the configuration UI.
 * </ul>
 *
 * <p>
 * Controllers should depend on facades from this package rather than on individual services under
 * {@code com.bytechef.ee.automation.ai.gateway.service} — the facade layer is what guarantees the consistent "request
 * on the hot path" semantics (budget check + rate limit + cache + provider call + trace/log write) happen atomically.
 *
 * @version ee
 */
package com.bytechef.ee.automation.ai.gateway.facade;
