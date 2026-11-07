/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage.repository;

import com.bytechef.ee.platform.ai.tool.usage.AiToolUsage;
import org.springframework.data.repository.CrudRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiToolUsageRepository extends CrudRepository<AiToolUsage, Long> {
}
