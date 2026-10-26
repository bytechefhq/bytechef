/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.security.web.filter.SpaWebFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RemoteSpaWebFilterContributorTest {

    @Test
    void testRemotePathsAreNotForwardedToTheClient() throws Exception {
        SpaWebFilter spaWebFilter = new SpaWebFilter(List.of(new RemoteSpaWebFilterContributor()));
        MockFilterChain mockFilterChain = new MockFilterChain();
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        spaWebFilter.doFilter(
            new MockHttpServletRequest("GET", "/remote/project/deployment-workflow-service/is-connection-used/1"),
            mockHttpServletResponse, mockFilterChain);

        assertThat(mockHttpServletResponse.getForwardedUrl()).isNull();
        assertThat(mockFilterChain.getRequest()).isNotNull();
    }
}
