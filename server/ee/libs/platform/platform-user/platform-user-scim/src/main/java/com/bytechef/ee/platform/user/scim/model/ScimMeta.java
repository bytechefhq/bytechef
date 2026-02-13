/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.model;

/**
 * SCIM 2.0 Meta sub-attribute (RFC 7643 Section 3.1).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ScimMeta(String resourceType, String created, String lastModified, String location) {
}
