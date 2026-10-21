/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.security;

import java.util.Set;

/**
 * The app instances whose invocation log rows a caller may read in a workspace query: every instance, the deleted ones
 * included, or only the listed ones. Rows without an app instance are not affected.
 *
 * @param restricted     {@code false} when the caller may read the rows of every app instance
 * @param appInstanceIds the app instances the caller may read when {@code restricted}; empty otherwise
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ViewableAppInstances(boolean restricted, Set<Long> appInstanceIds) {

    private static final ViewableAppInstances ALL = new ViewableAppInstances(false, Set.of());

    public ViewableAppInstances {
        appInstanceIds = Set.copyOf(appInstanceIds);
    }

    public static ViewableAppInstances all() {
        return ALL;
    }

    public static ViewableAppInstances only(Set<Long> appInstanceIds) {
        return new ViewableAppInstances(true, appInstanceIds);
    }
}
