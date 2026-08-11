/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

/**
 * Audit event types emitted through {@link CustomRoleAuditPublisher} for lifecycle mutations on a custom role. Every
 * event carries {@code roleId} in its payload; create/update events additionally include {@code name}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum CustomRoleAuditEvent {

    /**
     * A custom role was created. Payload: {@code roleId}, {@code name}.
     */
    CUSTOM_ROLE_CREATED,

    /**
     * An existing custom role's editable fields were updated. Payload: {@code roleId}, {@code name}.
     */
    CUSTOM_ROLE_UPDATED,

    /**
     * A custom role was deleted. Payload: {@code roleId}.
     */
    CUSTOM_ROLE_DELETED
}
