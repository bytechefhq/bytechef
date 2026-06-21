/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiConnectorFacadeImplTest {

    @Test
    void testConvertComponentNameNormalizes() {
        assertEquals("petstore", ApiConnectorFacadeImpl.convertComponentName("Pet Store"));
        assertEquals("pet.store", ApiConnectorFacadeImpl.convertComponentName("pet-store"));
    }

    @Test
    void testConvertComponentNameRejectsPathTraversal() {
        assertThrows(
            ConfigurationException.class,
            () -> ApiConnectorFacadeImpl.convertComponentName("../../etc/passwd"));
        assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName(".."));
        assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("a/b"));
        assertThrows(
            ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("a\\b"));
    }

    @Test
    void testConvertComponentNameRejectsDottedTraversal() {
        // '-' maps to '.', so consecutive dashes could otherwise produce a '..' segment that traverses upward.
        assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("a--b"));
        assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName(".lead"));
        assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("trail."));
    }

    @Test
    void testConvertComponentNameRejectsBlank() {
        assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("   "));
    }
}
