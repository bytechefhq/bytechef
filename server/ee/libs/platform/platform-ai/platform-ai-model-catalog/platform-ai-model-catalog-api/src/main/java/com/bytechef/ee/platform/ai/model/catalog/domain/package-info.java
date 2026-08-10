/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

/**
 * Domain model for the persisted AI model catalog. {@code AiModel} rows are populated from the models.dev catalog by
 * the AI Gateway's reconciler and consumed by any surface that needs to resolve a model identifier. Entities are Spring
 * Data JDBC aggregates; load and persist them via the repository/service layer in
 * {@code com.bytechef.ee.platform.ai.model.catalog.service}.
 *
 * @version ee
 */
package com.bytechef.ee.platform.ai.model.catalog.domain;
