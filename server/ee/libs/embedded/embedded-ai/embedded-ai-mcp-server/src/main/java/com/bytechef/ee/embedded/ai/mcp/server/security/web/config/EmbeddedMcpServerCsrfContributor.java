/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.CsrfContributor;
import java.util.List;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedMcpServerCsrfContributor implements CsrfContributor {

    @Override
    public List<RequestMatcher> getIgnoringRequestMatchers() {
        return List.of(
            PathPatternRequestMatcher.withDefaults()
                .matcher("/api/embedded/sse"));
    }
}
