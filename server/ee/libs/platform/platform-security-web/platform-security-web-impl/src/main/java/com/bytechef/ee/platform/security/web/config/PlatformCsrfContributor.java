/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.config;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.CsrfContributor;
import java.util.List;
import java.util.Objects;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class PlatformCsrfContributor implements CsrfContributor {

    @Override
    public List<RequestMatcher> getIgnoringRequestMatchers() {
        return List.of(
            regexMatcher("^/api/platform/v[0-9]+/.+"),
            // For CORS requests
            request -> Objects.equals(request.getMethod(), "OPTIONS"));
    }
}
