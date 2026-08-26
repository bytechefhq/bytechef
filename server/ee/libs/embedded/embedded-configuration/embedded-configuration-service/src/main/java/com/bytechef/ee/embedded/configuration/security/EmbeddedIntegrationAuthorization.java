/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@code @PreAuthorize} predicate bean answering whether the <em>calling</em> connected user may see a given
 * integration. Used by {@code IntegrationInstanceConfigurationFacadeImpl}'s by-id read, which is reached only from the
 * embedded connected-user path.
 *
 * <p>
 * The caller is resolved from the security context, never from a method argument, so the predicate cannot be satisfied
 * by naming somebody else's external id. It then applies exactly the check the list path applies per row —
 * {@link EmbeddedPermissionEvaluator} against the integration's own permission expression — so the by-id read and the
 * list read cannot answer the same integration differently.
 *
 * <p>
 * This is deliberately not merely "a connected user is authenticated": a connected user whose attributes fail the
 * integration's permission expression is denied. Where an integration carries no expression the product's own
 * visibility model treats it as visible to every connected user, and this predicate inherits that — it is precisely as
 * strong as the model it enforces, no stronger and no weaker.
 *
 * <p>
 * Fails closed: no security context, a principal that is not a connected user in the requested environment, an unknown
 * integration, or any evaluation error all deny.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("embeddedIntegrationAuthorization")
@ConditionalOnEEVersion
public class EmbeddedIntegrationAuthorization {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedIntegrationAuthorization.class);

    private final ConnectedUserService connectedUserService;
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;
    private final IntegrationService integrationService;

    @SuppressFBWarnings("EI")
    public EmbeddedIntegrationAuthorization(
        ConnectedUserService connectedUserService, EmbeddedPermissionEvaluator embeddedPermissionEvaluator,
        IntegrationService integrationService) {

        this.connectedUserService = connectedUserService;
        this.embeddedPermissionEvaluator = embeddedPermissionEvaluator;
        this.integrationService = integrationService;
    }

    /**
     * Whether the calling connected user may see {@code integrationId} in {@code environment}.
     *
     * @param integrationId the integration being requested
     * @param environment   the environment the caller is acting in
     * @return {@code true} if the caller is a connected user in that environment and passes the integration's
     *         permission expression
     */
    public boolean canAccessIntegration(long integrationId, Environment environment) {
        Optional<String> externalId = SecurityUtils.fetchCurrentUserLogin();

        if (externalId.isEmpty() || environment == null) {
            return false;
        }

        try {
            Optional<ConnectedUser> connectedUser = connectedUserService.fetchConnectedUser(
                externalId.get(), environment);

            if (connectedUser.isEmpty()) {
                return false;
            }

            Integration integration = integrationService.getIntegration(integrationId);

            return embeddedPermissionEvaluator.evaluate(integration.getPermissionExpression(), connectedUser.get());
        } catch (Exception exception) {
            log.debug(
                "Denying integration id={} for principal [{}] in environment [{}] — fail closed.",
                integrationId, externalId.get(), environment, exception);

            return false;
        }
    }
}
