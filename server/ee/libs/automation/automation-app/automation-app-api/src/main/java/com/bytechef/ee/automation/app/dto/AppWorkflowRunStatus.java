/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

/**
 * The state of an app's workflow run as the app sees it. Derived from the job on every read, never persisted.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AppWorkflowRunStatus {

    RUNNING, APPROVAL_REQUIRED, COMPLETED, FAILED, CANCELLED
}
