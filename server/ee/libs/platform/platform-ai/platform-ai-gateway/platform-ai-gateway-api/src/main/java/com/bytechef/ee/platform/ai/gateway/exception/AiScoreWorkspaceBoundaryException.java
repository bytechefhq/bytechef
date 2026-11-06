/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

/**
 * @author Ivica Cardic
 * @version ee
 */
public class AiScoreWorkspaceBoundaryException extends RuntimeException {

    private final Long workspaceId;
    private final AiScoreTargetType targetType;
    private final Long targetId;

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
