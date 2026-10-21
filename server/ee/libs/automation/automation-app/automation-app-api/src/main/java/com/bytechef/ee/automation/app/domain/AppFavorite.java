/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import java.time.Instant;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppFavorite(long userId, long appId, Instant createdDate) {
}
