
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.swagger.util;

import org.springdoc.core.models.GroupedOpenApi;

public class SwaggerUtils {

    public static final GroupedOpenApi EMBEDDED_GROUP_API = GroupedOpenApi.builder()
        .group("embedded")
        .displayName("Embedded API")
        .pathsToMatch(new String[] {
            "/api/embedded/**"
        })
        .build();
}
