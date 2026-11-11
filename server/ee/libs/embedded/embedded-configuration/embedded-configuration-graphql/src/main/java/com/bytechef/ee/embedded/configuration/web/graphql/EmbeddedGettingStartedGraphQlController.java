/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.dto.EmbeddedGettingStartedDTO;
import com.bytechef.ee.embedded.configuration.dto.LastFailedCallDTO;
import com.bytechef.ee.embedded.configuration.facade.EmbeddedGettingStartedFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class EmbeddedGettingStartedGraphQlController {

    private static final String EMBEDDED_API_PATH = "/api/embedded/v1";

    private final ApplicationProperties applicationProperties;
    private final EmbeddedGettingStartedFacade embeddedGettingStartedFacade;

    @SuppressFBWarnings("EI")
    public EmbeddedGettingStartedGraphQlController(
        ApplicationProperties applicationProperties, EmbeddedGettingStartedFacade embeddedGettingStartedFacade) {

        this.applicationProperties = applicationProperties;
        this.embeddedGettingStartedFacade = embeddedGettingStartedFacade;
    }

    @SchemaMapping(typeName = "EmbeddedGettingStarted", field = "apiBaseUrl")
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public @Nullable String apiBaseUrl(EmbeddedGettingStartedDTO embeddedGettingStarted) {
        String publicUrl = applicationProperties.getPublicUrl();

        if (StringUtils.isBlank(publicUrl)) {
            return null;
        }

        return StringUtils.removeEnd(publicUrl, "/") + EMBEDDED_API_PATH;
    }

    @QueryMapping
    public EmbeddedGettingStartedDTO embeddedGettingStarted() {
        return embeddedGettingStartedFacade.getEmbeddedGettingStarted();
    }

    @QueryMapping
    public boolean embeddedGettingStartedCompleted() {
        return embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted();
    }

    @MutationMapping
    public boolean dismissEmbeddedGettingStarted() {
        embeddedGettingStartedFacade.dismissEmbeddedGettingStarted();

        return true;
    }

    @SchemaMapping(typeName = "EmbeddedGettingStartedFailedCall", field = "createdDate")
    public long createdDate(LastFailedCallDTO lastFailedCall) {
        Instant createdDate = lastFailedCall.createdDate();

        return createdDate.toEpochMilli();
    }
}
