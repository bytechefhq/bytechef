/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SCIM 2.0 Group reference within a User resource (RFC 7643 Section 4.1.2).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ScimGroupRef(String value, @JsonProperty("$ref") String ref, String display) {
}
