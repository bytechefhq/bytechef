/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
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
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.google.protobuf.Empty;
import java.io.Serializable;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Answers permission checks on configuration-app, which evaluates them for the caller whose identity travels on every
 * gRPC call. Checks skipped for an embedded delegation are granted locally, since the skip flag is bound to this
 * thread. Workspace scope caches live on configuration-app, so evictions here are no-ops.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressWarnings("PMD.UnusedFormalParameter")
public class RemotePermissionServiceClient implements PermissionService {

    private static final String SERVICE_ID = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemotePermissionServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public boolean isTenantAdmin() {
        BoolResponse boolResponse = getStub().isTenantAdmin(Empty.getDefaultInstance());

        return boolResponse.getValue();
    }

    @Override
    public boolean isCurrentUser(long userId) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        BoolResponse boolResponse = getStub().isCurrentUser(toIdRequest(userId));

        return boolResponse.getValue();
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        BoolResponse boolResponse = getStub().hasWorkspaceRole(
            WorkspaceRoleRequest.newBuilder()
                .setWorkspaceId(workspaceId)
                .setMinimumRole(minimumRole)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        BoolResponse boolResponse = getStub().hasWorkspaceScope(
            WorkspaceScopeRequest.newBuilder()
                .setWorkspaceId(workspaceId)
                .setScope(scope)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        BoolResponse boolResponse = getStub().hasWorkspaceScopeForProject(
            ProjectScopeRequest.newBuilder()
                .setProjectId(projectId)
                .setScope(scope)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        BoolResponse boolResponse = getStub().hasResourceScope(toResourceScopeRequest(id, resourceType, scope));

        return boolResponse.getValue();
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        BoolResponse boolResponse = getStub().isResourceOwner(
            ResourceOwnerRequest.newBuilder()
                .setResourceType(resourceType)
                .setId(id)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        BoolResponse boolResponse = getStub().hasResourceRole(
            ResourceRoleRequest.newBuilder()
                .setId(id)
                .setResourceType(resourceType)
                .setMinimumRole(minimumRole)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        BoolResponse boolResponse = getStub().hasWorkflowScope(
            WorkflowScopeRequest.newBuilder()
                .setWorkflowId(workflowId)
                .setScope(scope)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        StringsResponse stringsResponse = getStub().getMyWorkspaceScopes(toIdRequest(workspaceId));

        return Set.copyOf(stringsResponse.getValuesList());
    }

    @Override
    public @Nullable String getMyWorkspaceRole(long workspaceId) {
        OptionalStringResponse optionalStringResponse = getStub().getMyWorkspaceRole(toIdRequest(workspaceId));

        return optionalStringResponse.hasValue() ? optionalStringResponse.getValue() : null;
    }

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
    }

    private PermissionRemoteServiceGrpc.PermissionRemoteServiceBlockingStub getStub() {
        return PermissionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }

    private static ResourceScopeRequest toResourceScopeRequest(Serializable id, String resourceType, String scope) {
        ResourceScopeRequest.Builder builder = ResourceScopeRequest.newBuilder()
            .setResourceType(resourceType)
            .setScope(scope);

        if (id instanceof Long || id instanceof Integer) {
            Number numericId = (Number) id;

            builder.setNumericId(numericId.longValue());
        } else if (id instanceof String stringId) {
            builder.setStringId(stringId);
        } else {
            throw new IllegalArgumentException("Unsupported resource id for a remote permission check: " + id);
        }

        return builder.build();
    }

    private static IdRequest toIdRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }
}
