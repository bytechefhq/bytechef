/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

/**
 * A connection the app could be bound to. {@code componentVersion} is the latest version of the connection's component,
 * the one app executions run, and the version whose operations a binding offers. A connection whose component cannot be
 * resolved is never offered, so the version is always known. {@code usable} means valid credentials and
 * {@code CONNECTION_EDIT} for the caller.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppBindableConnectionDTO(
    long id, String name, String componentName, int connectionVersion, int componentVersion, boolean usable) {
}
