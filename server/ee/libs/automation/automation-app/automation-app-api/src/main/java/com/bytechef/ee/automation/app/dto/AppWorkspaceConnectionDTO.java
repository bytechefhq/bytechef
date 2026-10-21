/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import org.jspecify.annotations.Nullable;

/**
 * A connection of a workspace as the app loop lists it. It never carries parameters or credentials. {@code usable}
 * means valid credentials and {@code CONNECTION_EDIT} for the caller; an AI-provider connection is never usable.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppWorkspaceConnectionDTO(
    long id, String name, String componentName, int connectionVersion, @Nullable String authorizationType,
    String credentialStatus, boolean aiProvider, boolean usable) {
}
