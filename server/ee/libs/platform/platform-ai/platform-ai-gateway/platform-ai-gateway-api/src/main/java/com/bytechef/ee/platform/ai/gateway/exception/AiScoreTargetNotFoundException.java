/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

/**
 * Thrown when a score write targets a trace or span that does not exist. Mapped to HTTP 404 by the gateway's exception
 * handler. Distinct from {@link AiScoreWorkspaceBoundaryException} which signals "exists but not yours".
 *
 * <p>
 * Carries structured target context as fields — {@code targetType}, {@code targetId}, and {@code workspaceId} — so log
 * appenders and audit subscribers do not have to parse the message string. {@code workspaceId} matches the field on
 * {@link AiScoreWorkspaceBoundaryException}: a 404 still happens "under" a workspace (the caller's), and operators
 * triaging missing-target spikes need that context to scope by tenant. References the top-level
 * {@link AiScoreTargetType} so the 404 type does not depend on the 403 type.
 *
 * <p>
 * <strong>Construction:</strong> always via {@link #forTarget(Long, AiScoreTargetType, Long)} or
 * {@link #forTarget(Long, AiScoreTargetType, Long, Throwable)}. The factories enforce that the structured workspace /
 * target fields are populated — without them the cross-tenant triage signal would be silently severed at every log
 * appender that reads the structured fields. {@code targetType} must be non-null at the call site; silently coercing to
 * {@link AiScoreTargetType#UNKNOWN} would mask a programming error (every legitimate caller knows the resource kind it
 * is looking up). The {@code UNKNOWN} variant exists for hydration paths where the type was lost in transit, not for
 * factory inputs.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class AiScoreTargetNotFoundException extends RuntimeException {

    private final Long workspaceId;
    private final AiScoreTargetType targetType;
    private final Long targetId;

    private AiScoreTargetNotFoundException(
        String message, Long workspaceId, AiScoreTargetType targetType, Long targetId, Throwable cause) {

        super(message, cause);

        this.workspaceId = workspaceId;
        this.targetType = targetType == null ? AiScoreTargetType.UNKNOWN : targetType;
        this.targetId = targetId;
    }

    /**
     * Factory mirroring {@link AiScoreWorkspaceBoundaryException#forTarget(Long, AiScoreTargetType, Long)}. Always
     * populates the structured workspace / target fields so a missing-target spike on a single tenant is greppable
     * without parsing the message string. Rejects {@code null targetType} — every legitimate call site has the type at
     * hand; a silent coercion to {@link AiScoreTargetType#UNKNOWN} would mask a programming error and feed
     * mis-attributed metrics to the security dashboard.
     */
    public static AiScoreTargetNotFoundException forTarget(
        Long callerWorkspaceId, AiScoreTargetType targetType, Long targetId) {

        return forTarget(callerWorkspaceId, targetType, targetId, null);
    }

    public static AiScoreTargetNotFoundException forTarget(
        Long callerWorkspaceId, AiScoreTargetType targetType, Long targetId, Throwable cause) {

        if (targetType == null) {
            throw new IllegalArgumentException(
                "targetType must not be null — call sites always know the resource kind they are looking up; "
                    + "use a specific AiScoreTargetType variant (the UNKNOWN value is reserved for hydration paths "
                    + "where the type was lost in transit, not for factory inputs)");
        }

        return new AiScoreTargetNotFoundException(
            buildMessage(targetType, targetId), callerWorkspaceId, targetType, targetId, cause);
    }

    private static String buildMessage(AiScoreTargetType targetType, Long targetId) {
        String idSuffix = targetId == null ? "" : " " + targetId;

        return switch (targetType) {
            case TRACE -> "Trace" + idSuffix + " not found";
            case SPAN -> "Span" + idSuffix + " not found";
            case EXPERIMENT -> "Experiment" + idSuffix + " not found";
            case DATASET -> "Dataset" + idSuffix + " not found";
            case DATASET_VERSION -> "Dataset version" + idSuffix + " not found";
            case DATASET_ITEM -> "Dataset item" + idSuffix + " not found";
            case WORKSPACE -> "Workspace" + idSuffix + " not found";
            case UNKNOWN -> "Target" + idSuffix + " not found";
        };
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
}
