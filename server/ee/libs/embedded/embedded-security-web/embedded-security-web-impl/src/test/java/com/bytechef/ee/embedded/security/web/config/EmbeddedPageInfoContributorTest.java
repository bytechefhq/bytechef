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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedPageInfoContributorTest {

    @Test
    void testContributesConfiguredAllowedParentOrigins() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        embedded.setAllowedParentOrigins(List.of("https://a.example", "https://b.example/"));

        Info info = contribute(applicationProperties);

        assertThat(info.getDetails())
            .containsEntry(
                "embedded", Map.of("allowedParentOrigins", List.of("https://a.example", "https://b.example")));
    }

    @Test
    void testContributesEmptyAllowedParentOriginsWhenNotConfigured() {
        Info info = contribute(new ApplicationProperties());

        assertThat(info.getDetails()).containsEntry("embedded", Map.of("allowedParentOrigins", List.of()));
    }

    private static Info contribute(ApplicationProperties applicationProperties) {
        EmbeddedPageInfoContributor contributor = new EmbeddedPageInfoContributor(applicationProperties);

        Info.Builder builder = new Info.Builder();

        contributor.contribute(builder);

        return builder.build();
    }
}
