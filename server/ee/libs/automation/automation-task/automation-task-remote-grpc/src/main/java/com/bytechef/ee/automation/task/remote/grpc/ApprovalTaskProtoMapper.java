/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.task.remote.grpc;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApprovalTaskProtoMapper {

    private static final TypeReference<ApprovalTask> APPROVAL_TASK_TYPE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<List<PendingApproval>> PENDING_APPROVALS_TYPE_REFERENCE =
        new TypeReference<>() {};

    private final JsonBytes jsonBytes;

    public ApprovalTaskProtoMapper(ObjectMapper objectMapper) {
        this.jsonBytes = new JsonBytes(
            objectMapper.rebuild()
                .addMixIn(ApprovalTask.class, ApprovalTaskMixIn.class)
                .build());
    }

    public ApprovalTask toApprovalTask(ByteString bytes) {
        return Objects.requireNonNull(jsonBytes.read(bytes, APPROVAL_TASK_TYPE_REFERENCE), "approvalTask");
    }

    public List<PendingApproval> toPendingApprovals(ByteString bytes) {
        List<PendingApproval> pendingApprovals = jsonBytes.read(bytes, PENDING_APPROVALS_TYPE_REFERENCE);

        return pendingApprovals == null ? List.of() : pendingApprovals;
    }

    public ByteString write(Object value) {
        return jsonBytes.write(value);
    }

    @JsonIgnoreProperties("environmentId")
    private abstract static class ApprovalTaskMixIn {

        @JsonCreator
        ApprovalTaskMixIn() {
        }
    }
}
