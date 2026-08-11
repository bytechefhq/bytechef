/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.audit;

/**
 * Audit event types emitted through {@link CustomComponentAuditPublisher} for lifecycle mutations on a custom
 * component. Every event carries {@code customComponentId} in its payload; create/update events additionally include
 * {@code name} when available.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum CustomComponentAuditEvent {

    /**
     * A custom component was created. Payload: {@code customComponentId}, {@code name}.
     */
    CUSTOM_COMPONENT_CREATED,

    /**
     * An existing custom component's editable fields were updated. Payload: {@code customComponentId}, {@code name}.
     */
    CUSTOM_COMPONENT_UPDATED,

    /**
     * A custom component was deleted. Payload: {@code customComponentId}.
     */
    CUSTOM_COMPONENT_DELETED,

    /**
     * A custom component was enabled. Payload: {@code customComponentId}.
     */
    CUSTOM_COMPONENT_ENABLED,

    /**
     * A custom component was disabled. Payload: {@code customComponentId}.
     */
    CUSTOM_COMPONENT_DISABLED
}
