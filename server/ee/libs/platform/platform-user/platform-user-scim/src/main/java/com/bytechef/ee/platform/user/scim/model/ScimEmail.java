/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.model;

/**
 * SCIM 2.0 Email sub-resource (RFC 7643 Section 4.1.2).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ScimEmail(String value, String type, boolean primary) {
}
