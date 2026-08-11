/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.audit;

/**
 * Audit event types emitted through {@link ApiCollectionAuditPublisher}.
 *
 * <p>
 * {@code API_COLLECTION_*} events describe state transitions on the API collection aggregate. Every event carries an
 * implicit {@code apiCollectionId} that {@link ApiCollectionAuditPublisher} attaches automatically.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ApiCollectionAuditEvent {

    /**
     * A new API collection was persisted. Payload: {@code name}, {@code projectId} (when available).
     */
    API_COLLECTION_CREATED,

    /**
     * An API collection was deleted. Payload: no additional keys required; {@code apiCollectionId} identifies the
     * now-removed row.
     */
    API_COLLECTION_DELETED
}
