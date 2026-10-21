/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The upstream HTTP response handed back to the app.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ProxyResponse(
    int status, Map<String, List<String>> headers, @Nullable String body, BodyEncoding bodyEncoding) {

    public ProxyResponse {
        headers = Collections.unmodifiableMap(ProxyRequest.deepCopy(headers));
    }
}
