/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

/**
 * Thrown when a single embedded integration is not visible to the requesting connected user because its permission
 * expression evaluated to {@code false}. The embedded public REST controller maps this to HTTP 404.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedIntegrationNotVisibleException extends RuntimeException {

    public EmbeddedIntegrationNotVisibleException(long integrationId) {
        super("Integration not visible: " + integrationId);
    }
}
