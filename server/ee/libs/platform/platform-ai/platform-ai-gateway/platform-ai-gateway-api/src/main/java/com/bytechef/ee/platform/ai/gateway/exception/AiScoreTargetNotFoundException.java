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
