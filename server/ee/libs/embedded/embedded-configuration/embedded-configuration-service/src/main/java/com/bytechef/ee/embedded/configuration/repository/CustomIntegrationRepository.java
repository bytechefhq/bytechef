/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomIntegrationRepository {

    List<Integration> findAllIntegrations(Long categoryId, List<Long> ids, Long tagId, Integer status);
}
