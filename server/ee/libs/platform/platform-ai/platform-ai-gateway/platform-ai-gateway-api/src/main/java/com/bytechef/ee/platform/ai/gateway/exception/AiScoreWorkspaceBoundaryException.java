/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

/**
 * Thrown when an AI-gateway access targets a resource outside the caller's workspace. Mapped to HTTP 403 by the
 * gateway's exception handler — the boundary is surfaced, not hidden behind a 404.
 *
 * <p>
 * Carries structured workspace / target context as fields so log appenders, the security-ops dashboard, and audit
 * subscribers do not have to parse the message string. The message itself stays uniform ("Caller is not authorized for
 * X") to avoid leaking target-existence and target-workspace-id to cross-tenant probers — the structured fields are
 * server-side only.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class AiScoreWorkspaceBoundaryException extends RuntimeException {

    private final Long workspaceId;
    private final AiScoreTargetType targetType;
    private final Long targetId;

    // Constructors are private so callers must use the forTarget(...) factories. Exposing them publicly let a
    // future caller skip the uniform-message contract centralised in targetTypeLabel — distinguishable error
    // shapes between throw sites would let workspace-A enumerate workspace-B resource ids by probing message
    // deltas. Eleven existing callers were already migrated to forTarget; this lockdown matches the
    // factory-only construction pattern AiScoreTargetNotFoundException already enforces.
    private AiScoreWorkspaceBoundaryException(
        String message, Long workspaceId, AiScoreTargetType targetType, Long targetId) {

        this(message, workspaceId, targetType, targetId, null);
    }

    private AiScoreWorkspaceBoundaryException(
        String message, Long workspaceId, AiScoreTargetType targetType, Long targetId, Throwable cause) {

        super(message, cause);

        this.workspaceId = workspaceId;
        this.targetType = targetType == null ? AiScoreTargetType.UNKNOWN : targetType;
        this.targetId = targetId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public AiScoreTargetType getTargetTypeCanonical() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    /**
     * Builds the uniform "Caller is not authorized for X" label used in the wire-facing message. Centralized here so
     * every boundary throw site renders the same shape; deviating messages would let workspace-A enumerate workspace-B
     * resource ids by reading the 403 body. Tolerates {@code null targetType} so the helper remains safe when called
     * from log appenders that may receive a partially-populated exception; the {@link #forTarget} factories enforce
     * non-null at construction time.
     */
    public static String targetTypeLabel(AiScoreTargetType targetType, Long targetId) {
        AiScoreTargetType resolvedType = targetType == null ? AiScoreTargetType.UNKNOWN : targetType;
        String idSuffix = targetId == null ? "" : " " + targetId;

        return switch (resolvedType) {
            case TRACE -> "trace" + idSuffix;
            case SPAN -> "span" + idSuffix;
            case EXPERIMENT -> "experiment" + idSuffix;
            case DATASET -> "dataset" + idSuffix;
            case DATASET_VERSION -> "dataset version" + idSuffix;
            case DATASET_ITEM -> "dataset item" + idSuffix;
            case WORKSPACE -> "workspace" + idSuffix;
            case UNKNOWN -> "target" + idSuffix;
        };
    }

    /**
     * Convenience factory for the uniform-message + structured-fields pattern. Equivalent to
     * {@link #AiScoreWorkspaceBoundaryException(String, Long, AiScoreTargetType, Long, Throwable)} with the message
     * derived from {@link #targetTypeLabel}. Rejects {@code null targetType} — every legitimate call site has the type
     * at hand; a silent coercion to {@link AiScoreTargetType#UNKNOWN} would mask a programming error and feed
     * mis-attributed metrics to the security dashboard.
     */
    public static AiScoreWorkspaceBoundaryException forTarget(
        Long callerWorkspaceId, AiScoreTargetType targetType, Long targetId, Throwable cause) {

        if (targetType == null) {
            throw new IllegalArgumentException(
                "targetType must not be null — call sites always know the resource kind that tripped the boundary "
                    + "check; use a specific AiScoreTargetType variant (the UNKNOWN value is reserved for hydration "
                    + "paths where the type was lost in transit, not for factory inputs)");
        }

        return new AiScoreWorkspaceBoundaryException(
            "Caller is not authorized for " + targetTypeLabel(targetType, targetId),
            callerWorkspaceId, targetType, targetId, cause);
    }

    public static AiScoreWorkspaceBoundaryException forTarget(
        Long callerWorkspaceId, AiScoreTargetType targetType, Long targetId) {

        return forTarget(callerWorkspaceId, targetType, targetId, null);
    }
}
