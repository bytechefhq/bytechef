/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

/**
 * Thrown when an embedded integration or integration instance is not visible to the requesting connected user, either
 * because its permission expression evaluated to {@code false} or because the connected user does not own the targeted
 * integration instance. The embedded public REST controllers map this to HTTP 404 (rather than 403) to avoid leaking
 * the existence of resources the connected user is not permitted to see.
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
