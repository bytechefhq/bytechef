/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedPageAuthorizeHttpRequestContributorTest {

    @Test
    void testFrameablePagesAreTheEmbeddedPages() {
        EmbeddedPageAuthorizeHttpRequestContributor contributor = new EmbeddedPageAuthorizeHttpRequestContributor(
            new ApplicationProperties());

        assertThat(contributor.getFrameablePermitAllRequestMatcherPaths())
            .containsExactlyInAnyOrder("/automation-hub.html", "/integration-marketplace.html",
                "/workflow-builder.html");
        assertThat(contributor.getPermitAllRequestMatcherPaths()).isEmpty();
    }

    @Test
    void testFrameAncestorsAreEmptyWhenAllowedParentOriginsAreNotConfigured() {
        EmbeddedPageAuthorizeHttpRequestContributor contributor = new EmbeddedPageAuthorizeHttpRequestContributor(
            new ApplicationProperties());

        assertThat(contributor.getFrameAncestors()).isEmpty();
    }

    @Test
    void testFrameAncestorsAreTheNormalizedAllowedParentOrigins() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        embedded.setAllowedParentOrigins(
            List.of(" https://a.example ", "https://b.example/", "", "https://a.example"));

        EmbeddedPageAuthorizeHttpRequestContributor contributor = new EmbeddedPageAuthorizeHttpRequestContributor(
            applicationProperties);

        assertThat(contributor.getFrameAncestors()).containsExactly("https://a.example", "https://b.example");
    }
}
