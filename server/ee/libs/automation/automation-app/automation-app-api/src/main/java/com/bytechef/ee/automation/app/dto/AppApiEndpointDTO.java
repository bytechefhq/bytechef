/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * An endpoint of an API collection bound to the calling app. {@code path} is relative to the collection's
 * {@code /v{collectionVersion}/{contextPath}}. The schemas are the {@code request} and {@code response} properties of
 * the endpoint workflow's {@code apiPlatform/newApiRequest} trigger, each JSON Schema string parsed; null when they
 * cannot be read.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppApiEndpointDTO(
    String name, String httpMethod, @Nullable String path, @Nullable Map<String, Object> requestSchema,
    @Nullable Map<String, Object> responseSchema) {
}
