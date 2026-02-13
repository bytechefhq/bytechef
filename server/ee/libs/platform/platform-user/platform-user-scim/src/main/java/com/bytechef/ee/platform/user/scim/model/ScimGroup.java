/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * SCIM 2.0 Group resource (RFC 7643 Section 4.2).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ScimGroup(
    List<String> schemas,
    String id,
    String displayName,
    List<ScimMember> members,
    ScimMeta meta) {

    public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";
}
