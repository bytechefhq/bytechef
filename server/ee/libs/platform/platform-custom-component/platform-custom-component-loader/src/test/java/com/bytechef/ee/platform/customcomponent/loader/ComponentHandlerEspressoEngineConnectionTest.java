/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Property;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentHandlerEspressoEngineConnectionTest {

    @Test
    void testToConnectionDefinitionRebuildsStaticShape() throws Exception {
        Map<String, ?> connectionMap = Map.of(
            "baseUri", "https://api.example.com",
            "authorizations", List.of(
                Map.of(
                    "type", "OAUTH2_AUTHORIZATION_CODE",
                    "authorizationUrl", "https://example.com/oauth/authorize",
                    "tokenUrl", "https://example.com/oauth/token",
                    "scopes", List.of("read", "write"),
                    "properties", List.of(
                        Map.of("name", "clientId", "type", "STRING", "required", true)))));

        ConnectionDefinition connectionDefinition = ComponentHandlerEspressoEngine.toConnectionDefinition(
            connectionMap);

        ConnectionDefinition.BaseUriFunction baseUriFunction = connectionDefinition.getBaseUri()
            .orElseThrow();

        assertEquals("https://api.example.com", baseUriFunction.apply(null, null));

        List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations();

        assertEquals(1, authorizations.size());

        Authorization authorization = authorizations.getFirst();

        assertEquals(Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE, authorization.getType());

        Authorization.TokenUrlFunction tokenUrlFunction = authorization.getTokenUrl()
            .orElseThrow();

        assertEquals("https://example.com/oauth/token", tokenUrlFunction.apply(null, null));

        Authorization.ScopesFunction scopesFunction = authorization.getScopes()
            .orElseThrow();

        assertEquals(Map.of("read", true, "write", true), scopesFunction.apply(null, null));

        List<? extends Property> properties = authorization.getProperties();

        assertEquals(1, properties.size());
        assertEquals("clientId", properties.getFirst()
            .getName());
    }
}
