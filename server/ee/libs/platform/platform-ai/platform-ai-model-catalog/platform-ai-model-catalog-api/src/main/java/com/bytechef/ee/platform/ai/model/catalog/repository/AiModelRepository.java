/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.model.catalog.repository;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiModelRepository extends ListCrudRepository<AiModel, Long> {

    List<AiModel> findAllByProviderId(Long providerId);

    List<AiModel> findAllByEnabled(boolean enabled);

    Optional<AiModel> findByProviderIdAndName(Long providerId, String name);

    Optional<AiModel> findFirstByNameOrderByIdAsc(String name);
}
