/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;

/**
 * The role a {@link WorkspaceUser} row resolves to, in whichever of the two forms that row carries. Exactly one of the
 * two components is non-null, mirroring the XOR invariant the row itself enforces.
 * <p>
 * It exists so that callers resolving a role for an environment do not each re-implement the built-in-versus-custom
 * branch.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ResolvedRole(WorkspaceRole workspaceRole, Long customRoleId) {
}
