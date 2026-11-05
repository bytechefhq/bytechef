/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ProjectGitAuditEvents {

    public static final String PROJECT_GIT_CONFIGURATION_SAVED = "PROJECT_GIT_CONFIGURATION_SAVED";
    public static final String PROJECT_GIT_PULLED = "PROJECT_GIT_PULLED";
    public static final String PROJECT_GIT_PUSHED = "PROJECT_GIT_PUSHED";

    private ProjectGitAuditEvents() {
    }
}
