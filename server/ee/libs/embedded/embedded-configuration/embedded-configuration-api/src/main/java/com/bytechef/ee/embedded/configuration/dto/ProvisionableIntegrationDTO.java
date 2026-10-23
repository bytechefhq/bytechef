/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProvisionableIntegrationDTO(
    IntegrationDTO integration, int componentVersion,
    List<ProvisionableIntegrationInstanceConfiguration> integrationInstanceConfigurations) {

    @SuppressFBWarnings("EI")
    public record ProvisionableIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfiguration, boolean selfServe,
        List<String> sharedConnectionParameterNames,
        @Nullable OAuth2AuthorizationParameters oAuth2AuthorizationParameters, @Nullable String redirectUri) {
    }
}
