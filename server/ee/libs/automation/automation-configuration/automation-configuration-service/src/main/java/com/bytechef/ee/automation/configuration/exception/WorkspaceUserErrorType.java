/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.exception;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.exception.AbstractErrorType;

/**
 * Error types surfaced by {@link com.bytechef.ee.automation.configuration.service.WorkspaceUserServiceImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkspaceUserErrorType extends AbstractErrorType {

    public static final WorkspaceUserErrorType ALREADY_MEMBER = new WorkspaceUserErrorType(100);
    public static final WorkspaceUserErrorType NOT_MEMBER = new WorkspaceUserErrorType(101);
    public static final WorkspaceUserErrorType LAST_ADMIN_PROTECTED = new WorkspaceUserErrorType(102);
    public static final WorkspaceUserErrorType SELF_DEMOTION_FORBIDDEN = new WorkspaceUserErrorType(103);
    public static final WorkspaceUserErrorType INVALID_WORKSPACE_ROLE = new WorkspaceUserErrorType(104);
    public static final WorkspaceUserErrorType INHERITED_MEMBERSHIP = new WorkspaceUserErrorType(105);

    /**
     * The named custom role does not exist. Keeps error key 106, which was previously spelled
     * {@code CUSTOM_ROLE_NOT_IN_WORKSPACE} and served three unrelated meanings — including a role-scoping model that
     * {@code custom_role} never had, since it carries no {@code workspace_id}. Every custom role is tenant-global, so
     * existence is the only thing there is to check and the key means exactly that. The role-argument violations it
     * also used to carry now travel as {@link #ROLE_SELECTION_INVALID}.
     */
    public static final WorkspaceUserErrorType CUSTOM_ROLE_NOT_FOUND = new WorkspaceUserErrorType(106);
    public static final WorkspaceUserErrorType SELF_PROMOTION_FORBIDDEN = new WorkspaceUserErrorType(107);
    public static final WorkspaceUserErrorType EXPLICIT_MODE_MEMBERSHIP = new WorkspaceUserErrorType(108);

    /**
     * Neither or both of a built-in role and a custom role were supplied, breaking the XOR that {@code WorkspaceUser}
     * enforces on its own columns. Split out of 106 so a client can tell a malformed request from a role that is simply
     * gone: the first is fixed by sending one role, the second by picking a different one. Appended at the end because
     * the keys are persisted and renumbering would change what already-emitted errors mean.
     */
    public static final WorkspaceUserErrorType ROLE_SELECTION_INVALID = new WorkspaceUserErrorType(109);

    /**
     * A per-environment role was asked to be revoked from a member who holds no row naming that environment. Distinct
     * from {@link #NOT_MEMBER}: the member is reachable there through their workspace-wide role, which is changed
     * workspace-wide rather than one environment at a time. Appended at the end because the keys are persisted.
     */
    public static final WorkspaceUserErrorType NO_ENVIRONMENT_ROLE = new WorkspaceUserErrorType(110);

    private WorkspaceUserErrorType(int errorKey) {
        super(WorkspaceUser.class, errorKey);
    }
}
