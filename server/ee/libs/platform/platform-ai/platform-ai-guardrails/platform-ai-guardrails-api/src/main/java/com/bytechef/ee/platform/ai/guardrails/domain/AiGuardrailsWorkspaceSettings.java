/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.domain;

/**
 * Workspace-scoped AI guardrails configuration. Persisted as a single
 * {@link com.bytechef.platform.configuration.domain.Property} row rather than a dedicated table — the platform property
 * store already handles scope/audit/versioning/encryption and this is plain config data. All fields other than
 * {@link #workspaceId()} are nullable; a null field means "not overridden at this level". For a real workspace
 * (non-null {@code workspaceId}) that unions the field with the GLOBAL {@code bytechef.ai.gateway.guardrails.*}
 * properties only — it does NOT fall back to the tenant-default (null-{@code workspaceId}) row's value; the two
 * PLATFORM-scoped rows are otherwise independent. The tenant-default row is consulted only for calls that resolve to
 * {@code workspaceId == null} in the first place (e.g. embedded runs, unattributed calls). See
 * {@code AiGuardrailsWorkspaceSettingsServiceImpl}'s class javadoc for how the tenant-default row is stored, and
 * {@code AiGuardrails#resolvePolicy} for the union logic.
 *
 * @version ee
 */
public record AiGuardrailsWorkspaceSettings(
    Long workspaceId, // null = tenant default
    Boolean redactPii,
    Boolean redactSecrets,
    String blockedTerms, // comma-separated, same format as the gateway field
    Boolean moderationEnabled,
    Boolean injectionDetectionEnabled,
    Boolean scanResponses,
    BlockingMode blockingMode) {

    public static final String PROPERTY_KEY = "ai_guardrails_workspace_settings";

    /**
     * Stored in the property value map by {@link #name()}, not ordinal — but the ordinal is still pinned (see
     * {@code BlockingModeStabilityTest}) so this enum stays append-only, matching every other persisted enum in this
     * codebase. Append new values at the end only.
     *
     * @version ee
     */
    public enum BlockingMode {

        BLOCK,
        REDACT_AND_CONTINUE
    }
}
