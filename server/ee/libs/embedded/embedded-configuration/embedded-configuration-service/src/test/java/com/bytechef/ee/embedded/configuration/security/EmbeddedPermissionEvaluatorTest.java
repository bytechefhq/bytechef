/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class EmbeddedPermissionEvaluatorTest {

    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Mock
    private ConnectedUser connectedUser;

    @BeforeEach
    void setUp() {
        embeddedPermissionEvaluator = new EmbeddedPermissionEvaluator(SpelEvaluator.create());
    }

    @Test
    void testNullExpressionIsVisible() {
        assertTrue(embeddedPermissionEvaluator.evaluate(null, connectedUser));
    }

    @Test
    void testBlankExpressionIsVisible() {
        assertTrue(embeddedPermissionEvaluator.evaluate("   ", connectedUser));
    }

    @Test
    void testMetadataMatchIsVisible() {
        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "pro"));

        assertTrue(embeddedPermissionEvaluator.evaluate("metadata['plan'] == 'pro'", connectedUser));
    }

    @Test
    void testMetadataMismatchIsHidden() {
        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "free"));

        assertFalse(embeddedPermissionEvaluator.evaluate("metadata['plan'] == 'pro'", connectedUser));
    }

    @Test
    void testMissingMetadataKeyIsHidden() {
        when(connectedUser.getMetadata()).thenReturn(Map.of());

        assertFalse(embeddedPermissionEvaluator.evaluate("metadata['plan'] == 'pro'", connectedUser));
    }

    @Test
    void testEmailUserFieldIsVisible() {
        when(connectedUser.getMetadata()).thenReturn(Map.of());
        when(connectedUser.getEmail()).thenReturn("jane@acme.com");

        assertTrue(embeddedPermissionEvaluator.evaluate("email == 'jane@acme.com'", connectedUser));
    }

    @Test
    void testEnvironmentUserFieldIsVisible() {
        when(connectedUser.getMetadata()).thenReturn(Map.of());
        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);

        assertTrue(embeddedPermissionEvaluator.evaluate("environment == 'PRODUCTION'", connectedUser));
    }

    @Test
    void testInvalidExpressionIsHiddenFailClosed() {
        when(connectedUser.getMetadata()).thenReturn(Map.of());

        assertFalse(embeddedPermissionEvaluator.evaluate("this is not valid spel )(", connectedUser));
    }

    @Test
    void testNonBooleanResultIsHidden() {
        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "pro"));

        assertFalse(embeddedPermissionEvaluator.evaluate("metadata['plan']", connectedUser));
    }
}
