/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates a SpEL permission expression authored on an integration or integration workflow against the requesting
 * connected user. Fails closed: a null/blank expression is visible, anything that does not evaluate to {@code true}
 * (including evaluation errors) is hidden.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedPermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedPermissionEvaluator.class);

    private final Evaluator evaluator;

    @SuppressFBWarnings("EI")
    public EmbeddedPermissionEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public boolean evaluate(@Nullable String permissionExpression, ConnectedUser connectedUser) {
        if (StringUtils.isBlank(permissionExpression)) {
            return true;
        }

        try {
            Map<String, Object> context = buildContext(connectedUser);

            Map<String, Object> evaluated = evaluator.evaluate(
                Map.of("__result", "=" + permissionExpression), context);

            return Boolean.parseBoolean(String.valueOf(evaluated.get("__result")));
        } catch (Exception exception) {
            log.warn(
                "Failed to evaluate permission expression [{}] for connected user [{}]; hiding (fail closed)",
                permissionExpression, connectedUser.getExternalId(), exception);

            return false;
        }
    }

    private Map<String, Object> buildContext(ConnectedUser connectedUser) {
        Map<String, Object> context = new HashMap<>();

        Environment environment = connectedUser.getEnvironment();

        context.put("metadata", connectedUser.getMetadata());
        context.put("externalId", connectedUser.getExternalId());
        context.put("email", connectedUser.getEmail());
        context.put("name", connectedUser.getName());
        context.put("environment", environment == null ? null : environment.name());

        return context;
    }
}
