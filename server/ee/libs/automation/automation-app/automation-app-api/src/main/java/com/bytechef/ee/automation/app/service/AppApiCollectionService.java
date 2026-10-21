/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppApiCollection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppApiCollectionService {

    /**
     * Binds the API collection to the app, returning the existing binding when there already is one.
     */
    BindResult bind(long appId, long apiCollectionId);

    /**
     * Returns the app's bindings ordered by API collection id.
     */
    List<AppApiCollection> getBindings(long appId);

    /**
     * Returns whether the API collection is bound to the app.
     */
    boolean isBound(long appId, long apiCollectionId);

    /**
     * Deletes the app's binding to the API collection.
     *
     * @return {@code true} when a binding was deleted
     */
    boolean unbind(long appId, long apiCollectionId);

    /**
     * The binding and whether this call created it.
     */
    @SuppressFBWarnings("EI")
    record BindResult(AppApiCollection appApiCollection, boolean created) {
    }
}
