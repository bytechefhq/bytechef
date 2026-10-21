/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import org.jspecify.annotations.Nullable;

/**
 * A tag of an app: an existing tag by id, or a tag by name that is created when there is none of that name.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppTagDTO(@Nullable Long id, String name) {
}
