/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.audit;

/**
 * Audit event types emitted through {@link IntegrationAuditPublisher} for lifecycle mutations on an integration. Every
 * event carries {@code integrationId} in its payload; create/update events additionally include {@code name} and
 * {@code componentName} when available.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum IntegrationAuditEvent {

    /**
     * An integration was created. Payload: {@code integrationId}, {@code name}, {@code componentName}.
     */
    INTEGRATION_CREATED,

    /**
     * An existing integration's editable fields were updated. Payload: {@code integrationId}, {@code name},
     * {@code componentName}.
     */
    INTEGRATION_UPDATED,

    /**
     * An integration was deleted. Payload: {@code integrationId}.
     */
    INTEGRATION_DELETED
}
