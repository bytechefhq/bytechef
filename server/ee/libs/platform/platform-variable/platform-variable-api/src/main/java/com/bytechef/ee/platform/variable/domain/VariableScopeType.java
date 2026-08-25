/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.domain;

/**
 * Where a set of variables lives: an automation workspace or the embedded organization (the tenant).
 *
 * @version ee
 */
public enum VariableScopeType {

    WORKSPACE, EMBEDDED
}
