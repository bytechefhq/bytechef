/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.app.dto.AppApiCollectionDTO;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * API Platform calls of the calling app. Every method resolves the app from the request's
 * {@link com.bytechef.ee.automation.app.security.AppPrincipal} and refuses a disabled app. The app's bindings are the
 * authorization: an app key holds no user permissions, so collections are read through API Platform services.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppApiExecutionFacade {

    /**
     * Returns the collections bound to the calling app that resolve as available, in binding order.
     */
    List<ApiCollection> getCallableApiCollections();

    /**
     * Returns the collections bound to the calling app that exist but do not resolve as available.
     */
    List<ApiCollection> getUnavailableApiCollections();

    /**
     * Returns the collections bound to the calling app with their endpoints and schemas.
     */
    List<AppApiCollectionDTO> getApiCollections();

    /**
     * Runs {@code call} and records it in the tool invocation log against the calling app. A result whose
     * {@code statusOf} is 400 or above is recorded as an error.
     */
    <T> T recordApiCall(String collectionName, String endpointName, Supplier<T> call, ToIntFunction<T> statusOf);
}
