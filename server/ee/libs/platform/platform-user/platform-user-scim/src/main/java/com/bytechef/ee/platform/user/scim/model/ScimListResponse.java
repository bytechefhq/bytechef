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
 * SCIM 2.0 list response envelope (RFC 7644 Section 3.4.2).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ScimListResponse<T>(
    List<String> schemas,
    int totalResults,
    int startIndex,
    int itemsPerPage,
    @JsonProperty("Resources") List<T> resources) {

    public static <T> ScimListResponse<T> of(List<T> resources, int startIndex) {
        return new ScimListResponse<>(
            List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
            resources.size(), startIndex, resources.size(), resources);
    }
}
