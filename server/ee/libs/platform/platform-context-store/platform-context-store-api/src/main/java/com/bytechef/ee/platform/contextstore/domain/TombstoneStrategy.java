/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.domain;

/**
 * @version ee
 * @author Ivica Cardic
 */
public enum TombstoneStrategy {

    PERIODIC_FULL_REPLACE,

    UPSTREAM_CHANGE_FEED,

    NONE
}
