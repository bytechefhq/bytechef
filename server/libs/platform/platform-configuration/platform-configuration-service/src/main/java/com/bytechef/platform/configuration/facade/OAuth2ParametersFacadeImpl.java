/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.configuration.facade;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.Authorization;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.util.OAuth2DiscoveryUtils;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class OAuth2ParametersFacadeImpl implements OAuth2ParametersFacade {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionFacade connectionFacade;
    private final OAuth2Service oAuth2Service;

    @SuppressFBWarnings("EI2")
    public OAuth2ParametersFacadeImpl(
        ConnectionDefinitionService connectionDefinitionService, ConnectionFacade connectionFacade,
        OAuth2Service oAuth2Service) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionFacade = connectionFacade;
        this.oAuth2Service = oAuth2Service;
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters,
        AuthorizationType authorizationType, @Nullable Long connectionId, @Nullable PlatformType type) {

        Map<String, Object> parameters = new HashMap<>();

        if (connectionId != null) {
            parameters.putAll(
                connectionFacade.getReauthorizationParameters(connectionId, componentName, connectionVersion));
        }

        if (connectionParameters != null) {
            parameters.putAll(connectionParameters);
        }

        if (type == PlatformType.EMBEDDED &&
            runsDiscovery(componentName, connectionVersion, authorizationType, parameters)) {

            throw new ConfigurationException(
                "Automatic OAuth2 discovery isn't available for embedded integrations yet. Fill in the server's " +
                    "Authorization URL and Token URL, or use OAuth2 (manual).",
                OAuth2ErrorType.DISCOVERY_NOT_AVAILABLE);
        }

        Map<String, ?> checkedParameters = oAuth2Service.checkPredefinedParameters(componentName, parameters);

        if (OAuth2DiscoveryUtils.hasManualEndpoints(checkedParameters) &&
            StringUtils.isBlank(MapUtils.getString(checkedParameters, CLIENT_ID)) &&
            isDiscoveryAuthorization(componentName, connectionVersion, authorizationType)) {

            throw new ConfigurationException(
                "Enter the Client Id of the OAuth app registered with the server. It is required when the " +
                    "Authorization URL and Token URL are set; leave both empty to sign in automatically.",
                OAuth2ErrorType.CLIENT_ID_REQUIRED);
        }

        return connectionDefinitionService.getOAuth2AuthorizationParameters(
            componentName, connectionVersion, authorizationType, checkedParameters);
    }

    private boolean isDiscoveryAuthorization(
        String componentName, int connectionVersion, AuthorizationType authorizationType) {

        ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionConnectionDefinition(
            componentName, connectionVersion);

        List<Authorization> authorizations = connectionDefinition.getAuthorizations();

        return authorizations.stream()
            .filter(authorization -> authorization.getType() == authorizationType)
            .anyMatch(Authorization::isDiscovery);
    }

    private boolean runsDiscovery(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> parameters) {

        return OAuth2DiscoveryUtils.runsDiscovery(
            isDiscoveryAuthorization(componentName, connectionVersion, authorizationType), parameters);
    }
}
