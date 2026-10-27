/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.OptionalStringResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.PermissionRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectScopeRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ResourceOwnerRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ResourceRoleRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ResourceScopeRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.StringsResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkflowScopeRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkspaceRoleRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkspaceScopeRequest;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.Serializable;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class PermissionRemoteServiceGrpcServer extends PermissionRemoteServiceGrpc.PermissionRemoteServiceImplBase {

    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public PermissionRemoteServiceGrpcServer(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public void getMyWorkspaceRole(IdRequest request, StreamObserver<OptionalStringResponse> responseObserver) {
        String workspaceRole = permissionService.getMyWorkspaceRole(request.getId());

        OptionalStringResponse.Builder builder = OptionalStringResponse.newBuilder();

        if (workspaceRole != null) {
            builder.setValue(workspaceRole);
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getMyWorkspaceScopes(IdRequest request, StreamObserver<StringsResponse> responseObserver) {
        Set<String> workspaceScopes = permissionService.getMyWorkspaceScopes(request.getId());

        responseObserver.onNext(
            StringsResponse.newBuilder()
                .addAllValues(workspaceScopes)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void hasResourceRole(ResourceRoleRequest request, StreamObserver<BoolResponse> responseObserver) {
        respond(
            permissionService.hasResourceRole(request.getId(), request.getResourceType(), request.getMinimumRole()),
            responseObserver);
    }

    @Override
    public void hasResourceScope(ResourceScopeRequest request, StreamObserver<BoolResponse> responseObserver) {
        Serializable resourceId = switch (request.getResourceIdCase()) {
            case NUMERIC_ID -> request.getNumericId();
            case STRING_ID -> request.getStringId();
            case RESOURCEID_NOT_SET -> null;
        };

        if (resourceId == null) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("A resource scope check needs a resource id")
                    .asRuntimeException());

            return;
        }

        respond(
            permissionService.hasResourceScope(resourceId, request.getResourceType(), request.getScope()),
            responseObserver);
    }

    @Override
    public void hasWorkflowScope(WorkflowScopeRequest request, StreamObserver<BoolResponse> responseObserver) {
        respond(permissionService.hasWorkflowScope(request.getWorkflowId(), request.getScope()), responseObserver);
    }

    @Override
    public void hasWorkspaceRole(WorkspaceRoleRequest request, StreamObserver<BoolResponse> responseObserver) {
        respond(
            permissionService.hasWorkspaceRole(request.getWorkspaceId(), request.getMinimumRole()), responseObserver);
    }

    @Override
    public void hasWorkspaceScope(WorkspaceScopeRequest request, StreamObserver<BoolResponse> responseObserver) {
        respond(permissionService.hasWorkspaceScope(request.getWorkspaceId(), request.getScope()), responseObserver);
    }

    @Override
    public void hasWorkspaceScopeForProject(
        ProjectScopeRequest request, StreamObserver<BoolResponse> responseObserver) {

        respond(
            permissionService.hasWorkspaceScopeForProject(request.getProjectId(), request.getScope()),
            responseObserver);
    }

    @Override
    public void isCurrentUser(IdRequest request, StreamObserver<BoolResponse> responseObserver) {
        respond(permissionService.isCurrentUser(request.getId()), responseObserver);
    }

    @Override
    public void isResourceOwner(ResourceOwnerRequest request, StreamObserver<BoolResponse> responseObserver) {
        respond(permissionService.isResourceOwner(request.getResourceType(), request.getId()), responseObserver);
    }

    @Override
    public void isTenantAdmin(Empty request, StreamObserver<BoolResponse> responseObserver) {
        respond(permissionService.isTenantAdmin(), responseObserver);
    }

    private static void respond(boolean value, StreamObserver<BoolResponse> responseObserver) {
        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(value)
                .build());
        responseObserver.onCompleted();
    }
}
