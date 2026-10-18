/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ScimConstants {

    public static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";
    public static final String PATH_PATTERN = "/api/scim/v2/**";
    public static final String PATH_PREFIX = "/api/scim/v2/";
    public static final String SCIM_AUTHORITY = "ROLE_SCIM";

    private ScimConstants() {
    }
}
