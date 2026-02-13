/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * SCIM 2.0 PATCH request body (RFC 7644 Section 3.5.2).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ScimPatchRequest(
    List<String> schemas,
    @JsonProperty("Operations") List<ScimPatchOperation> operations) {
}
